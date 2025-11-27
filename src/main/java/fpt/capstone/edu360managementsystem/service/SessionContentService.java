package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.SessionContentUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.dto.response.SessionContentResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.CourseChapter;
import fpt.capstone.edu360managementsystem.entity.CourseLesson;
import fpt.capstone.edu360managementsystem.entity.SessionChapter;
import fpt.capstone.edu360managementsystem.entity.SessionLesson;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.CourseChapterRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionLessonRepository;

@Service
public class SessionContentService {

    private static final Logger log = LoggerFactory.getLogger(SessionContentService.class);

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
    public void upsertSessionContentByClassDate(Long userId, Long classId, String dateStr, SessionContentUpsertRequest req) {
        LocalDate date = LocalDate.parse(dateStr);
        ClassSession session = classSessionRepository.findByClazz_IdAndDate(classId, date)
                .orElseThrow(() -> new RuntimeException("No session found for class " + classId + " on date " + dateStr));
        upsertSessionContent(userId, session.getId(), req);
    }

    @Transactional
    public void upsertSessionContent(Long userId, Long sessionId, SessionContentUpsertRequest req) {
        log.info("🔵 START upsertSessionContent: userId={}, sessionId={}, req={}", userId, sessionId, req);

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        log.info("✅ Session found: id={}, classId={}", session.getId(), session.getClazz().getId());

        // Check giáo viên sở hữu
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner session");
        }

        Course course = session.getClazz().getCourse();
        if (course == null) {
            throw new RuntimeException("Class has no course linked");
        }
        log.info("✅ Course found: id={}, title={}", course.getId(), course.getTitle());

        // Xóa link cũ
        log.info("🗑️ Deleting old links for sessionId={}", sessionId);
        sessionChapterRepository.deleteBySession_Id(sessionId);
        sessionChapterRepository.flush();
        sessionLessonRepository.deleteBySession_Id(sessionId);
        sessionLessonRepository.flush();
        log.info("✅ Old links deleted");

        if (req.getChapterIds() != null) {
            log.info("📚 Processing {} chapters", req.getChapterIds().size());
            for (Long chapId : req.getChapterIds()) {
                CourseChapter chap = chapterRepository.findById(chapId)
                        .orElseThrow(() -> new RuntimeException("Chapter not found: " + chapId));
                if (!chap.getCourse().getId().equals(course.getId())) {
                    throw new RuntimeException("Chapter does not belong to course");
                }
                log.info("💾 Saving SessionChapter: sessionId={}, chapterId={}", sessionId, chapId);
                SessionChapter sc = sessionChapterRepository.save(SessionChapter.builder()
                        .session(session)
                        .chapter(chap)
                        .build());
                log.info("✅ Saved SessionChapter: id={}", sc.getId());
            }
        }

        if (req.getLessonIds() != null) {
            log.info("📖 Processing {} lessons", req.getLessonIds().size());
            for (Long lessonId : req.getLessonIds()) {
                CourseLesson lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new RuntimeException("Lesson not found: " + lessonId));
                if (!lesson.getChapter().getCourse().getId().equals(course.getId())) {
                    throw new RuntimeException("Lesson does not belong to course");
                }
                log.info("💾 Saving SessionLesson: sessionId={}, lessonId={}", sessionId, lessonId);
                SessionLesson sl = sessionLessonRepository.save(SessionLesson.builder()
                        .session(session)
                        .lesson(lesson)
                        .build());
                log.info("✅ Saved SessionLesson: id={}", sl.getId());
            }
        }

        // Lưu nội dung text
        log.info("💾 Saving lesson content: length={}", req.getContent() != null ? req.getContent().length() : 0);
        session.setLessonContent(req.getContent());
        classSessionRepository.save(session);
        log.info("🟢 COMPLETED upsertSessionContent successfully");
    }

    @Transactional(readOnly = true)
    public SessionContentResponse getSessionContent(Long sessionId) {
        log.info("🔵 START getSessionContent: sessionId={}", sessionId);

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));
        log.info("✅ Session found: id={}, classId={}", session.getId(), session.getClazz().getId());

        Course course = session.getClazz().getCourse();
        if (course == null) {
            throw new RuntimeException("Class has no course linked");
        }
        log.info("✅ Course found: id={}, title={}", course.getId(), course.getTitle());

        log.info("📚 Loading SessionChapters for sessionId={}", sessionId);
        List<SessionChapter> scs = sessionChapterRepository.findBySession_Id(sessionId);
        log.info("✅ Found {} SessionChapters", scs.size());

        log.info("📖 Loading SessionLessons for sessionId={}", sessionId);
        List<SessionLesson> sls = sessionLessonRepository.findBySession_Id(sessionId);
        log.info("✅ Found {} SessionLessons", sls.size());

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
                .content(session.getLessonContent()) // thêm nội dung text
                .build();
    }

    @Transactional(readOnly = true)
    public SessionContentResponse getSessionContentByClassDate(Long classId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        ClassSession session = classSessionRepository.findByClazz_IdAndDate(classId, date)
                .orElseThrow(() -> new RuntimeException("No session found for class " + classId + " on date " + dateStr));
        return getSessionContent(session.getId());
    }
}
