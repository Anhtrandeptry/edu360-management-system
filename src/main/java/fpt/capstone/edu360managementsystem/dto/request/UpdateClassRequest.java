package fpt.capstone.edu360managementsystem.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateClassRequest {


    private String name;
    private String description;
    private String meetingLink;
    private LocalDate startDate;
    private LocalDate endDate;

    private Long roomId;

    @Min(1)
    private Integer maxStudents;


    private Long pricePerSession;


    private Long subjectId;
    private Long courseId;
    private Long teacherId;
    private Integer totalSessions;
    private List<ScheduleItemRequest> schedule;


    private Boolean forceDeleteContentAndCourse;
}
