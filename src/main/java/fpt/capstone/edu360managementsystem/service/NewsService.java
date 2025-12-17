package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.NewsRequest;
import fpt.capstone.edu360managementsystem.dto.response.NewsResponse;
import fpt.capstone.edu360managementsystem.entity.News;
import fpt.capstone.edu360managementsystem.enums.NewsStatus;
import fpt.capstone.edu360managementsystem.repository.NewsRepository;
import lombok.RequiredArgsConstructor;

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

        // Parse status thành enum nếu có
        NewsStatus newsStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                newsStatus = NewsStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore filter
            }
        }

        if (search != null && !search.trim().isEmpty()) {
            if (newsStatus != null) {
                // Search + filter by status
                final NewsStatus filterStatus = newsStatus;
                newsPage = newsRepository.searchNews(search, pageable)
                        .map(news -> news.getStatus() == filterStatus ? news : null)
                        .map(news -> news);
            } else {
                newsPage = newsRepository.searchNews(search, pageable);
            }
        } else if (newsStatus != null) {
            newsPage = newsRepository.findByStatus(newsStatus, pageable);
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
     *
     * @param request thông tin tin tức
     * @param authorUsername username của người dùng đang đăng nhập (tác giả)
     */
    @Transactional
    public NewsResponse createNews(NewsRequest request, String authorUsername) {
        // Kiểm tra nếu request có author và khác với user đang đăng nhập
        if (request.getAuthor() != null && !request.getAuthor().trim().isEmpty()
                && !request.getAuthor().equalsIgnoreCase(authorUsername)) {
            throw new IllegalArgumentException(
                    "Bạn không thể đặt author là người khác. Chỉ được phép sử dụng tài khoản của bạn: " + authorUsername);
        }

        News news = new News();
        news.setTitle(request.getTitle());
        news.setExcerpt(request.getExcerpt());
        news.setContent(request.getContent());
        news.setImageUrl(request.getImageUrl());
        // Author được lấy từ user đang đăng nhập, không cho phép sửa từ request
        news.setAuthor(authorUsername);
        news.setStatus(request.getStatus());
        news.setViews(0);

        if (request.getTags() != null) {
            news.setTags(String.join(",", request.getTags()));
        }

        if (NewsStatus.PUBLISHED == request.getStatus()) {
            news.setPublishedAt(LocalDateTime.now());
        }

        News savedNews = newsRepository.save(news);
        return toResponse(savedNews);
    }

    /**
     * Cập nhật tin tức
     *
     * @param id ID của tin tức cần cập nhật
     * @param request thông tin cập nhật
     * @param authorUsername username của người dùng đang đăng nhập (không thay
     * đổi author gốc)
     */
    @Transactional
    public NewsResponse updateNews(Long id, NewsRequest request, String authorUsername) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found with id: " + id));

        // Kiểm tra nếu request có author và khác với author hiện tại (không cho phép thay đổi)
        if (request.getAuthor() != null && !request.getAuthor().trim().isEmpty()
                && !request.getAuthor().equalsIgnoreCase(news.getAuthor())) {
            throw new IllegalArgumentException(
                    "Bạn không thể sửa author thành người khác. Author gốc: " + news.getAuthor());
        }

        news.setTitle(request.getTitle());
        news.setExcerpt(request.getExcerpt());
        news.setContent(request.getContent());
        news.setImageUrl(request.getImageUrl());
        // Giữ nguyên author gốc khi cập nhật, không cho phép thay đổi từ request
        // Author đã được set khi tạo tin tức và không thay đổi khi edit

        // Nếu chuyển từ DRAFT sang PUBLISHED, set publishedAt
        if (NewsStatus.PUBLISHED == request.getStatus()
                && NewsStatus.PUBLISHED != news.getStatus()) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news.setStatus(request.getStatus());

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

        // Validate và convert status sang enum
        NewsStatus newsStatus;
        try {
            newsStatus = NewsStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ: '" + status + "'. Chỉ chấp nhận: DRAFT hoặc PUBLISHED");
        }

        if (NewsStatus.PUBLISHED == newsStatus
                && NewsStatus.PUBLISHED != news.getStatus()) {
            news.setPublishedAt(LocalDateTime.now());
        }

        news.setStatus(newsStatus);
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
                .status(news.getStatus().name())
                .views(news.getViews())
                .tags(tags)
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .publishedAt(news.getPublishedAt())
                .build();
    }
}
