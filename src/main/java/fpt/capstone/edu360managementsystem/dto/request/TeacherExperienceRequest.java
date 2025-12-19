package fpt.capstone.edu360managementsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherExperienceRequest {
    private Long id;
    private String position;
    private String company;
    private Integer startYear;
    private Integer endYear;
    private String description;
}
