package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.AttendanceUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionDetailResponse;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionSummaryResponse;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceStudentItem;
import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassScheduleRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;

@Service
public class AttendanceService {

    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private ClassSessionRepository classSessionRepository;
    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ClazzRepository clazzRepository;
    @Autowired
    private ClassScheduleRepository classScheduleRepository;
    @Autowired
    private NotificationService notificationService;

    public List<AttendanceSessionSummaryResponse> getTodaySessionsForTeacher(Long userId) {
        // Map user -> teacher
        Teacher teacher = teacherRepository.findAll()
                .stream().filter(t -> t.getUser().getId().equals(userId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Teacher profile not found"));

        LocalDate today = LocalDate.now();
        var sessions = classSessionRepository.findTodaySessionsForTeacher(teacher.getId(), today);

        return sessions.stream().map(s -> {
            boolean marked = attendanceRepository.findBySession_Id(s.getId()).stream()
                    .anyMatch(a -> a.getStatus() != AttendanceStatus.UNMARKED);
            return AttendanceSessionSummaryResponse.builder()
                    .sessionId(s.getId())
                    .classId(s.getClazz().getId())
                    .className(s.getClazz().getName())
                    .subjectName(s.getClazz().getSubject().getName())
                    .roomName(s.getRoom().getName())
                    .timeStart(s.getTimeSlot().getStartTime().toString())
                    .timeEnd(s.getTimeSlot().getEndTime().toString())
                    .marked(marked)
                    .build();
        }).toList();
    }

    public AttendanceSessionDetailResponse getSessionDetailForTeacher(Long userId, Long sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner session");
        }

        var enrollments = classEnrollmentRepository.findByClazz_Id(session.getClazz().getId());
        var attendanceMap = attendanceRepository.findBySession_Id(sessionId)
                .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        var students = enrollments.stream().map(en -> {
            var att = attendanceMap.get(en.getStudent().getId());
            return AttendanceStudentItem.builder()
                    .studentId(en.getStudent().getId())
                    .studentName(en.getStudent().getUser().getFullName())
                    .status(att != null ? att.getStatus() : AttendanceStatus.UNMARKED)
                    .note(att != null ? att.getNote() : null)
                    .build();
        }).toList();

        return AttendanceSessionDetailResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .roomName(session.getRoom().getName())
                .timeStart(session.getTimeSlot().getStartTime().toString())
                .timeEnd(session.getTimeSlot().getEndTime().toString())
                .students(students)
                .build();
    }

    @Transactional
    public void upsertAttendanceForToday(Long userId, Long sessionId, AttendanceUpsertRequest req) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner session");
        }

        LocalDate today = LocalDate.now();
        if (!session.getDate().isEqual(today)) {
            throw new RuntimeException("Attendance allowed only on the session date");
        }

        var enrollments = classEnrollmentRepository.findByClazz_Id(session.getClazz().getId());
        Set<Long> validStudentIds = enrollments.stream()
                .map(en -> en.getStudent().getId()).collect(Collectors.toSet());

