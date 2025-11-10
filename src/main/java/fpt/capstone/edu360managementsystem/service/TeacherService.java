package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Service for Teacher entity operations. Provides business logic for teacher
 * management.
 */
@Service
public class TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    /**
     * Get all teachers, optionally filtered by subject.
     *
     * @param subjectId Optional subject ID to filter teachers
     * @return List of teacher responses
     */
    public List<TeacherResponse> getTeachers(Long subjectId) {
        List<Teacher> teachers;

        if (subjectId != null) {
            teachers = teacherRepository.findBySubjectId(subjectId);
        } else {
            teachers = teacherRepository.findAll();
        }

        return teachers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map Teacher entity to TeacherResponse DTO.
     */
    private TeacherResponse mapToResponse(Teacher teacher) {
        Long subjectId = null;
        String subjectName = null;
        try {
            if (teacher.getSubject() != null) {
                subjectId = teacher.getSubject().getId();
                subjectName = teacher.getSubject().getName();
            }
        } catch (EntityNotFoundException ex) {
            // Inconsistent FK (e.g., subject_id points to missing Subject). Tolerate and return nulls.
            subjectId = null;
            subjectName = null;
        }

        long count = 0L;
        try {
            count = clazzRepository.countActiveByTeacherUser(teacher.getUser().getId());
        } catch (Exception ex) {
            // Defensive: nếu query lỗi do mapping lazy bất thường, trả về 0
            count = 0L;
        }
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUser().getId())
                .username(teacher.getUser().getUsername())
                .fullName(teacher.getUser().getFullName())
                .email(teacher.getUser().getEmail())
                .phoneNumber(teacher.getUser().getPhoneNumber())
                .subjectId(subjectId)
                .subjectName(subjectName)
                .specialization(teacher.getSpecialization())
                .degree(teacher.getDegree())
                .active(teacher.getUser().getActive())
                .classCount(count)
                .build();
    }

    /**
     * Get a single teacher by the associated user id. Returns null if not found
     * (controller will translate to 404).
     */
    public TeacherResponse getByUserId(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found for userId=" + userId));
        return mapToResponse(teacher);
    }
}
