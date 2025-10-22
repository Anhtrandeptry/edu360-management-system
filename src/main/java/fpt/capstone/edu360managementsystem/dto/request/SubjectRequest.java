package fpt.capstone.edu360managementsystem.dto.request;

import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubjectRequest {
    @NotBlank(message = "Subject name is required")
    private String name;

    private SubjectStatus status = SubjectStatus.AVAILABLE;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SubjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubjectStatus status) {
        this.status = status;
    }
}
