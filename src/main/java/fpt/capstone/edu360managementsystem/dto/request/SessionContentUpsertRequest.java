package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SessionContentUpsertRequest {

    private List<Long> chapterIds;
    private List<Long> lessonIds;
    private String content;  // Nội dung text buổi học

    // Các trường cấu hình nguồn và khoá học để persist tường minh
    private String sourceType; // ADMIN | PERSONAL
    private Long courseId; // nếu ADMIN (base course id)
    private Long teacherCourseId; // nếu PERSONAL
    private Long chapterId; // chương được chọn (có thể trùng chapterIds[0])
    private Long lessonId;  // bài được chọn (có thể trùng lessonIds[0])
}
