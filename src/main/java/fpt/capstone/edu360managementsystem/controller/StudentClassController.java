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

@RestController
@RequestMapping("/api/enrollments/me")
public class StudentClassController {

    private static final Logger log = LoggerFactory.getLogger(StudentClassController.class);

    @Autowired
    private StudentClassService studentClassService;


    @GetMapping("/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentClassResponse>> myClasses(
            @AuthenticationPrincipal UserDetailsImpl user
    ) {
        try {
            log.info("[StudentClassController] Fetching enrolled classes for userId={}", user.getId());
            List<StudentClassResponse> result = studentClassService.getMyClasses(user.getId());
            log.info("[StudentClassController] Found {} classes for userId={}", result.size(), user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[StudentClassController] Failed to fetch classes for userId={}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }


    @GetMapping("/schedule/week")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleResponse>> myWeeklySchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        try {
            log.info("[StudentClassController] Fetching weekly schedule for userId={}, weekStart={}", 
                user.getId(), weekStart);
            List<StudentScheduleResponse> result = studentClassService.getMyScheduleByWeek(user.getId(), weekStart);
            log.info("[StudentClassController] Found {} schedule items for userId={}", result.size(), user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[StudentClassController] Failed to fetch schedule for userId={}: {}", 
                user.getId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    

    @GetMapping("/classes/{classId}/sessions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleResponse>> getClassSessions(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId
    ) {
        try {
            log.info("[StudentClassController] Fetching all sessions for classId={}, userId={}", classId, user.getId());
            List<StudentScheduleResponse> result = studentClassService.getClassSessions(user.getId(), classId);
            log.info("[StudentClassController] Found {} sessions for classId={}", result.size(), classId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[StudentClassController] Failed to fetch sessions for classId={}: {}", 
                classId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
