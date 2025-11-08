package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ScheduleItemRequest {
    @NotNull @Min(1) @Max(7)
    private Integer dayOfWeek;    // 1..7
    @NotNull
    private Long timeSlotId;      // tham chiếu TimeSlot
}
