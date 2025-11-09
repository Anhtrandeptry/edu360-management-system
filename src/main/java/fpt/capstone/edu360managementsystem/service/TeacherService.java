package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.entity.Teacher;
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
                .build();
    }
}
