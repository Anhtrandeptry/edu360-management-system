package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.ChangePasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.StudentProfileUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.response.StudentProfileResponse;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for student profile management.
 * Handles profile retrieval, update, avatar upload, and password change.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudentProfileService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get student profile by user ID.
     */
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long userId) {
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found for user: " + userId));
        
        return mapToResponse(student);
    }

    /**
     * Update student profile.
     */
    @Transactional
    public StudentProfileResponse updateProfile(Long userId, StudentProfileUpdateRequest request) {
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found for user: " + userId));
        
        User user = student.getUser();
        
        // Update user fields
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check if email already exists for another user
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
                throw new RuntimeException("Email already in use by another account");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        // Update student fields
        if (request.getDob() != null) {
            student.setDob(request.getDob());
        }
        if (request.getGrade() != null) {
            student.setGrade(request.getGrade());
        }
        if (request.getSchool() != null) {
            student.setSchool(request.getSchool());
        }
        if (request.getAvatarUrl() != null) {
            student.setAvatarUrl(request.getAvatarUrl());
        }
        
        userRepository.save(user);
        studentRepository.save(student);
        
        log.info("Updated profile for student userId={}", userId);
        return mapToResponse(student);
    }

    /**
     * Update student avatar URL.
     */
    @Transactional
    public void updateAvatar(Long userId, String avatarUrl) {
        Student student = studentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found for user: " + userId));
        
        student.setAvatarUrl(avatarUrl);
        studentRepository.save(student);
        
        log.info("Updated avatar for student userId={}", userId);
    }

    /**
     * Change student password.
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        
        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Changed password for user userId={}", userId);
    }

    /**
     * Map Student entity to StudentProfileResponse.
     */
    private StudentProfileResponse mapToResponse(Student student) {
        User user = student.getUser();
        Parent parent = student.getParent();
        
        StudentProfileResponse.ParentInfo parentInfo = null;
        if (parent != null && parent.getUser() != null) {
            User parentUser = parent.getUser();
            parentInfo = StudentProfileResponse.ParentInfo.builder()
                    .id(parent.getId())
                    .fullName(parentUser.getFullName())
                    .email(parentUser.getEmail())
                    .phoneNumber(parentUser.getPhoneNumber())
                    .occupation(parent.getOccupation())
                    .address(parent.getAddress())
                    .build();
        }
        
        return StudentProfileResponse.builder()
                .id(student.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(student.getAvatarUrl())
                .dob(student.getDob())
                .grade(student.getGrade())
                .school(student.getSchool())
                .parent(parentInfo)
                .isActive(user.getActive())
                .build();
    }
}
