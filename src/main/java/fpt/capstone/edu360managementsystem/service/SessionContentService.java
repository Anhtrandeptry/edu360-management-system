package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
import fpt.capstone.edu360managementsystem.repository.TeacherCourseVersionRepository;

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
    @Autowired
    private TeacherCourseVersionRepository teacherCourseVersionRepository;

    @Transactional
    public void upsertSessionContentByClassDate(Long userId, Long classId, String dateStr, SessionContentUpsertRequest req) {
        LocalDate date = LocalDate.parse(dateStr);
        ClassSession session = classSessionRepository.findByClazz_IdAndDate(classId, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No session found for class " + classId + " on date " + dateStr));
        log.info("➡️ upsertSessionContentByClassDate userId={}, classId={}, date={}, incomingChapters={}, incomingLessons={}",
                userId, classId, dateStr,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]");
        upsertSessionContent(userId, session.getId(), req);
    }

    @Transactional
    public void upsertSessionContent(Long userId, Long sessionId, SessionContentUpsertRequest req) {
        log.info("🔵 START upsertSessionContent: userId={}, sessionId={}, chapters={}, lessons={}, contentLength={}",
                userId,
                sessionId,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]",
                req.getContent() != null ? req.getContent().length() : 0);

        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        log.info("✅ Session found: id={}, classId={}", session.getId(), session.getClazz().getId());

        // Check giáo viên sở hữu
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this session");
        }

        Course course = session.getClazz().getCourse();
        if (course == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Class has no course linked");
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
            Long baseCourseId = course.getId();
            Long teacherId = session.getClazz().getTeacher().getId();
            log.info("📚 Processing {} chapters (baseCourseId={}, teacherId={})", req.getChapterIds().size(), baseCourseId, teacherId);
            for (Long chapId : req.getChapterIds()) {
                CourseChapter chap = chapterRepository.findById(chapId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapId));
                Long chapterCourseId = chap.getCourse().getId();

                boolean directBase = chapterCourseId.equals(baseCourseId);
                boolean mappedPersonal = !directBase && teacherCourseVersionRepository.existsByBaseCourse_IdAndTeacherCourse_IdAndTeacher_Id(baseCourseId, chapterCourseId, teacherId);

                if (!directBase && !mappedPersonal) {
                    log.warn("❌ Chapter {} rejected. chapterCourseId={}, baseCourseId={}, teacherId={}, reason=NO_MAPPING", chapId, chapterCourseId, baseCourseId, teacherId);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chương học không thuộc khóa học gốc hoặc phiên bản cá nhân được liên kết");
                }

                if (directBase) {
                    log.info("✅ Chapter {} accepted from BASE courseId={}", chapId, chapterCourseId);
                } else {
                    log.info("✅ Chapter {} accepted from PERSONAL courseId={} mapped to baseCourseId={} (teacherId={})", chapId, chapterCourseId, baseCourseId, teacherId);
                }

                SessionChapter sc = sessionChapterRepository.save(SessionChapter.builder()
                        .session(session)
                        .chapter(chap)
                        .build());
                log.info("💾 Saved SessionChapter id={} (chapId={})", sc.getId(), chapId);
            }
        }

        if (req.getLessonIds() != null) {
            Long baseCourseId = course.getId();
            Long teacherId = session.getClazz().getTeacher().getId();
            log.info("📖 Processing {} lessons (baseCourseId={}, teacherId={})", req.getLessonIds().size(), baseCourseId, teacherId);
            for (Long lessonId : req.getLessonIds()) {
                CourseLesson lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + lessonId));
                Long lessonCourseId = lesson.getChapter().getCourse().getId();

                boolean directBase = lessonCourseId.equals(baseCourseId);
                boolean mappedPersonal = !directBase && teacherCourseVersionRepository.existsByBaseCourse_IdAndTeacherCourse_IdAndTeacher_Id(baseCourseId, lessonCourseId, teacherId);

                if (!directBase && !mappedPersonal) {
                    log.warn("❌ Lesson {} rejected. lessonCourseId={}, baseCourseId={}, teacherId={}, reason=NO_MAPPING", lessonId, lessonCourseId, baseCourseId, teacherId);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài học không thuộc khóa học gốc hoặc phiên bản cá nhân được liên kết");
                }

                if (directBase) {
                    log.info("✅ Lesson {} accepted from BASE courseId={}", lessonId, lessonCourseId);
                } else {
                    log.info("✅ Lesson {} accepted from PERSONAL courseId={} mapped to baseCourseId={} (teacherId={})", lessonId, lessonCourseId, baseCourseId, teacherId);
                }

                SessionLesson sl = sessionLessonRepository.save(SessionLesson.builder()
                        .session(session)
                        .lesson(lesson)
                        .build());
                log.info("💾 Saved SessionLesson id={} (lessonId={})", sl.getId(), lessonId);
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
