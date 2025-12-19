package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import fpt.capstone.edu360managementsystem.enums.NewsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NewsRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    private String excerpt;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    private String imageUrl;

    private String author;

    @NotNull(message = "Trạng thái không được để trống. Chỉ chấp nhận: DRAFT hoặc PUBLISHED")
    private NewsStatus status;

    private List<String> tags;
}
