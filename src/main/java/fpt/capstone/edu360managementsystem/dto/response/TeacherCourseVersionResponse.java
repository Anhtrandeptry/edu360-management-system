package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherCourseVersionResponse {

    private Long id;
    private Long baseCourseId;
    private Long teacherCourseId;
    private Long teacherId;
    private String teacherCourseTitle;
}
