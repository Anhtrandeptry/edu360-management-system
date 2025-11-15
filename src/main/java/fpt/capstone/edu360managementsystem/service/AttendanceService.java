package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.AttendanceUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.*;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassSessionRepository classSessionRepository;
    @Autowired private ClassEnrollmentRepository classEnrollmentRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private StudentRepository studentRepository;

    /** Danh sách buổi dạy hôm nay của giáo viên (theo userId) */
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

    /** Chi tiết 1 buổi: danh sách HS & trạng thái hiện tại — chỉ buổi thuộc giáo viên */
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

    /** Chấm/ cập nhật điểm danh — CHỈ trong đúng ngày */
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
                        .build());
            } else {
                existing.setStatus(item.getStatus()); // cho phép cập nhật TRONG ngày
                attendanceRepository.save(existing);
            }
        }
    }
}
