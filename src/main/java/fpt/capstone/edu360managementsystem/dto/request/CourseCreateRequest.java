package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseCreateRequest {

    @NotNull
    private Long subjectId;

    @NotBlank
    private String title;

    private String description;
}
