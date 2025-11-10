package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.response.UserResponse;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.mapper.UserMapper;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatus(Long userId, Boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check if user is a teacher and is in use
        boolean isTeacher = user.getRoles().stream().anyMatch(r -> r.getName().name().equals("TEACHER"));
        if (isTeacher && Boolean.FALSE.equals(active)) {
            // Check if teacher is assigned to any active class
            var teacher = teacherRepository.findByUserId(user.getId());
            if (teacher.isPresent()) {
                long cnt = clazzRepository.countActiveByTeacherId(teacher.get().getId());
                if (cnt > 0) {
                    throw new RuntimeException("Không thể vô hiệu hóa giáo viên: đang được phân công " + cnt + " lớp học chưa hoàn thành");
                }
            }
        }

        // Check if user is a student and is in use
        boolean isStudent = user.getRoles().stream().anyMatch(r -> r.getName().name().equals("STUDENT"));
        if (isStudent && Boolean.FALSE.equals(active)) {
            // TODO: Check if student is enrolled in any active class (implement if needed)
            // For now, just allow
        }

        // Check if user is a parent and is in use
        boolean isParent = user.getRoles().stream().anyMatch(r -> r.getName().name().equals("PARENT"));
        if (isParent && Boolean.FALSE.equals(active)) {
            // TODO: Check if parent has any children enrolled in active classes (implement if needed)
            // For now, just allow
        }

        user.setActive(active);
        userRepository.save(user);
    }

}
