package fpt.capstone.edu360managementsystem.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.service.ParentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;

/**
 * REST controller for parent portal features. Provides endpoints for parents to
 * view their children's information.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

    @Autowired
    private ParentService parentService;

    /**
     * Get parent dashboard data with statistics
     *
     * @param user the authenticated parent
     * @return dashboard data including stats
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@AuthenticationPrincipal UserDetailsImpl user) {
        try {
            Map<String, Object> dashboard = parentService.getDashboardData(user.getId());
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy dữ liệu dashboard: " + e.getMessage()
            ));
        }
    }

    /**
     * Get list of children for the parent
     *
     * @param user the authenticated parent
     * @return list of children
     */
    @GetMapping("/children")
    public ResponseEntity<?> getChildren(@AuthenticationPrincipal UserDetailsImpl user) {
        try {
            return ResponseEntity.ok(parentService.getChildren(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách con: " + e.getMessage()
            ));
        }
    }

    /**
     * Get attendance records for a specific child
     *
     * @param user the authenticated parent
     * @param childId the student ID
     * @param month the month (1-12)
     * @param year the year
     * @return attendance data with statistics
     */
    @GetMapping("/children/{childId}/attendance")
    public ResponseEntity<?> getChildAttendance(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long childId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try {
            Map<String, Object> attendance = parentService.getChildAttendance(user.getId(), childId, month, year);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy điểm danh: " + e.getMessage()
            ));
        }
    }

    /**
     * Get schedule for a specific child
     *
     * @param user the authenticated parent
     * @param childId the student ID
     * @param startDate start date (YYYY-MM-DD)
     * @param endDate end date (YYYY-MM-DD)
     * @return list of schedule items
     */
    @GetMapping("/children/{childId}/schedule")
    public ResponseEntity<?> getChildSchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long childId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            return ResponseEntity.ok(parentService.getChildSchedule(user.getId(), childId, startDate, endDate));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy lịch học: " + e.getMessage()
            ));
        }
    }

    /**
     * Get classes for a specific child
     *
     * @param user the authenticated parent
     * @param childId the student ID
     * @return list of classes
     */
    @GetMapping("/children/{childId}/classes")
    public ResponseEntity<?> getChildClasses(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long childId) {
        try {
            return ResponseEntity.ok(parentService.getChildClasses(user.getId(), childId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy danh sách lớp: " + e.getMessage()
            ));
        }
    }

    /**
     * Get detailed information for a specific class
     *
     * @param user the authenticated parent
     * @param classId the class ID
     * @return class details with sessions and materials
     */
    @GetMapping("/classes/{classId}")
    public ResponseEntity<?> getClassDetail(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId) {
        try {
            return ResponseEntity.ok(parentService.getClassDetail(user.getId(), classId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết lớp: " + e.getMessage()
            ));
        }
    }

    /**
     * Get session detail with attendance for parent's child
     *
     * @param user the authenticated parent
     * @param sessionId the session ID
     * @return session details with attendance
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<?> getSessionDetail(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId) {
        try {
            return ResponseEntity.ok(parentService.getSessionDetail(user.getId(), sessionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy chi tiết buổi học: " + e.getMessage()
            ));
        }
    }

    /**
     * Get notifications for parent
     *
     * @param user the authenticated parent
     * @param filter filter by read/unread status
     * @param childId optional filter by child
     * @return list of notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) Long childId) {
        try {
            return ResponseEntity.ok(parentService.getNotifications(user.getId(), filter, childId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thông báo: " + e.getMessage()
            ));
        }
    }

    /**
     * Mark a notification as read
     *
     * @param user the authenticated parent
     * @param notificationId the notification ID
     * @return success response
     */
    @PutMapping("/notifications/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long notificationId) {
        try {
            parentService.markNotificationAsRead(user.getId(), notificationId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi đánh dấu đã đọc: " + e.getMessage()
            ));
        }
    }

    /**
     * Mark all notifications as read
     *
     * @param user the authenticated parent
     * @return success response
     */
    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead(@AuthenticationPrincipal UserDetailsImpl user) {
        try {
            parentService.markAllNotificationsAsRead(user.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi đánh dấu tất cả đã đọc: " + e.getMessage()
            ));
        }
    }

    /**
     * Get payment history for parent
     *
     * @param user the authenticated parent
     * @param childId optional filter by child
     * @param status optional filter by payment status
     * @return payment history with statistics
     */
    @GetMapping("/payment-history")
    public ResponseEntity<?> getPaymentHistory(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(required = false) Long childId,
            @RequestParam(required = false) String status) {
        try {
            Map<String, Object> payments = parentService.getPaymentHistory(user.getId(), childId, status);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy lịch sử thanh toán: " + e.getMessage()
            ));
        }
    }

    /**
     * Get parent profile information
     *
     * @param user the authenticated parent
     * @return parent profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetailsImpl user) {
        try {
            return ResponseEntity.ok(parentService.getProfile(user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi lấy thông tin: " + e.getMessage()
            ));
        }
    }

    /**
     * Update parent profile information
     *
     * @param user the authenticated parent
     * @param profile the updated profile data
     * @return updated profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestBody Map<String, Object> profile) {
        try {
            return ResponseEntity.ok(parentService.updateProfile(user.getId(), profile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Lỗi khi cập nhật thông tin: " + e.getMessage()
            ));
        }
    }
}
