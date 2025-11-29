package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleItemRequest {

    @NotNull
    @Min(1)
    @Max(7)
    private Integer dayOfWeek;
    @NotNull
    private Long timeSlotId;      // tham chiếu TimeSlot
}
