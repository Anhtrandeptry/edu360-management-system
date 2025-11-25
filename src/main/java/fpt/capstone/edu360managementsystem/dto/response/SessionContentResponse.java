package fpt.capstone.edu360managementsystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionContentResponse {

    private Long sessionId;
    private Long classId;
    private String className;
    private String subjectName;
    private String courseTitle;

    private List<ChapterResponse> chapters;   // chỉ những chapter/lesson đã link
}
