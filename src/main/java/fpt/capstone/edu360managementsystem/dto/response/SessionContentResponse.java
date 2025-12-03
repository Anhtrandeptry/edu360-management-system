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
    private Long baseCourseId; // ID khoá học gốc của lớp
    private String sourceType; // ADMIN hoặc CLASS_PERSONAL
    private Long classCourseId; // nếu CLASS_PERSONAL: id khoá học của lớp
    private Long chapterId; // chương đã chọn lần trước
    private Long lessonId;  // bài học đã chọn lần trước
    private List<Long> linkedChapterIds; // danh sách chương đã liên kết
    private List<Long> linkedLessonIds;  // danh sách bài đã liên kết

    private List<ChapterResponse> chapters;   // chỉ những chapter/lesson đã link
    private String content;  // nội dung text buổi học (lesson_content)
}
