package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.NewsRequest;
import fpt.capstone.edu360managementsystem.dto.response.NewsResponse;
import fpt.capstone.edu360managementsystem.entity.News;
import fpt.capstone.edu360managementsystem.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;

    /**
     * Lấy danh sách tin tức (có phân trang, tìm kiếm, lọc)
     */
    public Page<NewsResponse> getNewsList(String search, String status, int page, int size, String sortBy, String order) {
        Sort sort = Sort.by(Sort.Direction.fromString(order.toUpperCase()), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<News> newsPage;
        
        if (search != null && !search.trim().isEmpty()) {
            if (status != null && !status.trim().isEmpty()) {
                // Search + filter by status
                newsPage = newsRepository.searchNews(search, pageable)
                        .map(news -> news.getStatus().equalsIgnoreCase(status) ? news : null)
                        .map(news -> news);
            } else {
                newsPage = newsRepository.searchNews(search, pageable);
            }
        } else if (status != null && !status.trim().isEmpty()) {
            newsPage = newsRepository.findByStatus(status.toUpperCase(), pageable);
        } else {
            newsPage = newsRepository.findAll(pageable);
        }

        return newsPage.map(this::toResponse);
    }

    /**
     * Lấy tin tức PUBLISHED cho guest
     */
    public Page<NewsResponse> getPublishedNews(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        Page<News> newsPage;
        if (search != null && !search.trim().isEmpty()) {
            newsPage = newsRepository.searchPublishedNews(search, pageable);
        } else {
            newsPage = newsRepository.findPublishedNews(pageable);
        }

        return newsPage.map(this::toResponse);
    }

    /**
     * Lấy chi tiết tin tức theo ID
     */
    public NewsResponse getNewsById(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        return toResponse(news);
    }

    /**
     * Tạo tin tức mới
     */
    @Transactional
    public NewsResponse createNews(NewsRequest request) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setExcerpt(request.getExcerpt());
        news.setContent(request.getContent());
        news.setImageUrl(request.getImageUrl());
        news.setAuthor(request.getAuthor());
        news.setStatus(request.getStatus().toUpperCase());
        news.setViews(0);
        
        if (request.getTags() != null) {
            news.setTags(String.join(",", request.getTags()));
        }

        if ("PUBLISHED".equalsIgnoreCase(request.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }

        News savedNews = newsRepository.save(news);
        return toResponse(savedNews);
    }

    /**
     * Cập nhật tin tức
     */
    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));

        news.setTitle(request.getTitle());
        news.setExcerpt(request.getExcerpt());
        news.setContent(request.getContent());
        news.setImageUrl(request.getImageUrl());
        news.setAuthor(request.getAuthor());
        
        // Nếu chuyển từ DRAFT sang PUBLISHED, set publishedAt
        if ("PUBLISHED".equalsIgnoreCase(request.getStatus()) && 
            !"PUBLISHED".equalsIgnoreCase(news.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        
        news.setStatus(request.getStatus().toUpperCase());
        
        if (request.getTags() != null) {
            news.setTags(String.join(",", request.getTags()));
        }

        News updatedNews = newsRepository.save(news);
        return toResponse(updatedNews);
    }

    /**
     * Xóa tin tức
     */
    @Transactional
    public void deleteNews(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        newsRepository.delete(news);
    }

    /**
     * Tăng lượt xem
     */
    @Transactional
    public void incrementView(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));
        news.setViews(news.getViews() + 1);
        newsRepository.save(news);
    }

    /**
     * Cập nhật trạng thái
     */
    @Transactional
    public NewsResponse updateStatus(Long id, String status) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));

        if ("PUBLISHED".equalsIgnoreCase(status) && 
            !"PUBLISHED".equalsIgnoreCase(news.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news.setStatus(status.toUpperCase());
        News updatedNews = newsRepository.save(news);
        return toResponse(updatedNews);
    }

    /**
     * Convert entity sang response DTO
     */
    private NewsResponse toResponse(News news) {
        List<String> tags = null;
        if (news.getTags() != null && !news.getTags().isEmpty()) {
            tags = Arrays.stream(news.getTags().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        return NewsResponse.builder()
                .id(news.getId())
                .title(news.getTitle())
                .excerpt(news.getExcerpt())
                .content(news.getContent())
                .imageUrl(news.getImageUrl())
                .author(news.getAuthor())
                .status(news.getStatus())
                .views(news.getViews())
                .tags(tags)
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
