package fpt.capstone.edu360managementsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fpt.capstone.edu360managementsystem.dto.request.NewsRequest;
import fpt.capstone.edu360managementsystem.dto.response.NewsResponse;
import fpt.capstone.edu360managementsystem.service.CloudinaryService;
import fpt.capstone.edu360managementsystem.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final CloudinaryService cloudinaryService;

    // ===================== IMAGE UPLOAD =====================
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String ERROR_KEY = "error";

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

            // Upload to Cloudinary
            String fileUrl = cloudinaryService.uploadImage(file, "news-images");

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        }
    }

    /**
     * GET /api/news - Lấy danh sách tin tức (có phân trang) Public: Chỉ lấy
     * PUBLISHED Admin: Lấy tất cả
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
