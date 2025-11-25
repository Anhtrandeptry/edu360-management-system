package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.SessionContentUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.dto.response.SessionContentResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SessionContentService {

    @Autowired
    private ClassSessionRepository classSessionRepository;
    @Autowired
    private SessionChapterRepository sessionChapterRepository;
    @Autowired
    private SessionLessonRepository sessionLessonRepository;
    @Autowired
    private CourseChapterRepository chapterRepository;
    @Autowired
    private CourseLessonRepository lessonRepository;

    @Transactional
    public void upsertSessionContent(Long userId, Long sessionId, SessionContentUpsertRequest req) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        // Check giáo viên sở hữu
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner session");
        }

        Course course = session.getClazz().getCourse();
        if (course == null) {
            throw new RuntimeException("Class has no course linked");
        }

        // Xóa link cũ
        sessionChapterRepository.deleteBySession_Id(sessionId);
        sessionLessonRepository.deleteBySession_Id(sessionId);

        if (req.getChapterIds() != null) {
            for (Long chapId : req.getChapterIds()) {
                CourseChapter chap = chapterRepository.findById(chapId)
                        .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapId));
                if (!chap.getCourse().getId().equals(course.getId())) {
                    throw new RuntimeException("Chapter does not belong to course");
                }
                sessionChapterRepository.save(SessionChapter.builder()
                        .session(session)
                        .chapter(chap)
                        .build());
            }
        }

        if (req.getLessonIds() != null) {
            for (Long lessonId : req.getLessonIds()) {
                CourseLesson lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
                if (!lesson.getChapter().getCourse().getId().equals(course.getId())) {
                    throw new RuntimeException("Lesson does not belong to course");
                }
                sessionLessonRepository.save(SessionLesson.builder()
                        .session(session)
                        .lesson(lesson)
                        .build());
            }
        }
    }

    @Transactional(readOnly = true)
    public SessionContentResponse getSessionContent(Long sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        Course course = session.getClazz().getCourse();
        if (course == null) {
            throw new RuntimeException("Class has no course linked");
        }

        List<SessionChapter> scs = sessionChapterRepository.findBySession_Id(sessionId);
        List<SessionLesson> sls = sessionLessonRepository.findBySession_Id(sessionId);

        var chapterIds = scs.stream().map(sc -> sc.getChapter().getId()).distinct().toList();
        var lessonIds = sls.stream().map(sl -> sl.getLesson().getId()).distinct().toList();

        // load chapters + lessons theo danh sách đã link
        List<CourseChapter> chapters = chapterRepository.findAllById(chapterIds);
        List<CourseLesson> lessons = lessonRepository.findAllById(lessonIds);

        List<ChapterResponse> chapterResponses = chapters.stream().map(ch -> {
            List<LessonResponse> lessonResponses = lessons.stream()
                    .filter(l -> l.getChapter().getId().equals(ch.getId()))
                    .map(l -> LessonResponse.builder()
                            .id(l.getId())
                            .chapterId(ch.getId())
                            .title(l.getTitle())
                            .description(l.getDescription())
                            .orderIndex(l.getOrderIndex())
                            .build())
                    .toList();
            return ChapterResponse.builder()
                    .id(ch.getId())
                    .courseId(course.getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .orderIndex(ch.getOrderIndex())
                    .lessons(lessonResponses)
                    .build();
        }).toList();

        return SessionContentResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .courseTitle(course.getTitle())
                .chapters(chapterResponses)
                .build();
    }
}
