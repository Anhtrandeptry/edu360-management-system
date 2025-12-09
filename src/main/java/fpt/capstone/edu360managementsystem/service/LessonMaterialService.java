package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.LessonMaterialResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.CourseLesson;
import fpt.capstone.edu360managementsystem.entity.LessonMaterial;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.LessonMaterialRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonMaterialService {

    private final LessonMaterialRepository materialRepository;
    private final CourseLessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final ClazzRepository clazzRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    /**
     * Upload tài liệu cho bài học lên Cloudinary
     */
    @Transactional
    public LessonMaterialResponse uploadMaterial(Long lessonId, MultipartFile file,
            String description, Long userId) throws IOException {
        log.info("Uploading material for lesson {} by user {}", lessonId, userId);

        // Validate lesson
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học với ID: " + lessonId));

        // Validate user
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Tạo tên file gốc
        String originalFileName = file.getOriginalFilename();

        // Upload file lên Cloudinary
        String folder = "materials/lesson-" + lessonId;
        String fileUrl = cloudinaryService.uploadFile(file, folder);

        // Lưu metadata vào database
        LessonMaterial material = LessonMaterial.builder()
                .lesson(lesson)
                .fileName(originalFileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .description(description)
                .uploadedBy(uploader)
                .build();

        material = materialRepository.save(material);
        log.info("Lesson material saved with ID: {}, Cloudinary URL: {}", material.getId(), fileUrl);

        // Gửi thông báo cho học sinh trong các lớp sử dụng course này
        sendMaterialNotification(lesson, originalFileName, uploader.getFullName(), false);

        return mapToResponse(material);
    }

    /**
     * Thêm link tài liệu cho bài học
     */
    @Transactional
    public LessonMaterialResponse addLink(Long lessonId, String url, Long userId) {
        log.info("Adding link for lesson {} by user {}: {}", lessonId, userId, url);

        // Validate lesson
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài học với ID: " + lessonId));

        // Validate user
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Lưu metadata vào database với fileType = "LINK"
        LessonMaterial material = LessonMaterial.builder()
                .lesson(lesson)
                .fileName(url) // Dùng URL làm tên
                .fileUrl(url)
                .fileType("LINK")
                .fileSize(0L)
                .description(null)
                .uploadedBy(uploader)
                .build();

        material = materialRepository.save(material);
        log.info("Link saved with ID: {}", material.getId());

        // Gửi thông báo cho học sinh
        sendMaterialNotification(lesson, url, uploader.getFullName(), true);

        return mapToResponse(material);
    }

    /**
     * Lấy danh sách tài liệu của lesson
     */
    public List<LessonMaterialResponse> getMaterialsByLesson(Long lessonId) {
        return materialRepository.findByLesson_IdOrderByUploadedAtDesc(lessonId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tài liệu theo chapter (tất cả lessons trong chapter)
     */
    public List<LessonMaterialResponse> getMaterialsByChapter(Long chapterId) {
        return materialRepository.findByLesson_Chapter_IdOrderByUploadedAtDesc(chapterId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xóa tài liệu
     */
    @Transactional
    public void deleteMaterial(Long materialId, Long userId) throws IOException {
        LessonMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));

        // Xóa file trên Cloudinary nếu không phải link và là URL Cloudinary
        if (!"LINK".equals(material.getFileType())) {
            try {
                String fileUrl = material.getFileUrl();
                if (fileUrl != null && fileUrl.contains("cloudinary.com")) {
                    String publicId = cloudinaryService.extractPublicIdFromUrl(fileUrl);
                    if (publicId != null) {
                        cloudinaryService.deleteFile(publicId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to delete file from Cloudinary: {}", e.getMessage());
            }
        }

        // Xóa record trong database
        materialRepository.delete(material);
        log.info("Lesson material {} deleted by user {}", materialId, userId);
    }

    /**
     * Lấy thông tin material theo ID
     */
    public LessonMaterialResponse getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
    }

    /**
     * Gửi thông báo cho học sinh khi có tài liệu mới trong lesson
     */
    private void sendMaterialNotification(CourseLesson lesson, String materialName,
            String teacherName, boolean isLink) {
        try {
            // Lấy course từ lesson -> chapter -> course
            Long courseId = lesson.getChapter().getCourse().getId();
            String courseName = lesson.getChapter().getCourse().getTitle();
            String lessonTitle = lesson.getTitle();
            String chapterTitle = lesson.getChapter().getTitle();

            String title = isLink ? "📎 Link tài liệu mới" : "Tài liệu bài học mới";
            String message = String.format(
                    "Giáo viên %s đã thêm %s cho bài học \"%s\" (Chương: %s) trong khóa học %s.\n %s",
                    teacherName,
                    isLink ? "link" : "tài liệu",
                    lessonTitle,
                    chapterTitle,
                    courseName,
                    materialName
            );

            // Tìm các lớp sử dụng course này
            List<Clazz> classes = clazzRepository.findByCourse_Id(courseId);

            Set<Long> notifiedUserIds = new HashSet<>();
            for (Clazz clazz : classes) {
                List<ClassEnrollment> enrollments = classEnrollmentRepository.findByClazz_Id(clazz.getId());
                for (ClassEnrollment enrollment : enrollments) {
                    Long studentUserId = enrollment.getStudent().getUser().getId();
                    if (!notifiedUserIds.contains(studentUserId)) {
                        notifiedUserIds.add(studentUserId);
                        String link = "/home/student/courses/" + courseId;
                        notificationService.createNotification(
                                studentUserId,
                                title,
                                message,
                                NotificationType.NEW_LESSON_AVAILABLE,
                                link
                        );
                    }
                }
            }

            log.info("Sent lesson material notification to {} students for lesson {}",
                    notifiedUserIds.size(), lesson.getId());
        } catch (Exception e) {
            log.error("Failed to send lesson material notification: {}", e.getMessage());
        }
    }

    private LessonMaterialResponse mapToResponse(LessonMaterial material) {
        return LessonMaterialResponse.builder()
                .id(material.getId())
                .lessonId(material.getLesson().getId())
                .lessonTitle(material.getLesson().getTitle())
                .fileName(material.getFileName())
                .fileUrl(material.getFileUrl())
                .fileType(material.getFileType())
                .fileSize(material.getFileSize())
                .description(material.getDescription())
                .uploadedAt(material.getUploadedAt())
                .uploadedByName(material.getUploadedBy() != null
                        ? material.getUploadedBy().getFullName() : null)
                .build();
    }
}
