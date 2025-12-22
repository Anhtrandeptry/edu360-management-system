package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.*;
import fpt.capstone.edu360managementsystem.dto.response.*;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CourseService Unit Tests - 50 Cases
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseServiceTest {
    @Mock private CourseRepository courseRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private CourseChapterRepository chapterRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @InjectMocks private CourseService courseService;

    private Subject subject;
    private User adminUser;
    private User teacherUser;
    private Teacher teacher;
    private Course course;
    private CourseChapter chapter;
    private CourseLesson lesson;

    @BeforeEach
    void setUp() {
        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFullName("Admin User");

        teacherUser = new User();
        teacherUser.setId(2L);
        teacherUser.setFullName("Teacher User");

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(teacherUser);

        course = new Course();
        course.setId(1L);
        course.setSubject(subject);
        course.setTitle("Course 1");
        course.setDescription("Description");
        course.setStatus(CourseStatus.APPROVED);
        course.setCreatedBy(adminUser);

        chapter = new CourseChapter();
        chapter.setId(1L);
        chapter.setCourse(course);
        chapter.setTitle("Chapter 1");
        chapter.setOrderIndex(1);

        lesson = new CourseLesson();
        lesson.setId(1L);
        lesson.setChapter(chapter);
        lesson.setTitle("Lesson 1");
        lesson.setOrderIndex(1);
    }

    // ========== createCourse() - 15 cases ==========

    @Test void test01_createCourse_subjectNotFound() {
        CourseCreateRequest req = new CourseCreateRequest();
        req.setSubjectId(999L);
        when(subjectRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.createCourse(1L, true, req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Subject not found");
    }

    @Test void test02_createCourse_userNotFound() {
        CourseCreateRequest req = new CourseCreateRequest();
        req.setSubjectId(1L);
        req.setTitle("Test Course");
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(courseRepository.existsByTitleIgnoreCaseAndSubjectId("Test Course", 1L)).thenReturn(false);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.createCourse(999L, true, req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("User not found");
    }

    @Test void test03_createCourse_admin_statusApproved() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        CourseResponse response = courseService.createCourse(1L, true, req);
        assertThat(response.getStatus()).isEqualTo(CourseStatus.APPROVED);
    }

    @Test void test04_createCourse_teacher_statusPending() {
        CourseCreateRequest req = createValidCourseRequest();
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(2L)).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any())).thenAnswer(i -> {
            Course c = i.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        CourseResponse response = courseService.createCourse(2L, false, req);
        assertThat(response.getStatus()).isEqualTo(CourseStatus.PENDING);
    }

    @Test void test05_createCourse_teacher_ownerTeacherSet() {
        CourseCreateRequest req = createValidCourseRequest();
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(2L)).thenReturn(Optional.of(teacherUser));
        when(teacherRepository.findByUserId(2L)).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any())).thenAnswer(i -> {
            Course c = i.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        courseService.createCourse(2L, false, req);
        verify(courseRepository).save(argThat(c -> c.getOwnerTeacher() != null));
    }

    @Test void test06_createCourse_admin_ownerTeacherNull() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(argThat(c -> c.getOwnerTeacher() == null));
    }

    @Test void test07_createCourse_courseSaved() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(any(Course.class));
    }

    @Test void test08_createCourse_allFieldsMapped() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        CourseResponse response = courseService.createCourse(1L, true, req);
        assertThat(response.getTitle()).isEqualTo("Test Course");
        assertThat(response.getDescription()).isEqualTo("Test Description");
        assertThat(response.getSubjectId()).isEqualTo(1L);
    }

    @Test void test09_createCourse_transactionCommit() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(any());
    }

    @Test void test10_createCourse_returnResponseCorrect() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        CourseResponse response = courseService.createCourse(1L, true, req);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test void test11_createCourse_emptyChapters() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        CourseResponse response = courseService.createCourse(1L, true, req);
        assertThat(response.getChapters()).isEmpty();
    }

    @Test void test12_createCourse_nullDescription() {
        CourseCreateRequest req = createValidCourseRequest();
        req.setDescription(null);
        mockValidCourseCreation();
        CourseResponse response = courseService.createCourse(1L, true, req);
        assertThat(response.getDescription()).isNull();
    }

    @Test void test13_createCourse_validTitle() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(argThat(c -> c.getTitle().equals("Test Course")));
    }

    @Test void test14_createCourse_subjectLink() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(argThat(c -> c.getSubject().getId().equals(1L)));
    }

    @Test void test15_createCourse_creatorLink() {
        CourseCreateRequest req = createValidCourseRequest();
        mockValidCourseCreation();
        courseService.createCourse(1L, true, req);
        verify(courseRepository).save(argThat(c -> c.getCreatedBy().getId().equals(1L)));
    }

    // ========== listCourses() - 12 cases ==========

    @Test void test16_listCourses_noFilters_allCourses() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, null);
        assertThat(result).hasSize(1);
    }

    @Test void test17_listCourses_filterBySubjectId() {
        when(courseRepository.findBySubject_IdAndStatus(1L, CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(1L, null);
        assertThat(result).hasSize(1);
    }

    @Test void test18_listCourses_filterByStatus() {
        when(courseRepository.findByStatus(CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, CourseStatus.APPROVED);
        assertThat(result).hasSize(1);
    }

    @Test void test19_listCourses_bothFilters() {
        when(courseRepository.findBySubject_IdAndStatus(1L, CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(1L, CourseStatus.APPROVED);
        assertThat(result).hasSize(1);
    }

    @Test void test20_listCourses_subjectIdOnly_defaultApproved() {
        when(courseRepository.findBySubject_IdAndStatus(1L, CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        courseService.listCourses(1L, null);
        verify(courseRepository).findBySubject_IdAndStatus(1L, CourseStatus.APPROVED);
    }

    @Test void test21_listCourses_emptyDatabase() {
        when(courseRepository.findAll()).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, null);
        assertThat(result).isEmpty();
    }

    @Test void test22_listCourses_multipleCourses() {
        Course course2 = new Course();
        course2.setId(2L);
        course2.setSubject(subject);
        course2.setTitle("Course 2");
        course2.setStatus(CourseStatus.APPROVED);
        course2.setCreatedBy(adminUser);
        when(courseRepository.findAll()).thenReturn(List.of(course, course2));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, null);
        assertThat(result).hasSize(2);
    }

    @Test void test23_listCourses_mappingCorrect() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, null);
        assertThat(result.get(0).getTitle()).isEqualTo("Course 1");
    }

    @Test void test24_listCourses_statusFilterExact() {
        when(courseRepository.findByStatus(CourseStatus.PENDING)).thenReturn(List.of());
        when(courseRepository.findByStatus(CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, CourseStatus.APPROVED);
        assertThat(result).hasSize(1);
    }

    @Test void test25_listCourses_subjectFilterExact() {
        when(courseRepository.findBySubject_IdAndStatus(1L, CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(courseRepository.findBySubject_IdAndStatus(2L, CourseStatus.APPROVED)).thenReturn(List.of());
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(1L, null);
        assertThat(result).hasSize(1);
    }

    @Test void test26_listCourses_combinedFiltersAnd() {
        when(courseRepository.findBySubject_IdAndStatus(1L, CourseStatus.APPROVED)).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(1L, CourseStatus.APPROVED);
        assertThat(result).hasSize(1);
        verify(courseRepository).findBySubject_IdAndStatus(1L, CourseStatus.APPROVED);
    }

    @Test void test27_listCourses_chaptersNotLoaded() {
        when(courseRepository.findAll()).thenReturn(List.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        List<CourseResponse> result = courseService.listCourses(null, null);
        assertThat(result.get(0).getChapters()).isEmpty();
    }

    // ========== getCourseDetail() - 8 cases ==========

    @Test void test28_getCourseDetail_courseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.getCourseDetail(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Course not found");
    }

    @Test void test29_getCourseDetail_courseFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response).isNotNull();
    }

    @Test void test30_getCourseDetail_loadChapters() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(chapter));
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getChapters()).hasSize(1);
    }

    @Test void test31_getCourseDetail_loadLessonsPerChapter() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(chapter));
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(lesson));
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getChapters().get(0).getLessons()).hasSize(1);
    }

    @Test void test32_getCourseDetail_emptyChapters() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getChapters()).isEmpty();
    }

    @Test void test33_getCourseDetail_emptyLessons() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(chapter));
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getChapters().get(0).getLessons()).isEmpty();
    }

    @Test void test34_getCourseDetail_fullHierarchy() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(chapter));
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of(lesson));
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getChapters()).hasSize(1);
        assertThat(response.getChapters().get(0).getLessons()).hasSize(1);
    }

    @Test void test35_getCourseDetail_mappingAllFields() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        CourseResponse response = courseService.getCourseDetail(1L);
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Course 1");
        assertThat(response.getSubjectName()).isEqualTo("Math");
    }

    // ========== approveCourse() - 5 cases ==========

    @Test void test36_approveCourse_courseNotFound() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.approveCourse(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Course not found");
    }

    @Test void test37_approveCourse_courseFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
        courseService.approveCourse(1L);
        verify(courseRepository).save(any());
    }

    @Test void test38_approveCourse_statusUpdated() {
        Course pendingCourse = new Course();
        pendingCourse.setId(1L);
        pendingCourse.setStatus(CourseStatus.PENDING);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(pendingCourse));
        when(courseRepository.save(any())).thenReturn(pendingCourse);
        courseService.approveCourse(1L);
        verify(courseRepository).save(argThat(c -> c.getStatus() == CourseStatus.APPROVED));
    }

    @Test void test39_approveCourse_courseSaved() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
        courseService.approveCourse(1L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test void test40_approveCourse_transactionCommit() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any())).thenReturn(course);
        courseService.approveCourse(1L);
        verify(courseRepository).save(any());
    }

    // ========== createChapter() - 5 cases ==========

    @Test void test41_createChapter_courseNotFound() {
        ChapterCreateRequest req = new ChapterCreateRequest();
        req.setCourseId(999L);
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.createChapter(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Course not found");
    }

    @Test void test42_createChapter_chapterCreated() {
        ChapterCreateRequest req = createValidChapterRequest();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.save(any())).thenAnswer(i -> {
            CourseChapter ch = i.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        ChapterResponse response = courseService.createChapter(req);
        assertThat(response).isNotNull();
    }

    @Test void test43_createChapter_orderIndexSaved() {
        ChapterCreateRequest req = createValidChapterRequest();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.save(any())).thenAnswer(i -> {
            CourseChapter ch = i.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        courseService.createChapter(req);
        verify(chapterRepository).save(argThat(ch -> ch.getOrderIndex() == 1));
    }

    @Test void test44_createChapter_chapterSaved() {
        ChapterCreateRequest req = createValidChapterRequest();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.save(any())).thenAnswer(i -> {
            CourseChapter ch = i.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        courseService.createChapter(req);
        verify(chapterRepository).save(any(CourseChapter.class));
    }

    @Test void test45_createChapter_returnResponse() {
        ChapterCreateRequest req = createValidChapterRequest();
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(chapterRepository.save(any())).thenAnswer(i -> {
            CourseChapter ch = i.getArgument(0);
            ch.setId(1L);
            return ch;
        });
        when(lessonRepository.findByChapter_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
        ChapterResponse response = courseService.createChapter(req);
        assertThat(response.getTitle()).isEqualTo("Chapter 1");
    }

    // ========== createLesson() - 5 cases ==========

    @Test void test46_createLesson_chapterNotFound() {
        LessonCreateRequest req = new LessonCreateRequest();
        req.setChapterId(999L);
        when(chapterRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> courseService.createLesson(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Chapter not found");
    }

    @Test void test47_createLesson_lessonCreated() {
        LessonCreateRequest req = createValidLessonRequest();
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(lessonRepository.save(any())).thenAnswer(i -> {
            CourseLesson l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        LessonResponse response = courseService.createLesson(req);
        assertThat(response).isNotNull();
    }

    @Test void test48_createLesson_orderIndexSaved() {
        LessonCreateRequest req = createValidLessonRequest();
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(lessonRepository.save(any())).thenAnswer(i -> {
            CourseLesson l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        courseService.createLesson(req);
        verify(lessonRepository).save(argThat(l -> l.getOrderIndex() == 1));
    }

    @Test void test49_createLesson_lessonSaved() {
        LessonCreateRequest req = createValidLessonRequest();
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(lessonRepository.save(any())).thenAnswer(i -> {
            CourseLesson l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        courseService.createLesson(req);
        verify(lessonRepository).save(any(CourseLesson.class));
    }

    @Test void test50_createLesson_returnResponse() {
        LessonCreateRequest req = createValidLessonRequest();
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(lessonRepository.save(any())).thenAnswer(i -> {
            CourseLesson l = i.getArgument(0);
            l.setId(1L);
            return l;
        });
        LessonResponse response = courseService.createLesson(req);
        assertThat(response.getTitle()).isEqualTo("Lesson 1");
    }

    // Helper methods
    private CourseCreateRequest createValidCourseRequest() {
        CourseCreateRequest req = new CourseCreateRequest();
        req.setSubjectId(1L);
        req.setTitle("Test Course");
        req.setDescription("Test Description");
        return req;
    }

    private void mockValidCourseCreation() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(courseRepository.save(any())).thenAnswer(i -> {
            Course c = i.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(chapterRepository.findByCourse_IdOrderByOrderIndexAsc(anyLong())).thenReturn(List.of());
    }

    private ChapterCreateRequest createValidChapterRequest() {
        ChapterCreateRequest req = new ChapterCreateRequest();
        req.setCourseId(1L);
        req.setTitle("Chapter 1");
        req.setDescription("Chapter Description");
        req.setOrderIndex(1);
        return req;
    }

    private LessonCreateRequest createValidLessonRequest() {
        LessonCreateRequest req = new LessonCreateRequest();
        req.setChapterId(1L);
        req.setTitle("Lesson 1");
        req.setDescription("Lesson Description");
        req.setOrderIndex(1);
        return req;
    }
}
