package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleItemRequest {

    @NotNull
    @Min(0)
    @Max(6)
    private Integer dayOfWeek;    // 0..6
    @NotNull
    private Long timeSlotId;      // tham chiếu TimeSlot
}
