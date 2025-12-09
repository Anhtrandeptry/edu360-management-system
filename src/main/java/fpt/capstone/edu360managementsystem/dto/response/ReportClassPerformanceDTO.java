package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportClassPerformanceDTO {

    private Long classId;
    private String className;
    private String teacherName;
    private String subjectName;
    private Integer maxStudents;
    private Integer enrolledStudents;
    private Integer paidStudents;
    private Double fillRate;         // Tỷ lệ lấp đầy (%)
    private Long totalRevenue;
    private Long pendingRevenue;
    private Boolean isOnline;
}
