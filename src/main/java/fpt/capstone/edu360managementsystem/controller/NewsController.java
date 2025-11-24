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

import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

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
