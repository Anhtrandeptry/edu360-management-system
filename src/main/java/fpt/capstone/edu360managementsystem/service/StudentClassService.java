package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;

@Service
public class StudentClassService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Autowired
    private ClassSessionRepository classSessionRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;

        public List<StudentClassResponse> getMyClasses(Long userId) {
        try {
            // map user -> student
            Student student = studentRepository.findByUser_Id(userId)
                .orElse(null);

            if (student == null) {
            // No student profile linked to this user; return empty instead of error
            org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                .warn("[StudentClassService] No student profile found for userId={}", userId);
            return java.util.Collections.emptyList();
            }

            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());

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
                org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                    .warn("[StudentClassService] No student profile found for userId={}", userId);
                return java.util.Collections.emptyList();
            }

            // Lấy các lớp đã đăng ký
            List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());
            
            if (enrollments.isEmpty()) {
                org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                    .info("[StudentClassService] No enrolled classes found for student={}", student.getId());
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

            org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                .info("[StudentClassService] Found {} sessions for student={} in week {}-{}", 
                    sessions.size(), student.getId(), weekStart, weekEnd);

            // Batch load attendance records for these sessions for the student
            var sessionIds = sessions.stream().map(ClassSession::getId).toList();
            List<Attendance> attendanceRecords = attendanceRepository.findBySession_IdInAndStudent_Id(sessionIds, student.getId());
            var statusBySession = attendanceRecords.stream()
                .collect(java.util.stream.Collectors.toMap(a -> a.getSession().getId(), Attendance::getStatus));

            // Map sang response (kèm trạng thái điểm danh)
            return sessions.stream()
                .map(session -> {
                    AttendanceStatus st = statusBySession.getOrDefault(session.getId(), AttendanceStatus.UNMARKED);
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
                        .build();
                })
                .toList();

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(StudentClassService.class)
                .error("[StudentClassService] getMyScheduleByWeek failed for userId={}: {}", 
                    userId, e.getMessage(), e);
            return java.util.Collections.emptyList();
        }
    }
}
