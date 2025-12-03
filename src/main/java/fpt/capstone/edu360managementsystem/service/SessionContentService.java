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
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.CourseChapterRepository;
import fpt.capstone.edu360managementsystem.repository.CourseLessonRepository;
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionContentConfigRepository;
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
    @Autowired
    private CourseRepository courseRepository;
    // BỎ logic phiên bản/mapping
    @Autowired
    private SessionContentConfigRepository sessionContentConfigRepository;

    @Transactional
    public void upsertSessionContentByClassDate(Long userId, Long classId, String dateStr, Long slotId, SessionContentUpsertRequest req) {
        LocalDate date = LocalDate.parse(dateStr);
        ClassSession session;
        if (slotId != null) {
            session = classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No session found for class " + classId + " on date " + dateStr + " with slotId=" + slotId));
        } else {
            // Avoid IncorrectResultSize: fetch list and choose earliest time slot
            List<ClassSession> sameDay = classSessionRepository
                    .findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (sameDay.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No session found for class " + classId + " on date " + dateStr);
            }
            if (sameDay.size() > 1) {
                log.warn("Multiple sessions found for class {} on {}. Consider passing slotId. Picking earliest.", classId, dateStr);
            }
            session = sameDay.get(0);
        }
        log.info("➡️ upsertSessionContentByClassDate userId={}, classId={}, date={}, slotId={}, incomingChapters={}, incomingLessons={}",
                userId, classId, dateStr, slotId,
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
            for (Long chapId : req.getChapterIds()) {
                CourseChapter chap = chapterRepository.findById(chapId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found: " + chapId));
                // Cho phép: chapter thuộc course môn (ADMIN) hoặc course của lớp (CLASS_PERSONAL)
                SessionChapter sc = sessionChapterRepository.save(SessionChapter.builder()
                        .session(session)
                        .chapter(chap)
                        .build());
                log.info("💾 Saved SessionChapter id={} (chapId={})", sc.getId(), chapId);
            }
        }

        if (req.getLessonIds() != null) {
            for (Long lessonId : req.getLessonIds()) {
                CourseLesson lesson = lessonRepository.findById(lessonId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found: " + lessonId));
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
        Long baseCourseId = course.getId();
        Long classCourseId = "CLASS_PERSONAL".equalsIgnoreCase(sourceType) ? req.getClassCourseId() : null;
        if ("CLASS_PERSONAL".equalsIgnoreCase(sourceType) && classCourseId == null) {
            // Tạo khóa học của lớp nếu chưa có và lấy id
            classCourseId = ensureClassCourse(session).getId();
        }
        // Fallback to the first ids from arrays if single fields not provided
        Long selectedChapterId = req.getChapterId() != null ? req.getChapterId() : (req.getChapterIds() != null && !req.getChapterIds().isEmpty() ? req.getChapterIds().get(0) : null);
        Long selectedLessonId = req.getLessonId() != null ? req.getLessonId() : (req.getLessonIds() != null && !req.getLessonIds().isEmpty() ? req.getLessonIds().get(0) : null);

        if (sourceType == null) {
            sourceType = "ADMIN";
        }

        var existingOpt = sessionContentConfigRepository.findBySession_Id(sessionId);
        SessionContentConfig cfg = existingOpt.orElseGet(() -> SessionContentConfig.builder()
                .session(session)
                .build());
        cfg.setSourceType(sourceType);
        cfg.setBaseCourseId(baseCourseId != null ? baseCourseId : course.getId());
        cfg.setTeacherCourseId(classCourseId); // Dùng teacherCourseId để lưu khóa học clone của lớp
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
        String inferredSourceType = "ADMIN";
        Long inferredClassCourseId = null;
        if (!linkedCourseIds.isEmpty() && !linkedCourseIds.contains(baseCourseId)) {
            inferredSourceType = "CLASS_PERSONAL";
            inferredClassCourseId = linkedCourseIds.get(0);
        }

        var cfgOpt = sessionContentConfigRepository.findBySession_Id(session.getId());
        String respSourceType = null;
        Long respClassCourseId = null;
        Long respChapterId = null;
        Long respLessonId = null;
        if (cfgOpt.isPresent()) {
            var cfg = cfgOpt.get();
            respSourceType = cfg.getSourceType();
            respClassCourseId = cfg.getTeacherCourseId(); // teacherCourseId lưu khóa học clone của lớp
            respChapterId = cfg.getChapterId();
            respLessonId = cfg.getLessonId();
        }

        // If config says CLASS_PERSONAL but classCourseId is missing, resolve from class/teacher
        if ("CLASS_PERSONAL".equalsIgnoreCase(respSourceType) && respClassCourseId == null) {
            try {
                Course cc = ensureClassCourse(session);
                if (cc != null) {
                    respClassCourseId = cc.getId();
                    log.info("🔧 Resolved classCourseId for CLASS_PERSONAL: {}", respClassCourseId);
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to resolve classCourseId for CLASS_PERSONAL: {}", e.getMessage());
            }
        }

        return SessionContentResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .courseTitle(course.getTitle())
                .baseCourseId(baseCourseId)
                .sourceType(respSourceType != null ? respSourceType : inferredSourceType)
                .classCourseId(respClassCourseId != null ? respClassCourseId : inferredClassCourseId)
                .chapterId(respChapterId)
                .lessonId(respLessonId)
                .linkedChapterIds(chapterIds)
                .linkedLessonIds(lessonIds)
                .chapters(chapterResponses)
                .content(session.getLessonContent())
                .build();
    }

    // Tạo khoá học riêng cho lớp nếu chưa tồn tại bằng cách clone từ course môn
    private Course ensureClassCourse(ClassSession session) {
        var clazz = session.getClazz();
        var teacherId = clazz.getTeacher().getId();
        var baseCourse = clazz.getCourse();
        String title = baseCourse.getTitle() + " – " + clazz.getName();
        var existed = courseRepository.findByOwnerTeacher_IdAndTitle(teacherId, title);
        if (existed != null && !existed.isEmpty()) {
            return existed.get(0);
        }
        // tạo mới
        Course newCourse = Course.builder()
                .subject(baseCourse.getSubject())
                .title(title)
                .description(baseCourse.getDescription())
                .status(CourseStatus.APPROVED)
                .createdBy(clazz.getTeacher().getUser())
                .ownerTeacher(clazz.getTeacher())
                .build();
        newCourse = courseRepository.save(newCourse);

        // clone chapters + lessons
        var baseChapters = chapterRepository.findByCourse_IdOrderByOrderIndexAsc(baseCourse.getId());
        int chOrder = 1;
        for (CourseChapter bc : baseChapters) {
            CourseChapter nc = CourseChapter.builder()
                    .course(newCourse)
                    .title(bc.getTitle())
                    .description(bc.getDescription())
                    .orderIndex(chOrder++)
                    .build();
            nc = chapterRepository.save(nc);
            var baseLessons = lessonRepository.findByChapter_IdOrderByOrderIndexAsc(bc.getId());
            int lOrder = 1;
            for (CourseLesson bl : baseLessons) {
                CourseLesson nl = CourseLesson.builder()
                        .chapter(nc)
                        .title(bl.getTitle())
                        .description(bl.getDescription())
                        .orderIndex(lOrder++)
                        .build();
                lessonRepository.save(nl);
            }
        }
        return newCourse;
    }

    @Transactional(readOnly = true)
    public SessionContentResponse getSessionContentByClassDate(Long classId, String dateStr, Long slotId) {
        LocalDate date = LocalDate.parse(dateStr);
        ClassSession session;
        if (slotId != null) {
            session = classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElseThrow(() -> new RuntimeException("No session found for class " + classId + " on date " + dateStr + " with slotId=" + slotId));
        } else {
            List<ClassSession> sameDay = classSessionRepository
                    .findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (sameDay.isEmpty()) {
                throw new RuntimeException("No session found for class " + classId + " on date " + dateStr);
            }
            if (sameDay.size() > 1) {
                log.warn("Multiple sessions found for class {} on {}. Consider passing slotId. Picking earliest.", classId, dateStr);
            }
            session = sameDay.get(0);
        }
        return getSessionContent(session.getId());
    }
}
