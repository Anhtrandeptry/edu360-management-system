package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.TeacherClassAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherListForAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherWorkSummaryResponse;
import fpt.capstone.edu360managementsystem.service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher-attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;

    /**
     * Lấy danh sách tất cả giáo viên với thống kê chấm công
     * Dành cho Admin quản lý
     */
    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeacherListForAttendanceResponse>> getAllTeachers() {
        return ResponseEntity.ok(teacherAttendanceService.getAllTeachersForAttendance());
    }

    /**
     * Lấy thống kê chi tiết chấm công của một giáo viên
     * @param teacherId ID của giáo viên
     * @param month Tháng (optional, mặc định tháng hiện tại)
     * @param year Năm (optional, mặc định năm hiện tại)
     */
    @GetMapping("/teachers/{teacherId}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherWorkSummaryResponse> getTeacherSummary(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(teacherAttendanceService.getTeacherWorkSummary(teacherId, month, year));
    }

    /**
     * Lấy chi tiết chấm công theo lớp của giáo viên
     * @param teacherId ID của giáo viên
     * @param classId ID của lớp
     */
    @GetMapping("/teachers/{teacherId}/classes/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherClassAttendanceResponse> getTeacherClassAttendance(
            @PathVariable Long teacherId,
            @PathVariable Long classId
    ) {
        return ResponseEntity.ok(teacherAttendanceService.getTeacherClassAttendance(teacherId, classId));
    }
}
