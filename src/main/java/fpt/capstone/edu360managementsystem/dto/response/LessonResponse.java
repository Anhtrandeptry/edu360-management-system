package fpt.capstone.edu360managementsystem.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonResponse {

    private Long id;
    private Long chapterId;
    private String title;
    private String description;
    private Integer orderIndex;
}
