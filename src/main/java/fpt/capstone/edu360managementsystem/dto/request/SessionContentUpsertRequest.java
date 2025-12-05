package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SessionContentUpsertRequest {

    private List<Long> chapterIds;
    private List<Long> lessonIds;
    private String content;  // Nội dung text buổi học

    // Nghiệp vụ mới: lưu nguồn và tham chiếu khóa học lớp
    private String sourceType; // ADMIN | CLASS_PERSONAL
    private Long classCourseId; // id course của lớp (nếu CLASS_PERSONAL)

    // Lưu chọn đơn để FE hydrate nhanh
    private Long chapterId;
    private Long lessonId;

    // XÓA toàn bộ logic cũ: PERSONAL/mapping
}
