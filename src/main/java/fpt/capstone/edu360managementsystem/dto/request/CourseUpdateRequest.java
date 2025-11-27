package fpt.capstone.edu360managementsystem.dto.request;

import lombok.Data;

@Data
public class CourseUpdateRequest {

    private String title;
    private String description;
    private Long subjectId;
}
