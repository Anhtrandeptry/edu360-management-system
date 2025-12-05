package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.ChangePasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.StudentProfileUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.response.StudentProfileResponse;
import fpt.capstone.edu360managementsystem.service.StudentProfileService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Student Profile Controller - allows students to manage their own profile.
 * Includes: view profile, update profile, upload avatar, change password.
 */
@RestController
@RequestMapping("/api/students/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    
    private static final String UPLOAD_DIR = "uploads/avatars/";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String ERROR_KEY = "error";

    /**
     * Helper method to get user ID from authentication.
     */
    private Long getAuthenticatedUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new SecurityException("User not authenticated");
        }
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    /**
     * Get current student profile.
     */
    @GetMapping
    public ResponseEntity<StudentProfileResponse> getProfile(Authentication auth) {
        Long userId = getAuthenticatedUserId(auth);
        StudentProfileResponse response = studentProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update current student profile.
     */
    @PutMapping
    public ResponseEntity<StudentProfileResponse> updateProfile(
            Authentication auth,
            @Valid @RequestBody StudentProfileUpdateRequest request) {
        Long userId = getAuthenticatedUserId(auth);
        StudentProfileResponse response = studentProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload avatar for current student.
     * Saves file to server and returns URL.
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            Long userId = getAuthenticatedUserId(auth);
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File is empty"));
            }
            
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File size exceeds 5MB"));
            }
            
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File must be an image"));
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = "student_" + userId + "_" + UUID.randomUUID().toString() + extension;
            
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return URL (relative path that will be served by static resources)
            String fileUrl = "/uploads/avatars/" + filename;
            
            // Update avatar URL in database
            studentProfileService.updateAvatar(userId, fileUrl);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            
            log.info("Avatar uploaded for student userId={}: {}", userId, fileUrl);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to upload avatar", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Change password for current student.
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            Long userId = getAuthenticatedUserId(auth);
            studentProfileService.changePassword(userId, request);
            
            log.info("Password changed for student userId={}", userId);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
            
        } catch (RuntimeException e) {
            log.warn("Password change failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, e.getMessage()));
        }
    }
}
