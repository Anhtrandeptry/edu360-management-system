package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;

@Service
public class StudentClassService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;

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
}
