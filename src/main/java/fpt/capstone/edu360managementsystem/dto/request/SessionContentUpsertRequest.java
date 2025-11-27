package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import lombok.Data;

@Data
public class SessionContentUpsertRequest {

    private List<Long> chapterIds;
    private List<Long> lessonIds;
    private String content;  // Nội dung text buổi học
}
