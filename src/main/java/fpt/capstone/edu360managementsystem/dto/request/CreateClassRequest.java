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
    private String code; // optional

    private Long semesterId; // optional - tự tính theo startDate/endDate
    @NotNull
    private Long subjectId;

    // Cho phép tùy chọn: không chọn cũng không sao (Offline/Online)
    private Long courseId; // optional: course thuộc subject (nullable)

    @NotNull
    private Long teacherId;

    private Long roomId; // nullable for online classes

    @NotNull
    @Size(min = 1, message = "Select at least one schedule item")
    private List<ScheduleItemRequest> schedule;

    @NotNull
    @Min(1)
    private Integer totalSessions;

    @Min(1)
    private Integer maxStudents; // nếu null => lấy room.capacity
    private String description;
    private String meetingLink; // optional, for online classes

    @NotNull
    @Min(0)
    private Long pricePerSession;

    @NotNull
    private LocalDate startDate; // required
    @NotNull
    private LocalDate endDate; // required //Fix to fit with service
}
