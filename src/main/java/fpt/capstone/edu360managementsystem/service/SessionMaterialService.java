package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.SessionMaterialResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.SessionMaterial;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.SessionMaterialRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionMaterialService {

    private final SessionMaterialRepository materialRepository;
    private final ClassSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final NotificationService notificationService;

    @Value("${app.upload.materials-dir:uploads/materials}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Upload tài liệu cho buổi học
     */
    @Transactional
    public SessionMaterialResponse uploadMaterial(Long sessionId, MultipartFile file, 
                                                   String description, Long userId) throws IOException {
        log.info("Uploading material for session {} by user {}", sessionId, userId);
        
        // Validate session
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học với ID: " + sessionId));
        
        // Validate user
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Tạo thư mục nếu chưa tồn tại
        Path uploadPath = Paths.get(uploadDir, "session-" + sessionId);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Tạo tên file unique
        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        
        // Lưu file
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Tạo URL để truy cập file
        String fileUrl = baseUrl + "/api/materials/download/" + sessionId + "/" + uniqueFileName;
        
        // Lưu metadata vào database
        SessionMaterial material = SessionMaterial.builder()
                .session(session)
                .fileName(originalFileName)
                .fileUrl(fileUrl)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .description(description)
                .uploadedBy(uploader)
                .build();
        
        material = materialRepository.save(material);
        log.info("Material saved with ID: {}", material.getId());
        
        // Gửi thông báo cho học sinh trong lớp
        sendMaterialNotification(session, originalFileName, uploader.getFullName(), false);
        
        return mapToResponse(material);
    }

    /**
     * Lấy danh sách tài liệu của session
     */
    public List<SessionMaterialResponse> getMaterialsBySession(Long sessionId) {
        return materialRepository.findBySession_IdOrderByUploadedAtDesc(sessionId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Thêm link tài liệu cho buổi học
     */
    @Transactional
    public SessionMaterialResponse addLink(Long sessionId, String url, String title, 
                                           String description, Long userId) {
        log.info("Adding link for session {} by user {}: {}", sessionId, userId, url);
        
        // Validate session
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy buổi học với ID: " + sessionId));
        
        // Validate user
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        // Sử dụng title nếu có, không thì dùng URL
        String displayName = (title != null && !title.isBlank()) ? title : url;
        
        // Lưu metadata vào database với fileType = "LINK"
        SessionMaterial material = SessionMaterial.builder()
                .session(session)
                .fileName(displayName)
                .fileUrl(url)
                .fileType("LINK")
                .fileSize(0L)
                .description(description)
                .uploadedBy(uploader)
                .build();
        
        material = materialRepository.save(material);
        log.info("Link saved with ID: {}", material.getId());
        
        // Gửi thông báo cho học sinh trong lớp
        sendMaterialNotification(session, displayName, uploader.getFullName(), true);
        
        return mapToResponse(material);
    }

    /**
     * Gửi thông báo cho học sinh khi có tài liệu mới
     */
    private void sendMaterialNotification(ClassSession session, String materialName, 
                                          String teacherName, boolean isLink) {
        try {
            Long classId = session.getClazz().getId();
            String className = session.getClazz().getName();
            String slotInfo = "Tiết " + session.getTimeSlot().getId() + 
                              " (" + session.getTimeSlot().getStartTime() + " - " + 
                              session.getTimeSlot().getEndTime() + ")";
            String dateInfo = session.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            
            String title = isLink ? "📎 Link tài liệu mới" : "Tài liệu mới";
            String message = String.format(
                "Giáo viên %s đã đăng %s cho lớp %s, %s ngày %s.\n %s",
                teacherName,
                isLink ? "link tài liệu" : "tài liệu",
                className,
                slotInfo,
                dateInfo,
                materialName
            );
            
            // Lấy danh sách học sinh trong lớp
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByClazz_Id(classId);
            
            for (ClassEnrollment enrollment : enrollments) {
                Long studentUserId = enrollment.getStudent().getUser().getId();
                String link = "/home/student/schedule"; // Link đến trang lịch học
                notificationService.createNotification(
                    studentUserId, 
                    title, 
                    message, 
                    NotificationType.NEW_LESSON_AVAILABLE, 
                    link
                );
            }
            
            log.info("Sent material notification to {} students for session {}", 
                    enrollments.size(), session.getId());
        } catch (Exception e) {
            log.error("Failed to send material notification: {}", e.getMessage());
        }
    }

    /**
     * Lấy tài liệu của nhiều sessions (batch query cho hiệu suất)
     */
    public Map<Long, List<SessionMaterialResponse>> getMaterialsBySessionIds(List<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        List<SessionMaterial> materials = materialRepository.findBySession_IdIn(sessionIds);
        
        return materials.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getSession().getId(),
                        Collectors.mapping(this::mapToResponse, Collectors.toList())
                ));
    }

    /**
     * Xóa tài liệu
     */
    @Transactional
    public void deleteMaterial(Long materialId, Long userId) throws IOException {
        SessionMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
        
        // Kiểm tra quyền (chỉ người upload mới được xóa, hoặc admin)
        // TODO: Thêm logic kiểm tra quyền
        
        // Xóa file vật lý
        try {
            String fileUrl = material.getFileUrl();
            if (fileUrl.contains("/download/")) {
                String[] parts = fileUrl.split("/download/")[1].split("/");
                if (parts.length >= 2) {
                    Path filePath = Paths.get(uploadDir, "session-" + parts[0], parts[1]);
                    Files.deleteIfExists(filePath);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to delete physical file: {}", e.getMessage());
        }
        
        // Xóa record trong database
        materialRepository.delete(material);
        log.info("Material {} deleted by user {}", materialId, userId);
    }

    /**
     * Lấy đường dẫn file để download
     */
    public Path getFilePath(Long sessionId, String fileName) {
        return Paths.get(uploadDir, "session-" + sessionId, fileName);
    }

    /**
     * Lấy thông tin material theo ID
     */
    public SessionMaterialResponse getMaterialById(Long materialId) {
        return materialRepository.findById(materialId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu"));
    }

    private SessionMaterialResponse mapToResponse(SessionMaterial material) {
        return SessionMaterialResponse.builder()
                .id(material.getId())
                .sessionId(material.getSession().getId())
                .fileName(material.getFileName())
                .fileUrl(material.getFileUrl())
                .fileType(material.getFileType())
                .fileSize(material.getFileSize())
                .description(material.getDescription())
                .uploadedAt(material.getUploadedAt())
                .uploadedByName(material.getUploadedBy() != null ? 
                        material.getUploadedBy().getFullName() : null)
                .build();
    }
}
