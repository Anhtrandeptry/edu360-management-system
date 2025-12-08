package fpt.capstone.edu360managementsystem.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClassRequest {

    @NotBlank
    private String name;
    private String code;

    private Long semesterId;
    @NotNull
    private Long subjectId;


    private Long courseId;

    @NotNull
    private Long teacherId;

    private Long roomId;

    @NotNull
    @Size(min = 1, message = "Select at least one schedule item")
    private List<ScheduleItemRequest> schedule;

    @NotNull
    @Min(1)
    private Integer totalSessions;

    @Min(1)
    private Integer maxStudents;
    private String description;
    private String meetingLink;


    @Min(0)
    private Long pricePerSession;

    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}
