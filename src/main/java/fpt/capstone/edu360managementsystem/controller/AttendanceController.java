package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.AttendanceUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionDetailResponse;
import fpt.capstone.edu360managementsystem.dto.response.AttendanceSessionSummaryResponse;
import fpt.capstone.edu360managementsystem.service.AttendanceService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Buổi dạy hôm nay của giáo viên
     */
    @GetMapping("/today")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> today(@AuthenticationPrincipal UserDetailsImpl user) {
        List<AttendanceSessionSummaryResponse> sessions
                = attendanceService.getTodaySessionsForTeacher(user.getId());
        if (sessions.isEmpty()) {
            return ResponseEntity.ok("Hôm nay không có tiết.");
        }
        return ResponseEntity.ok(sessions);
    }

    /**
     * Chi tiết 1 buổi (danh sách HS, trạng thái)
     */
    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttendanceSessionDetailResponse> sessionDetail(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(
                attendanceService.getSessionDetailForTeacher(user.getId(), sessionId)
        );
    }

    /**
     * Chấm/ cập nhật điểm danh — chỉ trong đúng ngày
     */
    @PostMapping("/session/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsert(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId,
            @Valid @RequestBody AttendanceUpsertRequest body) {
        attendanceService.upsertAttendanceForToday(user.getId(), sessionId, body);
        return ResponseEntity.ok("Đã lưu điểm danh.");
    }

    /**
     * Chấm điểm danh theo classId và date
     */
    @PostMapping("/class/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertByClass(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId,
            @Valid @RequestBody AttendanceUpsertRequest body) {
        attendanceService.upsertAttendanceByClassAndDate(user.getId(), classId, date, slotId, body);
        return ResponseEntity.ok("Đã lưu điểm danh.");
    }

    /**
     * Lấy chi tiết điểm danh theo classId & date (để FE load trạng thái)
     */
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<AttendanceSessionDetailResponse> detailByClass(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId) {
        return ResponseEntity.ok(
                attendanceService.getSessionDetailByClassAndDate(user.getId(), classId, date, slotId)
        );
    }

    /**
     * Admin xem điểm danh theo classId & date (không check ownership)
     */
    @GetMapping("/admin/class/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttendanceSessionDetailResponse> adminViewByClass(
            @PathVariable Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId) {
        return ResponseEntity.ok(
                attendanceService.getSessionDetailByClassAndDateForAdmin(classId, date, slotId)
        );
    }
}
