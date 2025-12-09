package fpt.capstone.edu360managementsystem.dto.response;

import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CourseResponse {

    private Long id;
    private Long subjectId;
    private String subjectName;
    private String title;
    private String description;
    private CourseStatus status;
    private Long createdByUserId;
    private String createdByName;
    private Long ownerTeacherId;
    private String ownerTeacherName;


    private List<ChapterResponse> chapters;
}
