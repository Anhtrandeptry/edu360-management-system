package fpt.capstone.edu360managementsystem.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleItemResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;

@Service
public class StudentScheduleService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;

    /** Lấy lịch cho 1 ngày (date) */
    public List<StudentScheduleItemResponse> getScheduleByDate(Long userId, LocalDate date) {
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());
        if (enrollments.isEmpty()) {
            return List.of();
        }

        var classIds = enrollments.stream()
                .map(en -> en.getClazz().getId())
                .distinct()
                .toList();

        List<ClassSession> sessions = classSessionRepository
                .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                        classIds, date, date
                );

        return sessions.stream()
                .map(s -> StudentScheduleItemResponse.builder()
                        .sessionId(s.getId())
                        .classId(s.getClazz().getId())
                        .className(s.getClazz().getName())
                        .subjectName(s.getClazz().getSubject().getName())
                        .teacherName(s.getClazz().getTeacher().getUser().getFullName())
                        .roomName(s.getRoom() != null ? s.getRoom().getName() : "Chưa phân phòng")
                        .date(s.getDate())
                        .timeStart(s.getTimeSlot().getStartTime().toString())
                        .timeEnd(s.getTimeSlot().getEndTime().toString())
                        .build()
                )
                .toList();
    }

    /** Lấy lịch cho 1 tuần: weekStart là ngày bắt đầu (thường là Monday) */
    public List<StudentScheduleItemResponse> getScheduleByWeek(Long userId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6); // 7 ngày: weekStart..weekEnd

        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());
        if (enrollments.isEmpty()) {
            return List.of();
        }

        var classIds = enrollments.stream()
                .map(en -> en.getClazz().getId())
                .distinct()
                .toList();

        List<ClassSession> sessions = classSessionRepository
                .findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
                        classIds, weekStart, weekEnd
                );

        return sessions.stream()
                .map(s -> StudentScheduleItemResponse.builder()
                        .sessionId(s.getId())
                        .classId(s.getClazz().getId())
                        .className(s.getClazz().getName())
                        .subjectName(s.getClazz().getSubject().getName())
                        .teacherName(s.getClazz().getTeacher().getUser().getFullName())
                        .roomName(s.getRoom() != null ? s.getRoom().getName() : "Chưa phân phòng")
                        .date(s.getDate())
                        .timeStart(s.getTimeSlot().getStartTime().toString())
                        .timeEnd(s.getTimeSlot().getEndTime().toString())
                        .build()
                )
                .toList();
    }

    /** Tính ngày đầu tuần (Monday) từ một ngày bất kỳ */
    public LocalDate getCurrentWeekStart(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek(); // MON..SUN
        int diff = dow.getValue() - DayOfWeek.MONDAY.getValue(); // MON=1
        return date.minusDays(diff);
    }
}
