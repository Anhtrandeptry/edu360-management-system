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

/**
 * REST controller for admin reports and statistics.
 * Provides endpoints for revenue, performance, and overview reports.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportService reportService;

    /**
     * Retrieves overview report with key metrics.
     *
     * @return overview statistics
     */
    @GetMapping("/overview")
    public ResponseEntity<ReportOverviewDTO> getOverview() {
        return ResponseEntity.ok(reportService.getOverview());
    }

    /**
     * Retrieves revenue report by teacher.
     *
     * @return list of teacher revenue data
     */
    @GetMapping("/teacher-revenue")
    public ResponseEntity<List<ReportTeacherRevenueDTO>> getTeacherRevenue() {
        return ResponseEntity.ok(reportService.getTeacherRevenue());
    }

    /**
     * Retrieves the top performing teacher by revenue.
     *
     * @return top teacher data or no content
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
     * Retrieves revenue report by subject.
     *
     * @return list of subject revenue data
     */
    @GetMapping("/subject-revenue")
    public ResponseEntity<List<ReportSubjectRevenueDTO>> getSubjectRevenue() {
        return ResponseEntity.ok(reportService.getSubjectRevenue());
    }

    /**
     * Retrieves daily revenue report.
     *
     * @param days number of days to include (default 30)
     * @return list of daily revenue data
     */
    @GetMapping("/revenue-by-day")
    public ResponseEntity<List<ReportRevenueByTimeDTO>> getRevenueByDay(
            @RequestParam(defaultValue = "30") Integer days) {
        return ResponseEntity.ok(reportService.getRevenueByDay(days));
    }

    /**
     * Retrieves class performance report.
     *
     * @return list of class performance data
     */
    @GetMapping("/class-performance")
    public ResponseEntity<List<ReportClassPerformanceDTO>> getClassPerformance() {
        return ResponseEntity.ok(reportService.getClassPerformance());
    }
}
