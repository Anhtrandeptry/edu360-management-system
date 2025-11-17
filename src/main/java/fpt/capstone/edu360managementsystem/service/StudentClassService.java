package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentClassService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassEnrollmentRepository classEnrollmentRepository;
    //st

    public List<StudentClassResponse> getMyClasses(Long userId) {
        // map user -> student
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        List<ClassEnrollment> enrollments = classEnrollmentRepository.findByStudent_Id(student.getId());

        return enrollments.stream()
                .map(en -> {
                    var clazz = en.getClazz();
                    return StudentClassResponse.builder()
                            .classId(clazz.getId())
                            .className(clazz.getName())
                            .subjectName(clazz.getSubject().getName())
                            .teacherName(clazz.getTeacher().getUser().getFullName())
                            .roomName(clazz.getRoom().getName())
                            .semesterName(clazz.getSemester().getName())
                            .startDate(clazz.getStartDate())
                            .endDate(clazz.getEndDate())
                            .status(clazz.getStatus())
                            .build();
                })
                .toList();
    }
}
