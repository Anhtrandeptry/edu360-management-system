package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleResponse;
import fpt.capstone.edu360managementsystem.service.StudentClassService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for student class and schedule management.
 * Provides endpoints for students to view their enrolled classes and schedules.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/enrollments/me")
public class StudentClassController {

    private static final Logger log = LoggerFactory.getLogger(StudentClassController.class);

    @Autowired
    private StudentClassService studentClassService;

    /**
     * Retrieves all classes the student is enrolled in.
     *
     * @param user the authenticated student
     * @return list of enrolled classes
     */
    @GetMapping("/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentClassResponse>> myClasses(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        try {
            log.info("Fetching enrolled classes for userId={}", user.getId());
            List<StudentClassResponse> result = studentClassService.getMyClasses(user.getId());
            log.info("Found {} classes for userId={}", result.size(), user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch classes for userId={}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves weekly schedule for the student.
     *
     * @param user      the authenticated student
     * @param weekStart the start date of the week
     * @return list of schedule items for the week
     */
    @GetMapping("/schedule/week")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleResponse>> myWeeklySchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        try {
            log.info("Fetching weekly schedule for userId={}, weekStart={}", user.getId(), weekStart);
            List<StudentScheduleResponse> result = studentClassService.getMyScheduleByWeek(user.getId(), weekStart);
            log.info("Found {} schedule items for userId={}", result.size(), user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch schedule for userId={}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retrieves all sessions for a specific class.
     *
     * @param user    the authenticated student
     * @param classId the class ID
     * @return list of class sessions
     */
    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleResponse>> getClassSessions(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId
    ) {
        try {
            log.info("Fetching all sessions for classId={}, userId={}", classId, user.getId());
            List<StudentScheduleResponse> result = studentClassService.getClassSessions(user.getId(), classId);
            log.info("Found {} sessions for classId={}", result.size(), classId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch sessions for classId={}: {}", classId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
