package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateClassRequest {
    @NotBlank private String name;
    private String code; // optional

    @NotNull private Long semesterId;
    @NotNull private Long subjectId;
    @NotNull private Long teacherId;
    @NotNull private Long roomId;

    @NotNull @Size(min=1, message="Select at least one schedule item")
    private List<ScheduleItemRequest> schedule;

    @NotNull @Min(1)
    private Integer totalSessions;

    @Min(1) private Integer maxStudents; // nếu null => lấy room.capacity
    private String description;
}
