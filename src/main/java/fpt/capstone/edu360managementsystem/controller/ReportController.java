package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.response.ReportClassPerformanceDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportOverviewDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportRevenueByTimeDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportSubjectRevenueDTO;
import fpt.capstone.edu360managementsystem.dto.response.ReportTeacherRevenueDTO;
import fpt.capstone.edu360managementsystem.service.ReportService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * Báo cáo tổng quan
     */
    @GetMapping("/overview")
    public ResponseEntity<ReportOverviewDTO> getOverview() {
        return ResponseEntity.ok(reportService.getOverview());
    }

    /**
     * Doanh thu theo giáo viên
     */
    @GetMapping("/teacher-revenue")
    public ResponseEntity<List<ReportTeacherRevenueDTO>> getTeacherRevenue() {
        return ResponseEntity.ok(reportService.getTeacherRevenue());
    }

    /**
     * Top giáo viên doanh thu cao nhất
     */
    @GetMapping("/top-teacher")
    public ResponseEntity<ReportTeacherRevenueDTO> getTopTeacher() {
        ReportTeacherRevenueDTO top = reportService.getTopTeacher();
        if (top == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(top);
    }

    /**
     * Doanh thu theo môn học
     */
    @GetMapping("/subject-revenue")
    public ResponseEntity<List<ReportSubjectRevenueDTO>> getSubjectRevenue() {
        return ResponseEntity.ok(reportService.getSubjectRevenue());
    }

    /**
     * Doanh thu theo ngày (mặc định 30 ngày)
     */
    @GetMapping("/revenue-by-day")
    public ResponseEntity<List<ReportRevenueByTimeDTO>> getRevenueByDay(
            @RequestParam(defaultValue = "30") Integer days) {
        return ResponseEntity.ok(reportService.getRevenueByDay(days));
    }

    /**
     * Hiệu suất lớp học
     */
    @GetMapping("/class-performance")
    public ResponseEntity<List<ReportClassPerformanceDTO>> getClassPerformance() {
        return ResponseEntity.ok(reportService.getClassPerformance());
    }
}
