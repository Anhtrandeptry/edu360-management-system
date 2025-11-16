package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollStudentRequest {
    @NotNull
    private Long studentId;
}
