package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.ChapterCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.request.LessonCreateRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.CourseResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.CourseChapter;
import fpt.capstone.edu360managementsystem.entity.CourseLesson;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.repository.CourseChapterRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
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

    @Transactional
    public CourseResponse createCourse(Long currentUserId, boolean isAdmin, CourseCreateRequest req) {
        Subject subject = subjectRepository.findById(req.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

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

        return courses.stream()
                .map(c -> mapCourse(c, null))
                .toList();
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
    public void updateCourse(Long courseId, CourseUpdateRequest req) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

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

        // Reset status to PENDING after edit (requires re-approval)
        course.setStatus(CourseStatus.PENDING);
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

        return CourseResponse.builder()
                .id(c.getId())
                .subjectId(c.getSubject().getId())
                .subjectName(c.getSubject().getName())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .createdByUserId(c.getCreatedBy().getId())
                .createdByName(c.getCreatedBy().getFullName())
                .ownerTeacherId(c.getOwnerTeacher() != null ? c.getOwnerTeacher().getId() : null)
                .ownerTeacherName(c.getOwnerTeacher() != null ? c.getOwnerTeacher().getUser().getFullName() : null)
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
