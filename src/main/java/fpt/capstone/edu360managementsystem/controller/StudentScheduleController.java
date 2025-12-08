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

@RestController
@RequestMapping("/api/my-schedule")
public class StudentScheduleController {

    @Autowired
    private StudentScheduleService studentScheduleService;


    @GetMapping("/test")
    public ResponseEntity<String> test(@AuthenticationPrincipal UserDetailsImpl user) {
        if (user == null) {
            System.out.println(" [STUDENT_SCHEDULE] test endpoint called - NO USER (anonymous)");
            return ResponseEntity.ok("Test endpoint works - No user authenticated");
        }
        System.out.println(" [STUDENT_SCHEDULE] test endpoint called by user: " + user.getUsername() + " (ID: " + user.getId() + ")");
        System.out.println(" [STUDENT_SCHEDULE] User roles: " + user.getAuthorities());
        return ResponseEntity.ok("Test endpoint works - User: " + user.getUsername() + ", Roles: " + user.getAuthorities());
    }


    @GetMapping("/day")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StudentScheduleItemResponse>> getDaySchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        System.out.println(" [STUDENT_SCHEDULE] getDaySchedule called by user: " + user.getUsername() + " (ID: " + user.getId() + ")");
        System.out.println(" [STUDENT_SCHEDULE] User roles: " + user.getAuthorities());
        if (date == null) {
            date = LocalDate.now();
        }
        System.out.println(" [STUDENT_SCHEDULE] Fetching schedule for date: " + date);
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(user.getId(), date);
        System.out.println(" [STUDENT_SCHEDULE] Found " + result.size() + " schedule items");
        return ResponseEntity.ok(result);
    }


    @GetMapping("/week")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StudentScheduleItemResponse>> getWeekSchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "weekStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        System.out.println(" [STUDENT_SCHEDULE] getWeekSchedule called by user: " + user.getUsername() + " (ID: " + user.getId() + ")");
        System.out.println(" [STUDENT_SCHEDULE] User roles: " + user.getAuthorities());
        if (weekStart == null) {
            weekStart = studentScheduleService.getCurrentWeekStart(LocalDate.now());
        }
        System.out.println(" [STUDENT_SCHEDULE] Fetching schedule for week starting: " + weekStart);
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(user.getId(), weekStart);
        System.out.println(" [STUDENT_SCHEDULE] Found " + result.size() + " schedule items for the week");
        return ResponseEntity.ok(result);
    }
}
