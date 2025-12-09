package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.SessionMaterialResponse;
import fpt.capstone.edu360managementsystem.service.SessionMaterialService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Slf4j
public class SessionMaterialController {

    private final SessionMaterialService materialService;

    @PostMapping("/upload/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<SessionMaterialResponse> uploadMaterial(
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            log.info("Upload material request for session {} by user {}",
                    sessionId, userDetails.getUsername());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Kiểm tra kích thước file (max 50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                log.warn("File too large: {} bytes", file.getSize());
                return ResponseEntity.badRequest().build();
            }

            SessionMaterialResponse response = materialService.uploadMaterial(
                    sessionId, file, description, userDetails.getId());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload material: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/link/{sessionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<SessionMaterialResponse> addLink(
            @PathVariable Long sessionId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String url = request.get("url");
        String title = request.get("title");
        String description = request.get("description");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Add link request for session {} by user {}: {}",
                sessionId, userDetails.getUsername(), url);

        SessionMaterialResponse response = materialService.addLink(
                sessionId, url, title, description, userDetails.getId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SessionMaterialResponse>> getMaterialsBySession(
            @PathVariable Long sessionId) {

        List<SessionMaterialResponse> materials = materialService.getMaterialsBySession(sessionId);
        return ResponseEntity.ok(materials);
    }

    /**
     * Download/redirect đến file trên Cloudinary Do file được lưu trên
     * Cloudinary, ta redirect đến URL trực tiếp
     */
    @GetMapping("/download/{materialId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> downloadMaterial(@PathVariable Long materialId) {
        try {
            SessionMaterialResponse material = materialService.getMaterialById(materialId);
            String fileUrl = material.getFileUrl();

            if (fileUrl == null || fileUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Redirect đến Cloudinary URL
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(fileUrl))
                    .build();

        } catch (Exception e) {
            log.error("Error downloading material: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            materialService.deleteMaterial(materialId, userDetails.getId());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to delete material: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{materialId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SessionMaterialResponse> getMaterial(@PathVariable Long materialId) {
        try {
            SessionMaterialResponse material = materialService.getMaterialById(materialId);
            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
