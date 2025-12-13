package fpt.capstone.edu360managementsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fpt.capstone.edu360managementsystem.dto.request.ChangePasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.StudentProfileUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.response.StudentProfileResponse;
import fpt.capstone.edu360managementsystem.service.CloudinaryService;
import fpt.capstone.edu360managementsystem.service.StudentProfileService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for student profile management.
 * Provides endpoints for viewing/updating profile, uploading avatar, and changing password.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/students/profile")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STUDENT')")
public class StudentProfileController {

    private final StudentProfileService studentProfileService;
    private final CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String ERROR_KEY = "error";

    private Long getAuthenticatedUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new SecurityException("User not authenticated");
        }
        return ((UserDetailsImpl) auth.getPrincipal()).getId();
    }

    /**
     * Retrieves the authenticated student's profile.
     *
     * @param auth the authentication object
     * @return student profile details
     */
    @GetMapping
    public ResponseEntity<StudentProfileResponse> getProfile(Authentication auth) {
        Long userId = getAuthenticatedUserId(auth);
        StudentProfileResponse response = studentProfileService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated student's profile.
     *
     * @param auth    the authentication object
     * @param request the profile update data
     * @return updated profile details
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
     * Uploads a new avatar for the authenticated student.
     *
     * @param file the avatar image file (max 5MB)
     * @param auth the authentication object
     * @return URL of the uploaded avatar
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        try {
            Long userId = getAuthenticatedUserId(auth);

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

            String fileUrl = cloudinaryService.uploadImage(file, "avatars");

            studentProfileService.updateAvatar(userId, fileUrl);

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            log.info("Avatar uploaded for student userId={}: {}", userId, fileUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * Changes the password for the authenticated student.
     *
     * @param auth    the authentication object
     * @param request the password change data
     * @return success or error message
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
