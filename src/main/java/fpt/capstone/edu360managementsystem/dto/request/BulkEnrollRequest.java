package fpt.capstone.edu360managementsystem.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkEnrollRequest {
    @NotEmpty
    private List<Long> studentIds;
}
