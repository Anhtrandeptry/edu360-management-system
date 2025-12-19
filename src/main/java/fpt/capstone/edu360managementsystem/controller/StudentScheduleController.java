package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleItemResponse;
import fpt.capstone.edu360managementsystem.service.StudentScheduleService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for student schedule management.
 * Provides endpoints for students to view their class schedules.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/my-schedule")
public class StudentScheduleController {

    @Autowired
    private StudentScheduleService studentScheduleService;

    /**
     * Test endpoint to verify authentication.
     *
     * @param user the authenticated user
     * @return test message with user info
     */
    @GetMapping("/test")
    public ResponseEntity<String> test(@AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.ok("Test endpoint works - No user authenticated");
        }
        return ResponseEntity.ok("Test endpoint works - User: " + user.getUsername() + ", Roles: " + user.getAuthorities());
    }

    /**
     * Retrieves the student's schedule for a specific day.
     *
     * @param user the authenticated student
     * @param date the date to get schedule for (defaults to today)
     * @return list of schedule items for the day
     */
    @GetMapping("/day")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StudentScheduleItemResponse>> getDaySchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(user.getId(), date);
        return ResponseEntity.ok(result);
    }

    /**
     * Retrieves the student's schedule for a week.
     *
     * @param user      the authenticated student
     * @param weekStart the start date of the week (defaults to current week)
     * @return list of schedule items for the week
     */
    @GetMapping("/week")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StudentScheduleItemResponse>> getWeekSchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "weekStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        if (weekStart == null) {
            weekStart = studentScheduleService.getCurrentWeekStart(LocalDate.now());
        }
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(user.getId(), weekStart);
        return ResponseEntity.ok(result);
    }
}
