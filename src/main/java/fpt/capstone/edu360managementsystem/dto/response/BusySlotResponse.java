package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for busy time slots. Used by free-busy endpoints to return
 * occupied time ranges.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusySlotResponse {

    /**
     * ISO format start time (e.g., "2025-08-15T08:00:00")
     */
    private String start;

    /**
     * ISO format end time (e.g., "2025-08-15T10:00:00")
     */
    private String end;
}
