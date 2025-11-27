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

    private List<ChapterResponse> chapters;   // chỉ những chapter/lesson đã link
    private String content;  // nội dung text buổi học (lesson_content)
}
