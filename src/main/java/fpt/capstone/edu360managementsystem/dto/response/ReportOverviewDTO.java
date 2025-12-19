package fpt.capstone.edu360managementsystem.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportOverviewDTO {

    // Tổng quan doanh thu
    private Long totalRevenue;           // Tổng doanh thu (đã thanh toán)
    private Long pendingRevenue;         // Doanh thu chờ thanh toán
    private Long monthlyRevenue;         // Doanh thu tháng này
    private Long weeklyRevenue;          // Doanh thu tuần này
    private Long todayRevenue;           // Doanh thu hôm nay

    // So sánh với tháng trước
    private Long lastMonthRevenue;       // Doanh thu tháng trước
    private Double monthGrowthPercent;   // % tăng trưởng so với tháng trước

    // Thống kê học sinh
    private Long totalStudents;          // Tổng học sinh
    private Long newStudentsThisMonth;   // Học sinh mới tháng này
    private Long activeEnrollments;      // Số đăng ký đang active

    // Thống kê lớp học
    private Long totalClasses;           // Tổng số lớp
    private Long publicClasses;          // Lớp đang PUBLIC
    private Long draftClasses;           // Lớp DRAFT

    // Thống kê giáo viên
    private Long totalTeachers;          // Tổng số giáo viên
    private Long activeTeachers;         // GV có lớp đang dạy

    // Thống kê thanh toán
    private Long paidPayments;           // Số thanh toán thành công
    private Long pendingPayments;        // Số thanh toán chờ
    private Double paymentSuccessRate;   // Tỷ lệ thanh toán thành công
}
