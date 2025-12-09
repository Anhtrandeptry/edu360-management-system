package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportTeacherRevenueDTO {

    private Long teacherId;
    private Long teacherUserId;
    private String teacherName;
    private String teacherAvatar;
    private String teacherEmail;
    private Long totalRevenue;       // Tổng doanh thu (đã thanh toán)
    private Long pendingRevenue;     // Doanh thu chờ thanh toán
    private Integer totalClasses;    // Số lớp đang dạy
    private Integer totalStudents;   // Tổng số học sinh
    private Integer paidStudents;    // Số học sinh đã thanh toán
}
