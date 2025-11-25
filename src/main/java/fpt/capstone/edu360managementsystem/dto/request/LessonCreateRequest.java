package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonCreateRequest {

    @NotNull
    private Long chapterId;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Integer orderIndex;
}
