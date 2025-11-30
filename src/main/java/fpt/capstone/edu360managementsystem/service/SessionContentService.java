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
import fpt.capstone.edu360managementsystem.entity.SessionContentConfig;
import fpt.capstone.edu360managementsystem.entity.SessionLesson;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.CourseChapterRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionContentConfigRepository;
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
    @Autowired
    private SessionContentConfigRepository sessionContentConfigRepository;

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

        // Persist explicit configuration: sourceType, course ids, chapter/lesson
        String sourceType = req.getSourceType();
        Long baseCourseId = (sourceType != null && sourceType.equalsIgnoreCase("ADMIN")) ? course.getId() : req.getCourseId();
        Long teacherCourseId = (sourceType != null && sourceType.equalsIgnoreCase("PERSONAL")) ? req.getTeacherCourseId() : null;
        // Fallback to the first ids from arrays if single fields not provided
        Long selectedChapterId = req.getChapterId() != null ? req.getChapterId() : (req.getChapterIds() != null && !req.getChapterIds().isEmpty() ? req.getChapterIds().get(0) : null);
        Long selectedLessonId = req.getLessonId() != null ? req.getLessonId() : (req.getLessonIds() != null && !req.getLessonIds().isEmpty() ? req.getLessonIds().get(0) : null);

        if (sourceType == null) {
            // Infer sourceType from links if not provided
            sourceType = (selectedChapterId != null) ? (chapterRepository.findById(selectedChapterId)
                    .map(ch -> ch.getCourse().getId().equals(course.getId()) ? "ADMIN" : "PERSONAL")
                    .orElse("ADMIN")) : "ADMIN";
        }

        var existingOpt = sessionContentConfigRepository.findBySession_Id(sessionId);
        SessionContentConfig cfg = existingOpt.orElseGet(() -> SessionContentConfig.builder()
                .session(session)
                .build());
        cfg.setSourceType(sourceType);
        cfg.setBaseCourseId(baseCourseId != null ? baseCourseId : course.getId());
        cfg.setTeacherCourseId(teacherCourseId);
        cfg.setChapterId(selectedChapterId);
        cfg.setLessonId(selectedLessonId);
        sessionContentConfigRepository.save(cfg);
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

        // Load existing links
        List<SessionChapter> scs = sessionChapterRepository.findBySession_Id(sessionId);
        List<SessionLesson> sls = sessionLessonRepository.findBySession_Id(sessionId);

        var chapterIds = scs.stream().map(sc -> sc.getChapter().getId()).distinct().toList();
        var lessonIds = sls.stream().map(sl -> sl.getLesson().getId()).distinct().toList();

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
                    .courseId(ch.getCourse().getId())
                    .title(ch.getTitle())
                    .description(ch.getDescription())
                    .orderIndex(ch.getOrderIndex())
                    .lessons(lessonResponses)
                    .build();
        }).toList();

        Long baseCourseId = course.getId();
        var linkedCourseIds = chapters.stream().map(ch -> ch.getCourse().getId()).distinct().toList();
        String inferredSourceType;
        Long inferredSelectedCourseId;
        if (linkedCourseIds.isEmpty() || (linkedCourseIds.size() == 1 && linkedCourseIds.get(0).equals(baseCourseId))) {
            inferredSourceType = "ADMIN";
            inferredSelectedCourseId = baseCourseId;
        } else {
            inferredSourceType = "PERSONAL";
            inferredSelectedCourseId = linkedCourseIds.stream().filter(id -> !id.equals(baseCourseId)).findFirst().orElse(baseCourseId);
        }

        var cfgOpt = sessionContentConfigRepository.findBySession_Id(session.getId());
        String respSourceType = null;
        Long respSelectedCourseId = null;
        Long respTeacherCourseId = null;
        Long respChapterId = null;
        Long respLessonId = null;
        if (cfgOpt.isPresent()) {
            var cfg = cfgOpt.get();
            respSourceType = cfg.getSourceType();
            respSelectedCourseId = (respSourceType != null && respSourceType.equalsIgnoreCase("PERSONAL")) ? cfg.getTeacherCourseId() : course.getId();
            respTeacherCourseId = cfg.getTeacherCourseId();
            respChapterId = cfg.getChapterId();
            respLessonId = cfg.getLessonId();
        }

        return SessionContentResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .courseTitle(course.getTitle())
                .baseCourseId(baseCourseId)
                .sourceType(respSourceType != null ? respSourceType : inferredSourceType)
                .selectedCourseId(respSelectedCourseId != null ? respSelectedCourseId : inferredSelectedCourseId)
                .teacherCourseId(respTeacherCourseId)
                .chapterId(respChapterId)
                .lessonId(respLessonId)
                .linkedChapterIds(chapterIds)
                .linkedLessonIds(lessonIds)
                .chapters(chapterResponses)
                .content(session.getLessonContent())
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
