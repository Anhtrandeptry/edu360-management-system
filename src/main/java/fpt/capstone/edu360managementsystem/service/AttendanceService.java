package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.List;
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
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
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

    /**
     * Danh sách buổi dạy hôm nay của giáo viên (theo userId)
     */
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

    /**
     * Chi tiết 1 buổi: danh sách HS & trạng thái hiện tại — chỉ buổi thuộc giáo
     * viên
     */
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

    /**
     * Chấm/ cập nhật điểm danh — CHỈ trong đúng ngày
     */
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
        }
    }

    /**
     * Chấm điểm danh theo classId và date
     */
    @Transactional
    public void upsertAttendanceByClassAndDate(Long userId, Long classId, String dateStr, Long slotId, AttendanceUpsertRequest req) {
        LocalDate date = LocalDate.parse(dateStr);

        // Find session by classId, date, and optionally slotId
        ClassSession session;
        if (slotId != null) {
            List<ClassSession> sessions = classSessionRepository
                    .findAllByClazz_IdAndDateAndTimeSlot_IdOrderByIdAsc(classId, date, slotId);
            if (sessions.isEmpty()) {
                throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                        "Không có buổi học nào cho lớp này vào ngày đã chọn với slot này.");
            }
            if (sessions.size() > 1) {
                System.err.println("⚠️ Duplicate sessions detected for class=" + classId + ", date=" + date + ", slotId=" + slotId + ". Using the first by ID.");
            }
            session = sessions.get(0);
        } else {
            // Nếu không có slotId, kiểm tra số lượng phiên trong ngày
            List<ClassSession> sameDaySessions = classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            if (sameDaySessions.isEmpty()) {
                throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                        "Không có buổi học nào cho lớp này vào ngày đã chọn.");
            }
            if (sameDaySessions.size() > 1) {
                // Giáo viên bắt buộc truyền slotId nếu có nhiều phiên trong ngày
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Ngày này có nhiều slot. Vui lòng chỉ định slotId để chọn đúng buổi học.");
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
        }
    }

    /**
     * Lấy chi tiết điểm danh theo classId và date (cho FE load lại)
     */
    public AttendanceSessionDetailResponse getSessionDetailByClassAndDate(Long userId, Long classId, String dateStr, Long slotId) {
        System.out.println("🔍 getSessionDetailByClassAndDate called: userId=" + userId + ", classId=" + classId + ", date=" + dateStr + ", slotId=" + slotId);

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr);
        } catch (Exception e) {
            System.err.println("❌ Error parsing date: " + dateStr);
            throw new RuntimeException("Invalid date format: " + dateStr);
        }

        // Try to find an existing session for the class on the date with slotId if provided
        ClassSession session = null;
        if (slotId != null) {
            System.out.println("🔎 Looking for session with slotId: " + slotId);
            List<ClassSession> sessions = classSessionRepository
                    .findAllByClazz_IdAndDateAndTimeSlot_IdOrderByIdAsc(classId, date, slotId);
            if (!sessions.isEmpty()) {
                if (sessions.size() > 1) {
                    System.err.println("⚠️ Duplicate sessions detected for GET detail (teacher). Using the first by ID.");
                }
                session = sessions.get(0);
            }
            System.out.println("📍 Found session by slotId: " + (session != null ? session.getId() : "null"));
        }

        // Nếu có nhiều phiên trong ngày và thiếu slotId, teacher phải chỉ định slotId
        if (session == null) {
            System.out.println("🔎 Looking for sessions on date without slotId");
            List<ClassSession> sameDay = classSessionRepository
                    .findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(classId, date);
            System.out.println("📍 Found " + sameDay.size() + " sessions on this date");
            if (!sameDay.isEmpty()) {
                if (sameDay.size() > 1) {
                    System.err.println("❌ Multiple sessions found on date without slotId. Teacher must specify slotId.");
                    throw new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Ngày này có nhiều slot. Vui lòng chỉ định slotId để xem đúng buổi học.");
                }
                session = sameDay.get(0);
                System.out.println("📍 Using single session: " + session.getId());
            }
        }

        // If still not found, try to create session from ClassSchedule (but ensure teacher ownership)
        if (session == null) {
            System.out.println("⚠️ No existing session found, attempting to create new session...");

            Clazz clazz = clazzRepository.findById(classId)
                    .orElseThrow(() -> {
                        System.err.println("❌ Class not found: " + classId);
                        return new RuntimeException("Không tìm thấy lớp học");
                    });

            System.out.println("📚 Found class: " + clazz.getName() + ", Teacher: " + clazz.getTeacher().getUser().getFullName());

            // Ownership check: only class teacher can create session here
            if (!clazz.getTeacher().getUser().getId().equals(userId)) {
                System.err.println("❌ Ownership check failed: userId=" + userId + ", teacherUserId=" + clazz.getTeacher().getUser().getId());
                throw new RuntimeException("Not owner of this class");
            }

            System.out.println("✅ Ownership check passed");

            int dayOfWeek = date.getDayOfWeek().getValue();
            System.out.println("📅 Looking for schedule on dayOfWeek: " + dayOfWeek + ", slotId: " + slotId);

            List<ClassSchedule> schedules = classScheduleRepository.findByClazz_Id(classId);
            System.out.println("📋 Found " + schedules.size() + " schedules for class");

            // Filter by slotId if provided, otherwise pick first matching schedule for the day
            List<ClassSchedule> matchingSchedules = schedules.stream()
                    .filter(s -> {
                        boolean dayMatch = s.getDayOfWeek() == dayOfWeek;
                        boolean slotMatch = slotId == null || s.getTimeSlot().getId().equals(slotId);
                        System.out.println("   Schedule check: dayOfWeek=" + s.getDayOfWeek() + " (match:" + dayMatch + "), slotId=" + s.getTimeSlot().getId() + " (match:" + slotMatch + ")");
                        return dayMatch && slotMatch;
                    })
                    .toList();

            System.out.println("✅ Matching schedules: " + matchingSchedules.size());

            if (matchingSchedules.isEmpty()) {
                System.err.println("❌ No matching schedule found for dayOfWeek=" + dayOfWeek + ", slotId=" + slotId);
                throw new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                        "Không có lịch học nào cho lớp này vào ngày đã chọn (thứ " + dayOfWeek + ").");
            }

            // If multiple schedules match and no slotId, pick the first one
            ClassSchedule matchingSchedule = matchingSchedules.get(0);
            System.out.println("✅ Using schedule with timeSlot: " + matchingSchedule.getTimeSlot().getId());

            ClassSession newSession = new ClassSession();
            newSession.setClazz(clazz);
            newSession.setDate(date);
            newSession.setDayOfWeek(dayOfWeek);
            newSession.setTimeSlot(matchingSchedule.getTimeSlot());
            newSession.setRoom(clazz.getRoom());
            session = classSessionRepository.save(newSession);
            System.out.println("✅ Created new session: " + session.getId());
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

    /**
     * Lấy chi tiết điểm danh theo classId và date cho Admin (không check
     * ownership) Nếu không tìm thấy session, sẽ tự động tạo dựa trên
     * ClassSchedule
     */
    @Transactional
    public AttendanceSessionDetailResponse getSessionDetailByClassAndDateForAdmin(Long classId, String dateStr, Long slotId) {
        LocalDate date = LocalDate.parse(dateStr);
        System.out.println("🔍 [ADMIN] getSessionDetailByClassAndDateForAdmin called: classId=" + classId + ", date=" + dateStr + ", slotId=" + slotId);

        // 1) Tìm session theo slot nếu có truyền slotId (tránh lỗi nhiều bản ghi)
        ClassSession session = null;
        if (slotId != null) {
            List<ClassSession> sessions = classSessionRepository
                    .findAllByClazz_IdAndDateAndTimeSlot_IdOrderByIdAsc(classId, date, slotId);
            if (!sessions.isEmpty()) {
                if (sessions.size() > 1) {
                    System.err.println("⚠️ [ADMIN] Duplicate sessions detected for class=" + classId + ", date=" + date + ", slotId=" + slotId + ". Using the first by ID.");
                }
                session = sessions.get(0);
            }
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

        // 3) Nếu vẫn chưa có, tạo mới dựa vào ClassSchedule (lọc theo slot nếu có)
        if (session == null) {
            System.out.println("⚠️ Session not found, creating new session for class " + classId + " on " + date);

            Clazz clazz = clazzRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học"));

            int dayOfWeek = date.getDayOfWeek().getValue(); // 1-7 (Mon-Sun)
            System.out.println("📅 Looking for schedule on dayOfWeek: " + dayOfWeek + ", slotId: " + slotId);

            List<ClassSchedule> schedules = classScheduleRepository.findByClazz_Id(classId);
            System.out.println("📋 Found " + schedules.size() + " schedules for class " + classId);

            ClassSchedule matchingSchedule = schedules.stream()
                    .filter(s -> {
                        System.out.println("   - Schedule dayOfWeek: " + s.getDayOfWeek() + ", timeSlot: " + s.getTimeSlot().getId());
                        boolean dayMatch = s.getDayOfWeek() == dayOfWeek;
                        boolean slotMatch = slotId == null || s.getTimeSlot().getId().equals(slotId);
                        return dayMatch && slotMatch;
                    })
                    .findFirst()
                    .orElseThrow(() -> new fpt.capstone.edu360managementsystem.exception.SessionNotFoundException(
                    "Không có lịch học nào cho lớp này vào ngày đã chọn (thứ " + dayOfWeek + ", slot " + slotId + ")."));

            System.out.println("✅ Creating session with timeSlot: " + matchingSchedule.getTimeSlot().getId());

            ClassSession newSession = new ClassSession();
            newSession.setClazz(clazz);
            newSession.setDate(date);
            newSession.setDayOfWeek(dayOfWeek);
            newSession.setTimeSlot(matchingSchedule.getTimeSlot());
            newSession.setRoom(clazz.getRoom());
            session = classSessionRepository.save(newSession);
        }

        System.out.println("✅ Session found/created: ID = " + session.getId());

        var enrollments = classEnrollmentRepository.findByClazz_Id(classId);
        System.out.println("👥 Found " + enrollments.size() + " enrolled students");

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

        System.out.println("📦 Returning " + students.size() + " students in response");

        if (students.isEmpty()) {
            System.out.println("⚠️ WARNING: No students enrolled in this class!");
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
}
