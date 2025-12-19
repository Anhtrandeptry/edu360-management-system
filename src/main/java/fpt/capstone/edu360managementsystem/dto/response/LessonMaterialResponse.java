package fpt.capstone.edu360managementsystem.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonMaterialResponse {
    private Long id;
    private Long lessonId;
    private String lessonTitle;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String description;
    private LocalDateTime uploadedAt;
    private String uploadedByName;
}
