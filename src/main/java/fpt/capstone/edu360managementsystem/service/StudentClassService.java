package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleResponse;
import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.SessionChapter;
import fpt.capstone.edu360managementsystem.entity.SessionLesson;
import fpt.capstone.edu360managementsystem.entity.SessionMaterial;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.SessionChapterRepository;
import fpt.capstone.edu360managementsystem.repository.SessionLessonRepository;
import fpt.capstone.edu360managementsystem.repository.SessionMaterialRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;

@Service
public class StudentClassService {

    private static final Logger log = LoggerFactory.getLogger(StudentClassService.class);

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SessionChapterRepository sessionChapterRepository;

    @Autowired
    private SessionLessonRepository sessionLessonRepository;

    @Autowired
    private SessionMaterialRepository sessionMaterialRepository;

    public List<StudentClassResponse> getMyClasses(Long userId) {
        try {
            log.info("[StudentClassService] getMyClasses called for userId={}", userId);

            // map user -> student
            Student student = studentRepository.findByUser_Id(userId)
                    .orElse(null);

            if (student == null) {
                log.warn("[StudentClassService] No student profile found for userId={}", userId);
                return java.util.Collections.emptyList();
            }

            log.info("[StudentClassService] ✓ Found student profile: studentId={}, userId={}", student.getId(), userId);

            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());
            log.info("[StudentClassService] Found {} enrollments for studentId={}", enrollments.size(), student.getId());

            return enrollments.stream()
                    .map(en -> {
                        var clazz = en.getClazz();

                        // Lấy course của lớp trực tiếp (mỗi lớp có 1 course riêng)
                        Long courseId = clazz.getCourse() != null ? clazz.getCourse().getId() : null;
                        String courseTitle = clazz.getCourse() != null ? clazz.getCourse().getTitle() : null;

                        org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                                .info("[StudentClassService] Class {} - courseId={}, courseTitle={}",
                                        clazz.getId(), courseId, courseTitle);

                        return StudentClassResponse.builder()
                                .classId(clazz.getId())
                                .className(clazz.getName())
                                .subjectName(clazz.getSubject() != null ? clazz.getSubject().getName() : null)
                                .teacherName(clazz.getTeacher() != null && clazz.getTeacher().getUser() != null
                                        ? clazz.getTeacher().getUser().getFullName() : null)
                                .teacherAvatarUrl(clazz.getTeacher() != null ? clazz.getTeacher().getAvatarUrl() : null)
                                .roomName(clazz.getRoom() != null ? clazz.getRoom().getName() : null)
                                .semesterName(clazz.getSemester() != null ? clazz.getSemester().getName() : null)
                                .startDate(clazz.getStartDate())
                                .endDate(clazz.getEndDate())
                                .status(clazz.getStatus())
                                .courseId(courseId)
                                .courseTitle(courseTitle)
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                    .error("[StudentClassService] getMyClasses failed for userId={}: {}", userId, e.getMessage(), e);
            // Return empty list to avoid propagating 500; frontend can show 'no classes'
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Lấy lịch học cho student theo tuần
     */
    public List<StudentScheduleResponse> getMyScheduleByWeek(Long userId, LocalDate weekStart) {
        try {
            // Tìm student profile
            Student student = studentRepository.findByUser_Id(userId)
                    .orElse(null);

            if (student == null) {
                log.warn("[StudentClassService] No student profile found for userId={}", userId);
                return java.util.Collections.emptyList();
            }

            // Lấy các lớp đã đăng ký
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());

            if (enrollments.isEmpty()) {
                log.info("[StudentClassService] No enrolled classes found for student={}", student.getId());
                return java.util.Collections.emptyList();
            }

            // Lấy danh sách classId
            var classIds = enrollments.stream()
                    .map(en -> en.getClazz().getId())
                    .distinct()
                    .toList();

            // Tính weekEnd
            LocalDate weekEnd = weekStart.plusDays(6);

            // Lấy các session trong tuần
            List<ClassSession> sessions = classSessionRepository
                    .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                            classIds, weekStart, weekEnd
                    );

            log.info("[StudentClassService] Found {} sessions for student={} in week {}-{}",
                    sessions.size(), student.getId(), weekStart, weekEnd);

            // Batch load attendance records for these sessions for the student
            var sessionIds = sessions.stream().map(ClassSession::getId).toList();
            List<Attendance> attendanceRecords = attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, student.getId());
            var statusBySession = attendanceRecords.stream()
                    .collect(java.util.stream.Collectors.toMap(a -> a.getSession().getId(), Attendance::getStatus));

            // Batch load session chapters and lessons
            List<SessionChapter> allSessionChapters = sessionChapterRepository.findBySession_IdIn(sessionIds);
            List<SessionLesson> allSessionLessons = sessionLessonRepository.findBySession_IdIn(sessionIds);
            List<SessionMaterial> allMaterials = sessionMaterialRepository.findBySession_IdIn(sessionIds);

            var chaptersBySession = allSessionChapters.stream()
                    .collect(java.util.stream.Collectors.groupingBy(sc -> sc.getSession().getId()));
            var lessonsBySession = allSessionLessons.stream()
                    .collect(java.util.stream.Collectors.groupingBy(sl -> sl.getSession().getId()));
            var materialsBySession = allMaterials.stream()
                    .collect(java.util.stream.Collectors.groupingBy(m -> m.getSession().getId()));

            // Map sang response (kèm trạng thái điểm danh và nội dung bài học)
            return sessions.stream()
                    .map(session -> {
                        AttendanceStatus st = statusBySession.getOrDefault(session.getId(), AttendanceStatus.UNMARKED);

                        // Lấy thông tin course của lớp
                        Long courseId = session.getClazz().getCourse() != null
                                ? session.getClazz().getCourse().getId() : null;
                        String courseTitle = session.getClazz().getCourse() != null
                                ? session.getClazz().getCourse().getTitle() : null;

                        // Lấy chapters, lessons và materials của session này
                        List<SessionChapter> sessionChapters = chaptersBySession.getOrDefault(session.getId(), List.of());
                        List<SessionLesson> sessionLessons = lessonsBySession.getOrDefault(session.getId(), List.of());
                        List<SessionMaterial> sessionMaterials = materialsBySession.getOrDefault(session.getId(), List.of());

                        List<StudentScheduleResponse.SessionChapterInfo> linkedChapters = sessionChapters.stream()
                                .map(sc -> StudentScheduleResponse.SessionChapterInfo.builder()
                                .id(sc.getChapter().getId())
                                .title(sc.getChapter().getTitle())
                                .description(sc.getChapter().getDescription())
                                .orderIndex(sc.getChapter().getOrderIndex())
                                .build())
                                .toList();

                        List<StudentScheduleResponse.SessionLessonInfo> linkedLessons = sessionLessons.stream()
                                .map(sl -> StudentScheduleResponse.SessionLessonInfo.builder()
                                .id(sl.getLesson().getId())
                                .chapterId(sl.getLesson().getChapter().getId())
                                .chapterTitle(sl.getLesson().getChapter().getTitle())
                                .title(sl.getLesson().getTitle())
                                .description(sl.getLesson().getDescription())
                                .orderIndex(sl.getLesson().getOrderIndex())
                                .build())
                                .toList();

                        List<StudentScheduleResponse.SessionMaterialInfo> materials = sessionMaterials.stream()
                                .map(m -> StudentScheduleResponse.SessionMaterialInfo.builder()
                                .id(m.getId())
                                .fileName(m.getFileName())
                                .fileUrl(m.getFileUrl())
                                .fileType(m.getFileType())
                                .fileSize(m.getFileSize())
                                .description(m.getDescription())
                                .uploadedAt(m.getUploadedAt() != null ? m.getUploadedAt().toString() : null)
                                .uploadedByName(m.getUploadedBy() != null ? m.getUploadedBy().getFullName() : null)
                                .build())
                                .toList();

                        return StudentScheduleResponse.builder()
                                .sessionId(session.getId())
                                .classId(session.getClazz().getId())
                                .className(session.getClazz().getName())
                                .subjectName(session.getClazz().getSubject() != null
                                        ? session.getClazz().getSubject().getName() : null)
                                .teacherName(session.getClazz().getTeacher() != null
                                        && session.getClazz().getTeacher().getUser() != null
                                        ? session.getClazz().getTeacher().getUser().getFullName() : null)
                                .roomName(session.getRoom() != null ? session.getRoom().getName() : null)
                                .date(session.getDate())
                                .timeStart(session.getTimeSlot().getStartTime().toString())
                                .timeEnd(session.getTimeSlot().getEndTime().toString())
                                .dayOfWeek(session.getDate().getDayOfWeek().getValue())
                                .attendanceStatus(st.name())
                                .lessonContent(session.getLessonContent())
                                .meetingLink(session.getClazz().getMeetingLink())
                                .isOnline(session.getClazz().getMeetingLink() != null && !session.getClazz().getMeetingLink().isEmpty())
                                .linkedChapters(linkedChapters)
                                .linkedLessons(linkedLessons)
                                .materials(materials)
                                .courseId(courseId)
                                .courseTitle(courseTitle)
                                .build();
                    })
                    .toList();

        } catch (Exception e) {
            log.error("[StudentClassService] getMyScheduleByWeek failed for userId={}: {}",
                    userId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Lấy tất cả các buổi học của một lớp kèm nội dung bài học Kiểm tra student
     * đã đăng ký lớp này chưa
     */
    public List<StudentScheduleResponse> getClassSessions(Long userId, Long classId) {
        try {
            // Tìm student profile
            Student student = studentRepository.findByUser_Id(userId).orElse(null);
            if (student == null) {
                log.warn("[StudentClassService] No student profile found for userId={}", userId);
                return java.util.Collections.emptyList();
            }

            // Kiểm tra student có đăng ký lớp này không
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());
            boolean isEnrolled = enrollments.stream()
                    .anyMatch(en -> en.getClazz().getId().equals(classId));

            if (!isEnrolled) {
                log.warn("[StudentClassService] Student {} not enrolled in class {}", student.getId(), classId);
                return java.util.Collections.emptyList();
            }

            // Lấy tất cả sessions của lớp, sắp xếp theo ngày và slot
            List<ClassSession> sessions = classSessionRepository
                    .findByClazz_IdOrderByDateAscTimeSlot_StartTimeAsc(classId);

            log.info("[StudentClassService] Found {} sessions for class {}", sessions.size(), classId);

            // Batch load attendance records
            var sessionIds = sessions.stream().map(ClassSession::getId).toList();
            List<Attendance> attendanceRecords = attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, student.getId());
            var statusBySession = attendanceRecords.stream()
                    .collect(java.util.stream.Collectors.toMap(a -> a.getSession().getId(), Attendance::getStatus));

            // Batch load session chapters and lessons
            List<SessionChapter> allSessionChapters = sessionChapterRepository.findBySession_IdIn(sessionIds);
            List<SessionLesson> allSessionLessons = sessionLessonRepository.findBySession_IdIn(sessionIds);
            List<SessionMaterial> allMaterials = sessionMaterialRepository.findBySession_IdIn(sessionIds);

            var chaptersBySession = allSessionChapters.stream()
                    .collect(java.util.stream.Collectors.groupingBy(sc -> sc.getSession().getId()));
            var lessonsBySession = allSessionLessons.stream()
                    .collect(java.util.stream.Collectors.groupingBy(sl -> sl.getSession().getId()));
            var materialsBySession = allMaterials.stream()
                    .collect(java.util.stream.Collectors.groupingBy(m -> m.getSession().getId()));

            // Map sang response
            return sessions.stream()
                    .map(session -> {
                        AttendanceStatus st = statusBySession.getOrDefault(session.getId(), AttendanceStatus.UNMARKED);

                        Long courseId = session.getClazz().getCourse() != null
                                ? session.getClazz().getCourse().getId() : null;
                        String courseTitle = session.getClazz().getCourse() != null
                                ? session.getClazz().getCourse().getTitle() : null;

                        List<SessionChapter> sessionChapters = chaptersBySession.getOrDefault(session.getId(), List.of());
                        List<SessionLesson> sessionLessons = lessonsBySession.getOrDefault(session.getId(), List.of());
                        List<SessionMaterial> sessionMaterials = materialsBySession.getOrDefault(session.getId(), List.of());

                        List<StudentScheduleResponse.SessionChapterInfo> linkedChapters = sessionChapters.stream()
                                .map(sc -> StudentScheduleResponse.SessionChapterInfo.builder()
                                .id(sc.getChapter().getId())
                                .title(sc.getChapter().getTitle())
                                .description(sc.getChapter().getDescription())
                                .orderIndex(sc.getChapter().getOrderIndex())
                                .build())
                                .toList();

                        List<StudentScheduleResponse.SessionLessonInfo> linkedLessons = sessionLessons.stream()
                                .map(sl -> StudentScheduleResponse.SessionLessonInfo.builder()
                                .id(sl.getLesson().getId())
                                .chapterId(sl.getLesson().getChapter().getId())
                                .chapterTitle(sl.getLesson().getChapter().getTitle())
                                .title(sl.getLesson().getTitle())
                                .description(sl.getLesson().getDescription())
                                .orderIndex(sl.getLesson().getOrderIndex())
                                .build())
                                .toList();

                        List<StudentScheduleResponse.SessionMaterialInfo> materials = sessionMaterials.stream()
                                .map(m -> StudentScheduleResponse.SessionMaterialInfo.builder()
                                .id(m.getId())
                                .fileName(m.getFileName())
                                .fileUrl(m.getFileUrl())
                                .fileType(m.getFileType())
                                .fileSize(m.getFileSize())
                                .description(m.getDescription())
                                .uploadedAt(m.getUploadedAt() != null ? m.getUploadedAt().toString() : null)
                                .uploadedByName(m.getUploadedBy() != null ? m.getUploadedBy().getFullName() : null)
                                .build())
                                .toList();

                        return StudentScheduleResponse.builder()
                                .sessionId(session.getId())
                                .classId(session.getClazz().getId())
                                .className(session.getClazz().getName())
                                .subjectName(session.getClazz().getSubject() != null
                                        ? session.getClazz().getSubject().getName() : null)
                                .teacherName(session.getClazz().getTeacher() != null
                                        && session.getClazz().getTeacher().getUser() != null
                                        ? session.getClazz().getTeacher().getUser().getFullName() : null)
                                .roomName(session.getRoom() != null ? session.getRoom().getName() : null)
                                .date(session.getDate())
                                .timeStart(session.getTimeSlot().getStartTime().toString())
                                .timeEnd(session.getTimeSlot().getEndTime().toString())
                                .dayOfWeek(session.getDate().getDayOfWeek().getValue())
                                .attendanceStatus(st.name())
                                .lessonContent(session.getLessonContent())
                                .meetingLink(session.getClazz().getMeetingLink())
                                .isOnline(session.getClazz().getMeetingLink() != null && !session.getClazz().getMeetingLink().isEmpty())
                                .linkedChapters(linkedChapters)
                                .linkedLessons(linkedLessons)
                                .materials(materials)
                                .courseId(courseId)
                                .courseTitle(courseTitle)
                                .build();
                    })
                    .toList();

        } catch (Exception e) {
            log.error("[StudentClassService] getClassSessions failed for classId={}: {}",
                    classId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}
