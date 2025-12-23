package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.ChapterCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.request.LessonCreateRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.CourseResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.CourseChapter;
import fpt.capstone.edu360managementsystem.entity.CourseLesson;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.CourseChapterRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionLessonRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private CourseChapterRepository chapterRepository;
    @Autowired
    private CourseLessonRepository lessonRepository;
    @Autowired
    private SessionLessonRepository sessionLessonRepository;
    @Autowired
    private SessionChapterRepository sessionChapterRepository;
    @Autowired
    private ClazzRepository clazzRepository;

    @Transactional
    public CourseResponse createCourse(Long currentUserId, boolean isAdmin, CourseCreateRequest req) {
        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        // Validate: Kiểm tra trùng tên khóa học trong cùng môn học
        if (courseRepository.existsByTitleIgnoreCaseAndSubjectId(req.getTitle().trim(), req.getSubjectId())) {
            throw new RuntimeException("Tên khóa học \"" + req.getTitle().trim() + "\" đã tồn tại trong môn học này. Vui lòng chọn tên khác.");
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CourseStatus status = isAdmin ? CourseStatus.APPROVED : CourseStatus.PENDING;
        Teacher ownerTeacher = null;
        if (!isAdmin) {
            ownerTeacher = teacherRepository.findByUserId(currentUserId).orElse(null);
        }

        Course course = Course.builder()
                .subject(subject)
                .title(req.getTitle())
                .description(req.getDescription())
                .status(status)
                .createdBy(creator)
                .ownerTeacher(ownerTeacher)
                .build();
        course = courseRepository.save(course);

        return mapCourse(course, List.of());
    }

    public List<CourseResponse> listCourses(Long subjectId, CourseStatus status) {
        return listCourses(subjectId, status, false);
    }

    public List<CourseResponse> listCourses(Long subjectId, CourseStatus status, boolean excludeHidden) {
        List<Course> courses;
        if (subjectId != null && status != null) {
            courses = courseRepository.findBySubject_IdAndStatus(subjectId, status);
        } else if (subjectId != null) {
            // tất cả theo subject
            courses = courseRepository.findBySubject_IdAndStatus(subjectId, CourseStatus.APPROVED);
        } else if (status != null) {
            courses = courseRepository.findByStatus(status);
        } else {
            courses = courseRepository.findAll();
        }

        // Filter hidden courses if requested
        if (excludeHidden) {
            courses = courses.stream()
                    .filter(c -> c.getHidden() == null || !c.getHidden())
                    .toList();
        }

        return courses.stream()
                .map(c -> mapCourse(c, null))
                .toList();
    }

    /**
     * Lấy danh sách courses với phân trang và filter
     *
     * @param search từ khóa tìm kiếm (title, description, teacherName)
     * @param status filter theo CourseStatus (DRAFT, PENDING, APPROVED,
     * ARCHIVED)
     * @param subjectId filter theo môn học
     * @param teacherUserId filter theo giáo viên tạo
     * @param page số trang (bắt đầu từ 0)
     * @param size số phần tử mỗi trang
     * @param sortBy trường để sắp xếp
     * @param order thứ tự sắp xếp (asc, desc)
     * @return Page<CourseResponse>
     */
    @Transactional(readOnly = true)
    public Page<CourseResponse> getCoursesWithPagination(
            String search,
            String status,
            Long subjectId,
            Long teacherUserId,
            int page,
            int size,
            String sortBy,
            String order
    ) {
        // Xử lý sort
        Sort sort = Sort.by(sortBy != null ? sortBy : "id");
        if ("desc".equalsIgnoreCase(order)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        // Xử lý status filter
        CourseStatus statusEnum = null;
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusEnum = CourseStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }

        // Query với pagination
        Page<Course> coursePage = courseRepository.findBySearchAndFilters(
                search, statusEnum, subjectId, teacherUserId, pageable
        );

        // Map to response
        return coursePage.map(c -> mapCourse(c, null));
    }

    public List<CourseResponse> listCoursesOfTeacher(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
        if (teacher == null) {
            return List.of();
        }
        List<Course> courses = courseRepository.findByOwnerTeacher_Id(teacher.getId());
        return courses.stream().map(c -> mapCourse(c, null)).toList();
    }

    public CourseResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<CourseChapter> chapters = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(courseId);
        return mapCourse(course, chapters);
    }

    @Transactional
    public void approveCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setStatus(CourseStatus.APPROVED);
        courseRepository.save(course);
    }

    @Transactional
    public void rejectCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setStatus(CourseStatus.REJECTED);
        courseRepository.save(course);
    }

    @Transactional
    public void updateCourse(Long userId, Long courseId, CourseUpdateRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Security check: Verify the user is the owner of the course
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (course.getOwnerTeacher() == null || !course.getOwnerTeacher().getId().equals(teacher.getId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa khóa học này");
        }

        // Update fields if provided
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            course.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            course.setDescription(req.getDescription());
        }
        if (req.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(req.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("Subject not found"));
            course.setSubject(subject);
        }

        // Giữ nguyên trạng thái hiện tại sau khi chỉnh sửa trong module cá nhân
        // Nghiệp vụ mới: KHÔNG reset về PENDING
        courseRepository.save(course);
    }

    // --- Chapter & Lesson ---
    @Transactional
    public ChapterResponse createChapter(ChapterCreateRequest req) {
        Course course = courseRepository.findById(req.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        CourseChapter chapter = CourseChapter.builder()
                .course(course)
                .title(req.getTitle())
                .description(req.getDescription())
                .orderIndex(req.getOrderIndex())
                .build();
        chapter = chapterRepository.save(chapter);
        return mapChapter(chapter, List.of());
    }

    @Transactional
    public ChapterResponse updateChapter(Long chapterId, ChapterCreateRequest req) {
        CourseChapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        if (req.getTitle() != null) {
            chapter.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            chapter.setDescription(req.getDescription());
        }
        if (req.getOrderIndex() != null) {
            chapter.setOrderIndex(req.getOrderIndex());
        }
        chapter = chapterRepository.save(chapter);
        return mapChapter(chapter, null);
    }

    @Transactional
    public LessonResponse createLesson(LessonCreateRequest req) {
        CourseChapter chapter = chapterRepository.findById(req.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapter not found"));
        CourseLesson lesson = CourseLesson.builder()
                .chapter(chapter)
                .title(req.getTitle())
                .description(req.getDescription())
                .orderIndex(req.getOrderIndex())
                .build();
        lesson = lessonRepository.save(lesson);
        return mapLesson(lesson);
    }

    @Transactional
    public LessonResponse updateLesson(Long lessonId, LessonCreateRequest req) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        if (req.getTitle() != null) {
            lesson.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            lesson.setDescription(req.getDescription());
        }
        if (req.getOrderIndex() != null) {
            lesson.setOrderIndex(req.getOrderIndex());
        }
        lesson = lessonRepository.save(lesson);
        return mapLesson(lesson);
    }

    // Xóa toàn bộ bài học trong chương rồi xóa chương
    @Transactional
    public void removeChapter(Long chapterId) {
        CourseChapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("Chapter not found"));

        // Xóa liên kết session_chapters trước
        sessionChapterRepository.deleteByChapter_Id(chapterId);

        List<CourseLesson> lessons = lessonRepository.findByChapter_IdOrderByOrderIndexAsc(chapterId);
        if (lessons != null && !lessons.isEmpty()) {
            for (CourseLesson l : lessons) {
                // Xóa liên kết session_lessons trước
                sessionLessonRepository.deleteByLesson_Id(l.getId());
                lessonRepository.delete(l);
            }
        }
        chapterRepository.delete(chapter);
    }

    // Xóa một bài học
    @Transactional
    public void removeLesson(Long lessonId) {
        CourseLesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        // Xóa liên kết session_lessons trước
        sessionLessonRepository.deleteByLesson_Id(lessonId);
        lessonRepository.delete(lesson);
    }

    /**
     * Ẩn/hiện khóa học. Khóa học bị ẩn sẽ không hiển thị trên landing page.
     *
     * @param id ID khóa học
     * @param hidden true để ẩn, false để hiện
     * @return CourseResponse sau khi cập nhật
     */
    @Transactional
    public CourseResponse toggleCourseHidden(Long id, boolean hidden) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học với ID: " + id));

        course.setHidden(hidden);
        courseRepository.save(course);

        return mapCourse(course, null);
    }

    // --- Mapping helpers ---
    private CourseResponse mapCourse(Course c, List<CourseChapter> chaptersOpt) {
        List<ChapterResponse> chapterResponses = null;
        List<CourseChapter> chapters = chaptersOpt;
        if (chapters == null) {
            chapters = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(c.getId());
        }
        chapterResponses = chapters.stream()
                .map(ch -> {
                    List<CourseLesson> lessons = lessonRepository.findByChapter_IdOrderByOrderIndexAsc(ch.getId());
                    return mapChapter(ch, lessons);
                }).toList();

        // Find linked class for this course
        Long classId = null;
        String className = null;
        java.time.LocalDate classEndDate = null;
        List<Clazz> linkedClasses = clazzRepository.findByCourse_Id(c.getId());
        if (!linkedClasses.isEmpty()) {
            Clazz linkedClass = linkedClasses.get(0); // Get first linked class
            classId = linkedClass.getId();
            className = linkedClass.getName();
            classEndDate = linkedClass.getEndDate();
        }

        return CourseResponse.builder()
                .id(c.getId())
                .subjectId(c.getSubject().getId())
                .subjectName(c.getSubject().getName())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .hidden(c.getHidden() != null ? c.getHidden() : false)
                .createdByUserId(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getFullName())
                .ownerTeacherId(c.getOwnerTeacher() != null ? c.getOwnerTeacher().getId() : null)
                .ownerTeacherName(c.getOwnerTeacher() != null ? c.getOwnerTeacher().getUser().getFullName() : null)
                .createdAt(c.getCreatedAt())
                .classId(classId)
                .className(className)
                .classEndDate(classEndDate)
                .chapters(chapterResponses)
                .build();
    }

    private ChapterResponse mapChapter(CourseChapter ch, List<CourseLesson> lessonsOpt) {
        List<CourseLesson> lessons = lessonsOpt != null ? lessonsOpt
                : lessonRepository.findByChapter_IdOrderByOrderIndexAsc(ch.getId());

        return ChapterResponse.builder()
                .id(ch.getId())
                .courseId(ch.getCourse().getId())
                .title(ch.getTitle())
                .description(ch.getDescription())
                .orderIndex(ch.getOrderIndex())
                .lessons(lessons.stream().map(this::mapLesson).toList())
                .build();
    }

    private LessonResponse mapLesson(CourseLesson l) {
        return LessonResponse.builder()
                .id(l.getId())
                .chapterId(l.getChapter().getId())
                .title(l.getTitle())
                .description(l.getDescription())
                .orderIndex(l.getOrderIndex())
                .build();
    }
}
