package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String excerpt;

    @NotBlank(message = "Content is required")
    private String content;

    private String imageUrl;

    private String author;

    @NotNull(message = "Status is required")
    private String status; // DRAFT, PUBLISHED, HIDDEN

    private List<String> tags;
}
