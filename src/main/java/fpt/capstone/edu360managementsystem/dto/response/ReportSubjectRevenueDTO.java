package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSubjectRevenueDTO {

    private Long subjectId;
    private String subjectName;
    private Long totalRevenue;
    private Integer totalClasses;
    private Integer totalStudents;
}
