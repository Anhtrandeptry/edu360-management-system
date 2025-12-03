package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.SessionContentUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.SessionContentResponse;
import fpt.capstone.edu360managementsystem.entity.*;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionContentServiceTest {
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private SessionChapterRepository sessionChapterRepository;
    @Mock private SessionLessonRepository sessionLessonRepository;
    @Mock private CourseChapterRepository chapterRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @InjectMocks private SessionContentService sessionContentService;

    private ClassSession session;
    private Clazz clazz;
    private Course course;
    private Teacher teacher;
    private User user;
    private Subject subject;
    private CourseChapter chapter;
    private CourseLesson lesson;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(user);

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");

        course = new Course();
        course.setId(1L);
        course.setTitle("Math Course");

        clazz = new Clazz();
        clazz.setId(1L);
        clazz.setName("Math 101");
        clazz.setTeacher(teacher);
        clazz.setCourse(course);
        clazz.setSubject(subject);

        session = new ClassSession();
        session.setId(1L);
        session.setClazz(clazz);

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

    // upsertSessionContent - 18 cases

    @Test void test01_upsert_sessionNotFound() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("Session not found");
    }

    @Test void test02_upsert_notOwner() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(999L, 1L, req))
            .hasMessageContaining("Not owner");
    }

    @Test void test03_upsert_owner_success() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(1L);
        doNothing().when(sessionLessonRepository).deleteBySession_Id(1L);
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).deleteBySession_Id(1L);
    }

    @Test void test04_upsert_courseNull_error() {
        clazz.setCourse(null);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("no course");
    }

    @Test void test05_upsert_deleteOldLinks() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(1L);
        doNothing().when(sessionLessonRepository).deleteBySession_Id(1L);
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).deleteBySession_Id(1L);
        verify(sessionLessonRepository).deleteBySession_Id(1L);
    }

    @Test void test06_upsert_emptyChapterIds() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository, never()).save(any());
    }

    @Test void test07_upsert_emptyLessonIds() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionLessonRepository, never()).save(any());
    }

    @Test void test08_upsert_validChapterIds() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(sessionChapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(1L));
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).save(any());
    }

    @Test void test09_upsert_validLessonIds() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(sessionLessonRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of(1L));
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionLessonRepository).save(any());
    }

    @Test void test10_upsert_chapterNotFound() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(999L)).thenReturn(Optional.empty());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(999L));
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("Chapter not found");
    }

    @Test void test11_upsert_lessonNotFound() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(lessonRepository.findById(999L)).thenReturn(Optional.empty());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of(999L));
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("Lesson not found");
    }

    @Test void test12_upsert_chapterNotInCourse() {
        CourseChapter wrongChapter = new CourseChapter();
        wrongChapter.setId(2L);
        Course wrongCourse = new Course();
        wrongCourse.setId(999L);
        wrongChapter.setCourse(wrongCourse);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(2L)).thenReturn(Optional.of(wrongChapter));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(2L));
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("does not belong to course");
    }

    @Test void test13_upsert_lessonNotInCourse() {
        CourseLesson wrongLesson = new CourseLesson();
        wrongLesson.setId(2L);
        CourseChapter wrongChapter = new CourseChapter();
        Course wrongCourse = new Course();
        wrongCourse.setId(999L);
        wrongChapter.setCourse(wrongCourse);
        wrongLesson.setChapter(wrongChapter);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(lessonRepository.findById(2L)).thenReturn(Optional.of(wrongLesson));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of(2L));
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("does not belong to course");
    }

    @Test void test14_upsert_mixedValidInvalid_rollback() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.findById(999L)).thenReturn(Optional.empty());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(1L, 999L));
        assertThatThrownBy(() -> sessionContentService.upsertSessionContent(1L, 1L, req))
            .hasMessageContaining("Chapter not found");
    }

    @Test void test15_upsert_allValid_commit() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(sessionChapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sessionLessonRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(1L));
        req.setLessonIds(List.of(1L));
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).save(any());
        verify(sessionLessonRepository).save(any());
    }

    @Test void test16_upsert_transactionHandling() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of());
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).deleteBySession_Id(1L);
    }

    @Test void test17_upsert_bidirectionalLinks() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(sessionChapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(1L));
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository).save(any());
    }

    @Test void test18_upsert_multipleChapters() {
        CourseChapter chapter2 = new CourseChapter();
        chapter2.setId(2L);
        chapter2.setCourse(course);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doNothing().when(sessionChapterRepository).deleteBySession_Id(anyLong());
        doNothing().when(sessionLessonRepository).deleteBySession_Id(anyLong());
        when(chapterRepository.findById(1L)).thenReturn(Optional.of(chapter));
        when(chapterRepository.findById(2L)).thenReturn(Optional.of(chapter2));
        when(sessionChapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        SessionContentUpsertRequest req = new SessionContentUpsertRequest();
        req.setChapterIds(List.of(1L, 2L));
        req.setLessonIds(List.of());
        sessionContentService.upsertSessionContent(1L, 1L, req);
        verify(sessionChapterRepository, times(2)).save(any());
    }

    // getSessionContent - 12 cases
    @Test void test19_get_sessionNotFound() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> sessionContentService.getSessionContent(1L))
            .hasMessageContaining("Session not found");
    }

    @Test void test20_get_courseNull_error() {
        clazz.setCourse(null);
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        assertThatThrownBy(() -> sessionContentService.getSessionContent(1L))
            .hasMessageContaining("no course");
    }

    @Test void test21_get_noLinkedContent() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(anyList())).thenReturn(List.of());
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters()).isEmpty();
    }

    @Test void test22_get_hasChapters() {
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters()).hasSize(1);
    }

    @Test void test23_get_hasLessons() {
        SessionLesson sl = SessionLesson.builder().session(session).lesson(lesson).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of(sl));
        when(chapterRepository.findAllById(anyList())).thenReturn(List.of());
        when(lessonRepository.findAllById(List.of(1L))).thenReturn(List.of(lesson));
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result).isNotNull();
    }

    @Test void test24_get_chaptersWithLessons() {
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        SessionLesson sl = SessionLesson.builder().session(session).lesson(lesson).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of(sl));
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(List.of(1L))).thenReturn(List.of(lesson));
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters()).hasSize(1);
        assertThat(result.getChapters().get(0).getLessons()).hasSize(1);
    }

    @Test void test25_get_emptyChapters() {
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(anyList())).thenReturn(List.of());
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters()).isEmpty();
    }

    @Test void test26_get_emptyLessons() {
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters().get(0).getLessons()).isEmpty();
    }

    @Test void test27_get_mappingAllFields() {
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getClassName()).isEqualTo("Math 101");
        assertThat(result.getSubjectName()).isEqualTo("Math");
        assertThat(result.getCourseTitle()).isEqualTo("Math Course");
    }

    @Test void test28_get_orderPreserved() {
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters().get(0).getOrderIndex()).isEqualTo(1);
    }

    @Test void test29_get_multipleChapters() {
        CourseChapter chapter2 = new CourseChapter();
        chapter2.setId(2L);
        chapter2.setCourse(course);
        chapter2.setTitle("Chapter 2");
        SessionChapter sc1 = SessionChapter.builder().session(session).chapter(chapter).build();
        SessionChapter sc2 = SessionChapter.builder().session(session).chapter(chapter2).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc1, sc2));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of());
        when(chapterRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(chapter, chapter2));
        when(lessonRepository.findAllById(anyList())).thenReturn(List.of());
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters()).hasSize(2);
    }

    @Test void test30_get_multipleLessons() {
        CourseLesson lesson2 = new CourseLesson();
        lesson2.setId(2L);
        lesson2.setChapter(chapter);
        lesson2.setTitle("Lesson 2");
        SessionChapter sc = SessionChapter.builder().session(session).chapter(chapter).build();
        SessionLesson sl1 = SessionLesson.builder().session(session).lesson(lesson).build();
        SessionLesson sl2 = SessionLesson.builder().session(session).lesson(lesson2).build();
        when(classSessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionChapterRepository.findBySession_Id(1L)).thenReturn(List.of(sc));
        when(sessionLessonRepository.findBySession_Id(1L)).thenReturn(List.of(sl1, sl2));
        when(chapterRepository.findAllById(List.of(1L))).thenReturn(List.of(chapter));
        when(lessonRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(lesson, lesson2));
        SessionContentResponse result = sessionContentService.getSessionContent(1L);
        assertThat(result.getChapters().get(0).getLessons()).hasSize(2);
    }
}
