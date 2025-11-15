package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.AttendanceUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionDetailResponse;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionSummaryResponse;
import fpt.capstone.edu360managementsystem.service.AttendanceService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /** Buổi dạy hôm nay của giáo viên */
    @GetMapping("/today")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> today(@AuthenticationPrincipal UserDetailsImpl user) {
        List<AttendanceSessionSummaryResponse> sessions =
                attendanceService.getTodaySessionsForTeacher(user.getId());
        if (sessions.isEmpty()) {
            return ResponseEntity.ok("Hôm nay không có tiết.");
        }
        return ResponseEntity.ok(sessions);
    }

    /** Chi tiết 1 buổi (danh sách HS, trạng thái) */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttendanceSessionDetailResponse> sessionDetail(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(
                attendanceService.getSessionDetailForTeacher(user.getId(), sessionId)
        );
    }

    /** Chấm/ cập nhật điểm danh — chỉ trong đúng ngày */
    @PostMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsert(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceUpsertRequest body) {
        attendanceService.upsertAttendanceForToday(user.getId(), sessionId, body);
        return ResponseEntity.ok("Đã lưu điểm danh.");
    }
}
