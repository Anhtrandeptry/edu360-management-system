package fpt.capstone.edu360managementsystem.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.service.ParentNotificationService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;

/**
 * REST controller for parent notification management.
 * Provides endpoints for teachers to send attendance notifications to parents.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/teacher/parent-notification")
public class ParentNotificationController {

    @Autowired
    private ParentNotificationService parentNotificationService;

    /**
     * Sends attendance notification to parents for a specific session.
     *
     * @param user      the authenticated teacher
     * @param sessionId the session ID
     * @return count of notifications sent
     */
    @PostMapping("/send/{sessionId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> sendNotificationBySession(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId) {
        try {
            int sentCount = parentNotificationService.sendParentNotificationManual(sessionId, user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi thông báo thành công cho " + sentCount + " phụ huynh",
                    "data", sentCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Sends attendance notification to parents for a class on a specific date.
     *
     * @param user    the authenticated teacher
     * @param classId the class ID
     * @param date    the date string
     * @param slotId  optional time slot ID
     * @return count of notifications sent
     */
    @PostMapping("/send-by-class")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> sendNotificationByClass(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId) {
        try {
            int sentCount = parentNotificationService.sendParentNotificationByClassAndDate(
                    classId, date, slotId, user.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi thông báo thành công cho " + sentCount + " phụ huynh",
                    "data", sentCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
