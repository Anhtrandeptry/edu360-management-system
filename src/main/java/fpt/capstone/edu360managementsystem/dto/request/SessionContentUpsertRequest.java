package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SessionContentUpsertRequest {

    private List<Long> chapterIds;
    private List<Long> lessonIds;
    private String content;


    private String sourceType;
    private Long classCourseId;


    private Long chapterId;
    private Long lessonId;

}
