package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.LessonMaterialResponse;
import fpt.capstone.edu360managementsystem.service.LessonMaterialService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý tài liệu bài học (CourseLesson)
 * - Giáo viên: upload, xóa tài liệu
 * - Học sinh: xem, download tài liệu
 */
@RestController
@RequestMapping("/api/lesson-materials")
@RequiredArgsConstructor
@Slf4j
public class LessonMaterialController {

    private final LessonMaterialService materialService;

    /**
     * Upload tài liệu cho bài học
     * Chỉ giáo viên hoặc admin mới có quyền
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
            
            // Kiểm tra kích thước file (max 50MB)
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
     * Thêm link tài liệu cho bài học
     * Chỉ giáo viên hoặc admin mới có quyền
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
     * Lấy danh sách tài liệu của lesson
     * Cả giáo viên và học sinh đều có thể xem
     */
    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonMaterialResponse>> getMaterialsByLesson(
            @PathVariable Long lessonId) {
        
        List<LessonMaterialResponse> materials = materialService.getMaterialsByLesson(lessonId);
        return ResponseEntity.ok(materials);
    }

    /**
     * Lấy danh sách tài liệu của chapter (tất cả lessons trong chapter)
     */
    @GetMapping("/chapter/{chapterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LessonMaterialResponse>> getMaterialsByChapter(
            @PathVariable Long chapterId) {
        
        List<LessonMaterialResponse> materials = materialService.getMaterialsByChapter(chapterId);
        return ResponseEntity.ok(materials);
    }

    /**
     * Download tài liệu
     * Không cần authentication để đơn giản hóa việc download
     */
    @GetMapping("/download/{lessonId}/{fileName}")
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable Long lessonId,
            @PathVariable String fileName) {
        
        try {
            Path filePath = materialService.getFilePath(lessonId, fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }
            
            // Detect content type
            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
            } catch (IOException e) {
                log.warn("Could not determine content type for: {}", fileName);
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
                    
        } catch (MalformedURLException e) {
            log.error("Invalid file path: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Xóa tài liệu
     * Chỉ giáo viên upload hoặc admin mới có quyền xóa
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
     * Lấy thông tin chi tiết một tài liệu
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
