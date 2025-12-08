package fpt.capstone.edu360managementsystem.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionContentResponse {

    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String courseTitle;
    private Long baseCourseId;
    private String sourceType;
    private Long classCourseId;
    private Long chapterId;
    private Long lessonId;
    private List<Long> linkedChapterIds;
    private List<Long> linkedLessonIds;

    private List<ChapterResponse> chapters;
    private String content;
}
