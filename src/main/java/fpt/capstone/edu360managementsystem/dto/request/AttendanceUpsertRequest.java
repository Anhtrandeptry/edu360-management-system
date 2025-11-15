package fpt.capstone.edu360managementsystem.dto.request;

import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AttendanceUpsertRequest {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Item {
        @NotNull private Long studentId;
        @NotNull private AttendanceStatus status; // UNMARKED/PRESENT/ABSENT
    }

    @NotNull
    private List<Item> items;
}