        for (var item : req.getItems()) {
            if (!validStudentIds.contains(item.getStudentId())) {
                throw new RuntimeException("Student not enrolled in class: " + item.getStudentId());
            }
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + item.getStudentId()));

            var existing = attendanceRepository.findBySessionAndStudent(session, student).orElse(null);
            AttendanceStatus oldStatus = existing != null ? existing.getStatus() : null;

            if (existing == null) {
                attendanceRepository.save(Attendance.builder()
                        .session(session)
                        .student(student)
                        .status(item.getStatus())
                        .note(item.getNote())
                        .build());
            } else {
                existing.setStatus(item.getStatus());
                existing.setNote(item.getNote());
                attendanceRepository.save(existing);
            }

            // Gửi notification cho học sinh về trạng thái điểm danh
            sendAttendanceNotification(student, session, item.getStatus(), item.getNote(), oldStatus);
        }
        // Note: Email thông báo cho phụ huynh được gửi thủ công bởi giáo viên qua nút "Gửi thông báo"
    }

    @Transactional
    public void upsertAttendanceByClassAndDate(Long userId, Long classId, String dateStr, Long slotId, AttendanceUpsertRequest req) {
        LocalDate date = LocalDate.parse(dateStr);

        // Find session by classId, date, and optionally slotId
        ClassSession session;
        if (slotId != null) {
            session = classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElseThrow(() -> new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                    "Không có buổi học nào cho lớp này vào ngày đã chọn với slot này."));
        } else {
            // Fallback to first session of the day if no slotId provided
            List<ClassSession> sameDaySessions = classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (sameDaySessions.isEmpty()) {
                throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                        "Không có buổi học nào cho lớp này vào ngày đã chọn.");
            }
            session = sameDaySessions.get(0);
        }

        // Verify teacher ownership
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner of this class");
        }

        var enrollments = classEnrollmentRepository.findByClazz_Id(classId);
        Set<Long> validStudentIds = enrollments.stream()
                .map(en -> en.getStudent().getId()).collect(Collectors.toSet());

        for (var item : req.getItems()) {
            if (!validStudentIds.contains(item.getStudentId())) {
                throw new RuntimeException("Student not enrolled in class: " + item.getStudentId());
            }
            Student student = studentRepository.findById(item.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: " + item.getStudentId()));

            var existing = attendanceRepository.findBySessionAndStudent(session, student).orElse(null);
            AttendanceStatus oldStatus = existing != null ? existing.getStatus() : null;

            if (existing == null) {
                attendanceRepository.save(Attendance.builder()
                        .session(session)
                        .student(student)
                        .status(item.getStatus())
                        .note(item.getNote())
                        .build());
            } else {
                existing.setStatus(item.getStatus());
                existing.setNote(item.getNote());
                attendanceRepository.save(existing);
            }

            // Gửi notification cho học sinh về trạng thái điểm danh
            sendAttendanceNotification(student, session, item.getStatus(), item.getNote(), oldStatus);
        }
    }

    private void sendAttendanceNotification(Student student, ClassSession session,
            AttendanceStatus status, String note, AttendanceStatus oldStatus) {
        // Chỉ gửi notification nếu trạng thái thay đổi hoặc là lần đầu điểm danh
        if (oldStatus != null && oldStatus == status && (note == null || note.isEmpty())) {
            return; // Không có thay đổi
        }

        String className = session.getClazz().getName();
        String slotInfo = "Tiết " + session.getTimeSlot().getId()
                + " (" + session.getTimeSlot().getStartTime() + " - " + session.getTimeSlot().getEndTime() + ")";
        String dateInfo = session.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String title;
        String message;
        // Sử dụng CLASS_REMINDER cho tất cả attendance notification 
        // vì database column type có thể chưa được mở rộng cho các giá trị mới
        NotificationType notificationType = NotificationType.CLASS_REMINDER;

        switch (status) {
            case PRESENT:
                title = "Điểm danh: Có mặt";
                message = String.format("Bạn được điểm danh CÓ MẶT tại lớp %s, %s ngày %s.",
                        className, slotInfo, dateInfo);
                break;
            case ABSENT:
                title = "Điểm danh: Vắng mặt";
                message = String.format("Bạn được điểm danh VẮNG MẶT tại lớp %s, %s ngày %s.",
                        className, slotInfo, dateInfo);
                break;
            case LATE:
                title = "Điểm danh: Đi muộn";
                message = String.format("Bạn được điểm danh ĐI MUỘN tại lớp %s, %s ngày %s.",
                        className, slotInfo, dateInfo);
                break;
            default:
                return; // UNMARKED - không gửi notification
        }

        // Thêm ghi chú nếu có
        if (note != null && !note.trim().isEmpty()) {
            message += "\n Ghi chú từ giáo viên: " + note.trim();
        }

        // Gửi notification
        try {
            Long userId = student.getUser().getId();
            Long classId = session.getClazz().getId();
            String link = "/home/my-classes/" + classId; // Link đến chi tiết lớp học
            notificationService.createNotification(userId, title, message, notificationType, link);
        } catch (Exception e) {
            // Log error but don't fail the attendance operation
            System.err.println("Failed to send attendance notification: " + e.getMessage());
        }
        // Note: Email thông báo cho phụ huynh được gửi thủ công bởi giáo viên qua nút "Gửi thông báo"
    }

    /**
     * Lấy chi tiết điểm danh theo classId và date (cho FE load lại)
     */
    public AttendanceSessionDetailResponse getSessionDetailByClassAndDate(Long userId, Long classId, String dateStr, Long slotId) {
        System.out.println("getSessionDetailByClassAndDate called: userId=" + userId + ", classId=" + classId + ", date=" + dateStr + ", slotId=" + slotId);

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.err.println("Error parsing date: " + dateStr);
            throw new RuntimeException("Invalid date format: " + dateStr);
        }

        // Try to find an existing session for the class on the date with slotId if provided
        ClassSession session = null;
        if (slotId != null) {
            System.out.println("Looking for session with slotId: " + slotId);
            session = classSessionRepository
                    .findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElse(null);
            System.out.println("Found session by slotId: " + (session != null ? session.getId() : "null"));
        }

        // If multiple sessions in a day and no slotId, pick the first one (time slot order)
        if (session == null) {
            System.out.println("Looking for sessions on date without slotId");
            List<ClassSession> sameDay = classSessionRepository
                    .findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            System.out.println("Found " + sameDay.size() + " sessions on this date");
            if (!sameDay.isEmpty()) {
                session = sameDay.get(0);
                System.out.println("Using first session: " + session.getId());
            }
        }

        // If still not found, throw error (DO NOT auto-create sessions to avoid unexpected session count increase)
        if (session == null) {
            System.out.println("No existing session found for classId=" + classId + ", date=" + date + ", slotId=" + slotId);
            throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                    "Không tìm thấy buổi học cho ngày " + date + ". Buổi học này chưa được tạo trong hệ thống.");
        }

        // Final ownership verification
        if (!session.getClazz().getTeacher().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not owner of this class");
        }

        var enrollments = classEnrollmentRepository.findByClazz_Id(classId);
        var attendanceMap = attendanceRepository.findBySession_Id(session.getId())
                .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        var students = enrollments.stream().map(en -> {
            var att = attendanceMap.get(en.getStudent().getId());
            return AttendanceStudentItem.builder()
                    .studentId(en.getStudent().getId())
                    .studentName(en.getStudent().getUser().getFullName())
                    .status(att != null ? att.getStatus() : AttendanceStatus.UNMARKED)
                    .note(att != null ? att.getNote() : null)
                    .build();
        }).toList();

        return AttendanceSessionDetailResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .roomName(session.getRoom() != null ? session.getRoom().getName() : "N/A")
                .timeStart(session.getTimeSlot().getStartTime().toString())
                .timeEnd(session.getTimeSlot().getEndTime().toString())
                .students(students)
                .build();
    }

    @Transactional
    public AttendanceSessionDetailResponse getSessionDetailByClassAndDateForAdmin(Long classId, String dateStr, Long slotId) {
        LocalDate date = LocalDate.parse(dateStr);
        System.out.println("[ADMIN] getSessionDetailByClassAndDateForAdmin called: classId=" + classId + ", date=" + dateStr + ", slotId=" + slotId);

        // 1) Tìm session theo slot nếu có truyền slotId (tránh lỗi nhiều bản ghi)
        ClassSession session = null;
        if (slotId != null) {
            session = classSessionRepository
                    .findByClazz_IdAndDateAndTimeSlot_Id(classId, date, slotId)
                    .orElse(null);
        }

        // 2) Nếu chưa có, thử tìm các session cùng ngày cho lớp này
        if (session == null) {
            List<ClassSession> sameDay = classSessionRepository
                    .findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (!sameDay.isEmpty()) {
                // Nếu không có slotId, chọn session đầu tiên để tránh fail; ưu tiên an toàn
                session = sameDay.stream()
                        .filter(s -> slotId == null || s.getTimeSlot().getId().equals(slotId))
                        .findFirst()
                        .orElse(sameDay.get(0));
            }
        }

        // 3) Nếu vẫn chưa có, throw error (KHÔNG tự động tạo session để tránh tăng số buổi không mong muốn)
        if (session == null) {
            System.out.println("Session not found for classId=" + classId + ", date=" + date + ", slotId=" + slotId);
            throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                    "Không tìm thấy buổi học cho ngày " + date + ". Buổi học này chưa được tạo trong hệ thống.");
        }

        System.out.println("Session found/created: ID = " + session.getId());

        var enrollments = classEnrollmentRepository.findByClazz_Id(classId);
        System.out.println("Found " + enrollments.size() + " enrolled students");

        var attendanceMap = attendanceRepository.findBySession_Id(session.getId())
                .stream().collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a));

        var students = enrollments.stream().map(en -> {
            var att = attendanceMap.get(en.getStudent().getId());
            return AttendanceStudentItem.builder()
                    .studentId(en.getStudent().getId())
                    .studentName(en.getStudent().getUser().getFullName())
                    .status(att != null ? att.getStatus() : AttendanceStatus.UNMARKED)
                    .note(att != null ? att.getNote() : null)
                    .build();
        }).toList();

        System.out.println("Returning " + students.size() + " students in response");

        if (students.isEmpty()) {
            System.out.println("WARNING: No students enrolled in this class!");
        }

        return AttendanceSessionDetailResponse.builder()
                .sessionId(session.getId())
                .classId(session.getClazz().getId())
                .className(session.getClazz().getName())
                .subjectName(session.getClazz().getSubject().getName())
                .roomName(session.getRoom() != null ? session.getRoom().getName() : "N/A")
                .timeStart(session.getTimeSlot().getStartTime().toString())
                .timeEnd(session.getTimeSlot().getEndTime().toString())
                .students(students)
                .build();
    }

    /**
     * Check attendance status for multiple sessions. Each session identifier is
     * in format "classId-date-slotId". Returns a map of session identifier to
     * boolean (true if has attendance).
     *
     * @param sessionIdentifiers list of session identifiers
     * @return map of session identifier to attendance status
     */
    public Map<String, Boolean> checkAttendanceStatus(List<String> sessionIdentifiers) {
        Map<String, Boolean> result = new HashMap<>();

        for (String identifier : sessionIdentifiers) {
            try {
                String[] parts = identifier.split("-");
                if (parts.length < 4) {
                    result.put(identifier, false);
                    continue;
                }

                // Parse identifier: classId-yyyy-MM-dd-slotId
                Long classId = Long.parseLong(parts[0]);
                String dateStr = parts[1] + "-" + parts[2] + "-" + parts[3];
                LocalDate date = LocalDate.parse(dateStr);
                Long slotId = parts.length > 4 ? Long.parseLong(parts[4]) : null;

                // Find session by class, date, and slot
                List<ClassSession> sessions = classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);

                boolean hasAttendance = false;
                for (ClassSession session : sessions) {
                    // If slotId is provided, filter by it
                    if (slotId != null && !session.getTimeSlot().getId().equals(slotId)) {
                        continue;
                    }
                    // Check if session has any attendance records (not UNMARKED)
                    List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
                    hasAttendance = attendances.stream()
                            .anyMatch(a -> a.getStatus() != AttendanceStatus.UNMARKED);
                    if (hasAttendance) {
                        break;
                    }
                }

                result.put(identifier, hasAttendance);
            } catch (Exception e) {
                result.put(identifier, false);
            }
        }

        return result;
    }
}
