package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.TeacherClassAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherListForAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherWorkSummaryResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.SessionStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAttendanceService {

    private final TeacherRepository teacherRepository;
    private final ClazzRepository clazzRepository;
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;

    /**
     * Lấy danh sách tất cả giáo viên với thống kê chấm công tháng hiện tại
     */
    public List<TeacherListForAttendanceResponse> getAllTeachersForAttendance() {
        List<Teacher> teachers = teacherRepository.findAll();
        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(now);
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        return teachers.stream().map(teacher -> {
            User user = teacher.getUser();

            // Lấy danh sách môn học
            List<String> subjectNames = new ArrayList<>();
            if (teacher.getSubject() != null) {
                subjectNames.add(teacher.getSubject().getName());
            }
            if (teacher.getSubjects() != null) {
                teacher.getSubjects().forEach(s -> {
                    if (!subjectNames.contains(s.getName())) {
                        subjectNames.add(s.getName());
                    }
                });
            }

            // Lấy lớp được phân công (chỉ lấy lớp PUBLIC - đang hoạt động)
            List<Clazz> assignedClasses = clazzRepository.findByTeacher_Id(teacher.getId())
                    .stream()
                    .filter(c -> c.getStatus() == ClassStatus.PUBLIC)
                    .toList();
            int assignedClassCount = assignedClasses.size();

            // Thống kê slots trong tháng
            int totalSlots = 0;
            int completedSlots = 0;

            for (Clazz clazz : assignedClasses) {
                List<ClassSession> sessions = classSessionRepository
                        .findByClazz_IdAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                                clazz.getId(), monthStart, monthEnd);

                totalSlots += sessions.size();

                for (ClassSession session : sessions) {
                    if (isSessionCompleted(session)) {
                        completedSlots++;
                    }
                }
            }

            double attendanceRate = totalSlots > 0 ? (completedSlots * 100.0 / totalSlots) : 0;

            return TeacherListForAttendanceResponse.builder()
                    .teacherId(teacher.getId())
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .phone(user.getPhoneNumber())
                    .subjectNames(subjectNames)
                    .degree(teacher.getDegree())
                    .specialization(teacher.getSpecialization())
                    .assignedClasses(assignedClassCount)
                    .completedSlotsThisMonth(completedSlots)
                    .totalSlotsThisMonth(totalSlots)
                    .attendanceRateThisMonth(Math.round(attendanceRate * 10) / 10.0)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Lấy danh sách giáo viên với thống kê chấm công - có phân trang và tìm
     * kiếm
     */
    public Page<TeacherListForAttendanceResponse> getAllTeachersForAttendancePaginated(String search, Pageable pageable) {
        // Lấy tất cả teachers và xử lý
        List<TeacherListForAttendanceResponse> allTeachers = getAllTeachersForAttendance();

        // Filter theo search
        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.toLowerCase().trim();
            allTeachers = allTeachers.stream()
                    .filter(t
                            -> (t.getFullName() != null && t.getFullName().toLowerCase().contains(keyword))
                    || (t.getEmail() != null && t.getEmail().toLowerCase().contains(keyword))
                    || (t.getPhone() != null && t.getPhone().contains(keyword))
                    || (t.getSubjectNames() != null && t.getSubjectNames().stream()
                    .anyMatch(s -> s.toLowerCase().contains(keyword)))
                    )
                    .collect(Collectors.toList());
        }

        // Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allTeachers.size());

        List<TeacherListForAttendanceResponse> pageContent
                = start > allTeachers.size() ? new ArrayList<>() : allTeachers.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allTeachers.size());
    }

    /**
     * Lấy thống kê chi tiết chấm công của một giáo viên
     */
    public TeacherWorkSummaryResponse getTeacherWorkSummary(Long teacherId, Integer month, Integer year) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        User user = teacher.getUser();

        // Xác định tháng/năm
        LocalDate now = LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();
        YearMonth targetYearMonth = YearMonth.of(targetYear, targetMonth);
        LocalDate monthStart = targetYearMonth.atDay(1);
        LocalDate monthEnd = targetYearMonth.atEndOfMonth();

        // Lấy danh sách môn học
        List<String> subjectNames = new ArrayList<>();
        if (teacher.getSubject() != null) {
            subjectNames.add(teacher.getSubject().getName());
        }
        if (teacher.getSubjects() != null) {
            teacher.getSubjects().forEach(s -> {
                if (!subjectNames.contains(s.getName())) {
                    subjectNames.add(s.getName());
                }
            });
        }

        // Lấy lớp được phân công (chỉ lấy lớp PUBLIC - đang hoạt động)
        List<Clazz> assignedClasses = clazzRepository.findByTeacher_Id(teacher.getId())
                .stream()
                .filter(c -> c.getStatus() == ClassStatus.PUBLIC)
                .toList();

        int totalScheduledSlots = 0;
        int totalCompletedSlots = 0;
        int totalPendingSlots = 0;

        List<TeacherWorkSummaryResponse.ClassWorkDetail> classDetails = new ArrayList<>();

        for (Clazz clazz : assignedClasses) {
            List<ClassSession> sessions = classSessionRepository
                    .findByClazz_IdAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                            clazz.getId(), monthStart, monthEnd);

            int classTotal = sessions.size();
            int classCompleted = 0;
            int classPending = 0;

            for (ClassSession session : sessions) {
                if (isSessionCompleted(session)) {
                    classCompleted++;
                } else if (session.getDate().isBefore(now) || session.getDate().isEqual(now)) {
                    classPending++;
                }
            }

            totalScheduledSlots += classTotal;
            totalCompletedSlots += classCompleted;
            totalPendingSlots += classPending;

            classDetails.add(TeacherWorkSummaryResponse.ClassWorkDetail.builder()
                    .classId(clazz.getId())
                    .className(clazz.getName())
                    .subjectName(clazz.getSubject() != null ? clazz.getSubject().getName() : "N/A")
                    .semesterName(clazz.getSemester() != null ? clazz.getSemester().getName() : "N/A")
                    .totalSlots(classTotal)
                    .completedSlots(classCompleted)
                    .pendingSlots(classPending)
                    .status(clazz.getStatus() != null ? clazz.getStatus().name() : "UNKNOWN")
                    .build());
        }

        double attendanceRate = totalScheduledSlots > 0
                ? (totalCompletedSlots * 100.0 / totalScheduledSlots) : 0;

        return TeacherWorkSummaryResponse.builder()
                .teacherId(teacher.getId())
                .teacherName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .subjectNames(subjectNames)
                .totalAssignedClasses(assignedClasses.size())
                .totalScheduledSlots(totalScheduledSlots)
                .totalCompletedSlots(totalCompletedSlots)
                .totalPendingSlots(totalPendingSlots)
                .attendanceRate(Math.round(attendanceRate * 10) / 10.0)
                .classDetails(classDetails)
                .build();
    }

    /**
     * Lấy chi tiết chấm công theo lớp của giáo viên
     */
    public TeacherClassAttendanceResponse getTeacherClassAttendance(Long teacherId, Long classId) {
        // Verify teacher exists
        if (!teacherRepository.existsById(teacherId)) {
            throw new RuntimeException("Teacher not found");
        }

        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Verify teacher is assigned to this class
        if (!clazz.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Teacher is not assigned to this class");
        }

        List<ClassSession> sessions = classSessionRepository
                .findByClazz_IdOrderByDateAscTimeSlot_StartTimeAsc(classId);

        int totalSlots = sessions.size();
        int completedSlots = 0;
        int pendingSlots = 0;
        LocalDate today = LocalDate.now();

        List<TeacherClassAttendanceResponse.SessionAttendanceDetail> sessionDetails = new ArrayList<>();

        for (ClassSession session : sessions) {
            boolean isCompleted = isSessionCompleted(session);
            if (isCompleted) {
                completedSlots++;
            } else if (session.getDate().isBefore(today) || session.getDate().isEqual(today)) {
                pendingSlots++;
            }

            // Lấy thống kê điểm danh của session
            List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
            int present = 0, absent = 0, late = 0;
            for (Attendance att : attendances) {
                switch (att.getStatus()) {
                    case PRESENT ->
                        present++;
                    case ABSENT ->
                        absent++;
                    case LATE ->
                        late++;
                    default -> {
                    }
                }
            }

            String timeSlotStr = "";
            if (session.getTimeSlot() != null) {
                timeSlotStr = session.getTimeSlot().getStartTime() + " - " + session.getTimeSlot().getEndTime();
            }

            sessionDetails.add(TeacherClassAttendanceResponse.SessionAttendanceDetail.builder()
                    .sessionId(session.getId())
                    .date(session.getDate())
                    .dayOfWeek(session.getDayOfWeek())
                    .timeSlot(timeSlotStr)
                    .roomName(session.getRoom() != null ? session.getRoom().getName() : "Online")
                    .sessionStatus(session.getStatus() != null ? session.getStatus().name() : "PLANNED")
                    .isAttendanceSubmitted(isCompleted)
                    .totalStudents(attendances.size())
                    .presentCount(present)
                    .absentCount(absent)
                    .lateCount(late)
                    .lessonContent(session.getLessonContent())
                    .build());
        }

        double completionRate = totalSlots > 0 ? (completedSlots * 100.0 / totalSlots) : 0;

        return TeacherClassAttendanceResponse.builder()
                .classId(clazz.getId())
                .className(clazz.getName())
                .subjectName(clazz.getSubject() != null ? clazz.getSubject().getName() : "N/A")
                .semesterName(clazz.getSemester() != null ? clazz.getSemester().getName() : "N/A")
                .classStatus(clazz.getStatus() != null ? clazz.getStatus().name() : "UNKNOWN")
                .totalSlots(totalSlots)
                .completedSlots(completedSlots)
                .pendingSlots(pendingSlots)
                .completionRate(Math.round(completionRate * 10) / 10.0)
                .sessions(sessionDetails)
                .build();
    }

    /**
     * Kiểm tra session đã được điểm danh hoàn tất chưa Điều kiện: có ít nhất 1
     * attendance record không phải UNMARKED
     */
    private boolean isSessionCompleted(ClassSession session) {
        if (session.getStatus() == SessionStatus.DONE) {
            return true;
        }

        List<Attendance> attendances = attendanceRepository.findBySession_Id(session.getId());
        if (attendances.isEmpty()) {
            return false;
        }

        // Nếu có ít nhất 1 học sinh được điểm danh (không phải UNMARKED) thì coi như đã điểm danh
        return attendances.stream()
                .anyMatch(a -> a.getStatus() != AttendanceStatus.UNMARKED);
    }
}
