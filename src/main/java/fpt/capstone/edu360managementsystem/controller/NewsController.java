package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.NewsRequest;
import fpt.capstone.edu360managementsystem.dto.response.NewsResponse;
import fpt.capstone.edu360managementsystem.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    
    // ===================== IMAGE UPLOAD =====================
    private static final String UPLOAD_DIR = "uploads/news-images/";
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String ERROR_KEY = "error";
    
    /**
     * POST /api/news/upload-image - Upload ảnh cho tin tức (Admin only)
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File is empty"));
            }
            
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File size exceeds 5MB"));
            }
            
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File must be an image (PNG, JPG, JPEG, GIF, WebP)"));
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
            String filename = UUID.randomUUID().toString() + extension;
            
            // Create upload directory if not exists
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Return URL
            String fileUrl = "/uploads/news-images/" + filename;
            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * GET /api/news - Lấy danh sách tin tức (có phân trang)
     * Public: Chỉ lấy PUBLISHED
     * Admin: Lấy tất cả
     */
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNewsList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order
    ) {
        // Nếu không có auth hoặc không phải admin, chỉ lấy PUBLISHED
        Page<NewsResponse> newsPage;
        try {
            // Admin có thể xem tất cả
            newsPage = newsService.getNewsList(search, status, page, size, sortBy, order);
        } catch (Exception e) {
            // Guest chỉ xem PUBLISHED
            newsPage = newsService.getPublishedNews(search, page, size);
        }
        return ResponseEntity.ok(newsPage);
    }

    /**
     * GET /api/news/public - Lấy tin tức PUBLISHED cho guest (không cần auth)
     */
    @GetMapping("/public")
    public ResponseEntity<Page<NewsResponse>> getPublishedNews(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<NewsResponse> newsPage = newsService.getPublishedNews(search, page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * GET /api/news/{id} - Lấy chi tiết tin tức
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }

    /**
     * POST /api/news - Tạo tin tức mới (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> createNews(@Valid @RequestBody NewsRequest request) {
        NewsResponse news = newsService.createNews(request);
        return ResponseEntity.ok(news);
    }

    /**
     * PUT /api/news/{id} - Cập nhật tin tức (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> updateNews(
            @PathVariable Long id,
            @Valid @RequestBody NewsRequest request
    ) {
        NewsResponse news = newsService.updateNews(id, request);
        return ResponseEntity.ok(news);
    }

    /**
     * DELETE /api/news/{id} - Xóa tin tức (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/news/{id}/view - Tăng lượt xem
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        newsService.incrementView(id);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/news/{id}/status - Cập nhật trạng thái (Admin only)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status");
        NewsResponse news = newsService.updateStatus(id, status);
        return ResponseEntity.ok(news);
    }
}
