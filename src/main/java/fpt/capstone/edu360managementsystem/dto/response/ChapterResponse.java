package fpt.capstone.edu360managementsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChapterResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String description;
    private Integer orderIndex;

    private List<LessonResponse> lessons;
}
