package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.LessonMaterialResponse;
import fpt.capstone.edu360managementsystem.service.LessonMaterialService;
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

/**
 * REST controller for lesson material management.
 * Provides endpoints for uploading, downloading, and managing course lesson materials.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/lesson-materials")
@RequiredArgsConstructor
@Slf4j
public class LessonMaterialController {

    private final LessonMaterialService materialService;

    /**
     * Uploads a material file for a lesson.
     *
     * @param lessonId    the lesson ID
     * @param file        the material file (max 50MB)
     * @param description optional file description
     * @param userDetails the authenticated user
     * @return uploaded material response
     */
    @PostMapping("/upload/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonMaterialResponse> uploadMaterial(
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            log.info("Upload lesson material request for lesson {} by user {}",
                    lessonId, userDetails.getUsername());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (file.getSize() > 50 * 1024 * 1024) {
                log.warn("File too large: {} bytes", file.getSize());
                return ResponseEntity.badRequest().build();
            }

            LessonMaterialResponse response = materialService.uploadMaterial(
                    lessonId, file, description, userDetails.getId());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload lesson material: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Adds a link material to a lesson.
     *
     * @param lessonId    the lesson ID
     * @param request     map containing the URL
     * @param userDetails the authenticated user
     * @return created link material response
     */
    @PostMapping("/link/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonMaterialResponse> addLink(
            @PathVariable Long lessonId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        String url = request.get("url");

        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Add link request for lesson {} by user {}: {}",
                lessonId, userDetails.getUsername(), url);

        LessonMaterialResponse response = materialService.addLink(
                lessonId, url, userDetails.getId());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all materials for a lesson.
     *
     * @param lessonId the lesson ID
     * @return list of lesson materials
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonMaterialResponse>> getMaterialsByLesson(
            @PathVariable Long lessonId) {

        List<LessonMaterialResponse> materials = materialService.getMaterialsByLesson(lessonId);
        return ResponseEntity.ok(materials);
    }

    /**
     * Retrieves all materials for a chapter.
     *
     * @param chapterId the chapter ID
     * @return list of lesson materials in the chapter
     */
    @GetMapping("/chapter/{chapterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonMaterialResponse>> getMaterialsByChapter(
            @PathVariable Long chapterId) {

        List<LessonMaterialResponse> materials = materialService.getMaterialsByChapter(chapterId);
        return ResponseEntity.ok(materials);
    }

    /**
     * Downloads or redirects to the material file on Cloudinary.
     *
     * @param materialId the material ID
     * @return redirect to Cloudinary URL
     */
    @GetMapping("/download/{materialId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> downloadMaterial(@PathVariable Long materialId) {
        try {
            LessonMaterialResponse material = materialService.getMaterialById(materialId);
            String fileUrl = material.getFileUrl();

            if (fileUrl == null || fileUrl.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(fileUrl))
                    .build();

        } catch (Exception e) {
            log.error("Error downloading material: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes a lesson material.
     *
     * @param materialId  the material ID
     * @param userDetails the authenticated user
     * @return success or error response
     */
    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteMaterial(
            @PathVariable Long materialId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        try {
            materialService.deleteMaterial(materialId, userDetails.getId());
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to delete lesson material: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves a material by ID.
     *
     * @param materialId the material ID
     * @return material details
     */
    @GetMapping("/{materialId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonMaterialResponse> getMaterial(@PathVariable Long materialId) {
        try {
            LessonMaterialResponse material = materialService.getMaterialById(materialId);
            return ResponseEntity.ok(material);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
