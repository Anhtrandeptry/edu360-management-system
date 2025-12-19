package fpt.capstone.edu360managementsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for news management. Provides endpoints for CRUD operations
 * on news articles.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final CloudinaryService cloudinaryService;

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String ERROR_KEY = "error";

    /**
     * Uploads an image for news articles.
     *
     * @param file the image file (max 5MB)
     * @return URL of the uploaded image
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        try {
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
     * Retrieves paginated news list. Public users see only PUBLISHED; Admin
     * sees all.
     *
     * @param search optional search term
     * @param status status filter
     * @param page page number
     * @param size page size
     * @param sortBy sort field
     * @param order sort order
     * @return paginated news list
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
        Page<NewsResponse> newsPage;
        try {
            newsPage = newsService.getNewsList(search, status, page, size, sortBy, order);
        } catch (Exception e) {
            newsPage = newsService.getPublishedNews(search, page, size);
        }
        return ResponseEntity.ok(newsPage);
    }

    /**
     * Retrieves published news for public access.
     *
     * @param search optional search term
     * @param page page number
     * @param size page size
     * @return paginated published news
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
     * Retrieves news details by ID.
     *
     * @param id the news ID
     * @return news details
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }

    /**
     * Creates a new news article.
     *
     * @param request news data
     * @param currentUser the authenticated user
     * @return created news
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> createNews(
            @Valid @RequestBody NewsRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        NewsResponse news = newsService.createNews(request, currentUser.getUsername());
        return ResponseEntity.ok(news);
    }

    /**
     * Updates an existing news article.
     *
     * @param id the news ID
     * @param request updated news data
     * @param currentUser the authenticated user
     * @return updated news
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsResponse> updateNews(
            @PathVariable Long id,
            @Valid @RequestBody NewsRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser
    ) {
        NewsResponse news = newsService.updateNews(id, request, currentUser.getUsername());
        return ResponseEntity.ok(news);
    }

    /**
     * Deletes a news article.
     *
     * @param id the news ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves news statistics by status.
     *
     * @return map containing counts for each status
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getNewsStats() {
        Map<String, Long> stats = newsService.getNewsStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Increments the view count for a news article.
     *
     * @param id the news ID
     * @return success response
     */
    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        newsService.incrementView(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the status of a news article.
     *
     * @param id the news ID
     * @param body map containing the new status
     * @return updated news
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
