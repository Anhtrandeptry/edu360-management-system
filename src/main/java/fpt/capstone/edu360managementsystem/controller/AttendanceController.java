package fpt.capstone.edu360managementsystem.controller;

import java.util.List;
import java.util.Map;

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

/**
 * REST controller for attendance management. Provides endpoints for teachers to
 * take and manage student attendance.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * Retrieves today's sessions for the authenticated teacher.
     *
     * @param user the authenticated teacher
     * @return list of today's sessions or message if none
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
     * Retrieves attendance details for a specific session.
     *
     * @param user the authenticated teacher
     * @param sessionId the session ID
     * @return session attendance details
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
     * Creates or updates attendance records for a session.
     *
     * @param user the authenticated teacher
     * @param sessionId the session ID
     * @param body attendance data
     * @return success message
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
     * Creates or updates attendance by class and date.
     *
     * @param user the authenticated teacher
     * @param classId the class ID
     * @param date the date string
     * @param slotId optional time slot ID
     * @param body attendance data
     * @return success message
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
     * Retrieves attendance details by class and date.
     *
     * @param user the authenticated teacher
     * @param classId the class ID
     * @param date the date string
     * @param slotId optional time slot ID
     * @return session attendance details
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
     * Admin endpoint to view attendance by class and date.
     *
     * @param classId the class ID
     * @param date the date string
     * @param slotId optional time slot ID
     * @return session attendance details
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

    /**
     * Check attendance status for multiple sessions (by class ID, date, and
     * slot). Returns a map of "classId-date-slotId" -> boolean (true if has
     * attendance)
     *
     * @param sessions list of session identifiers in format
     * "classId-date-slotId"
     * @return map of session identifier to attendance status
     */
    @PostMapping("/check-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<Map<String, Boolean>> checkAttendanceStatus(
            @RequestBody List<String> sessions) {
        return ResponseEntity.ok(attendanceService.checkAttendanceStatus(sessions));
    }
}
