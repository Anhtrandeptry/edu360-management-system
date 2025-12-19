package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeacherCourseVersionRequest {

    @NotNull
    private Long baseCourseId;

    @NotNull
    private Long teacherCourseId;
}
