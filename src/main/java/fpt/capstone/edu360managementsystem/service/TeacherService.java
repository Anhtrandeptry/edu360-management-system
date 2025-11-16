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
            teachers = teacherRepository.findByAnySubject(subjectId);
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
        java.util.List<Long> subjectIds = new java.util.ArrayList<>();
        java.util.List<String> subjectNames = new java.util.ArrayList<>();
        try {
            // Nếu có subject chính populate trước
            if (teacher.getSubject() != null) {
                try {
                    subjectId = teacher.getSubject().getId();
                    subjectName = teacher.getSubject().getName();
                } catch (EntityNotFoundException ex) {
                    subjectId = null;
                    subjectName = null;
                }
            }
            // Thêm tất cả môn từ tập subjects (nếu chưa có)
            if (teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
                for (var s : teacher.getSubjects()) {
                    try {
                        Long sid = s.getId();
                        String sname = s.getName();
                        if (!subjectIds.contains(sid)) {
                            subjectIds.add(sid);
                        }
                        if (!subjectNames.contains(sname)) {
                            subjectNames.add(sname);
                        }
                    } catch (EntityNotFoundException inner) {
                        // skip
                    }
                }
                // Fallback nếu subject chính null thì lấy first từ list
                if (subjectId == null && !subjectIds.isEmpty()) {
                    subjectId = subjectIds.get(0);
                    subjectName = subjectNames.get(0);
                }
            }
        } catch (EntityNotFoundException ex) {
            // ignore, giữ nulls
        }

        long count;
        try {
            count = clazzRepository.countActiveByTeacherUser(teacher.getUser().getId());
        } catch (Exception ex) {
            count = 0L; // Defensive fallback
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
                .subjectIds(subjectIds)
                .subjectNames(subjectNames)
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
