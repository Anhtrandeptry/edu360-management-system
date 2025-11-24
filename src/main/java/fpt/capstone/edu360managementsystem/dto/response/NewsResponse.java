package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponse {

    private Long id;
    private String title;
    private String excerpt;
    private String content;
    private String imageUrl;
    private String author;
    private String status;
    private Integer views;
    private List<String> tags; // Parsed từ string tags
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
