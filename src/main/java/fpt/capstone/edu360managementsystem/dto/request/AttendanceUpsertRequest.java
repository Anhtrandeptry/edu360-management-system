package fpt.capstone.edu360managementsystem.dto.request;

import java.util.List;

import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpsertRequest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {

        @NotNull
        private Long studentId;
        @NotNull
        private AttendanceStatus status;
        private String note;
    }

    @NotNull
    private List<Item> items;
}
