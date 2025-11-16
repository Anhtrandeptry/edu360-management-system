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

    /**
     * Lịch theo NGÀY.
     * Nếu không truyền ?date= thì mặc định là hôm nay.
     * Ví dụ:
     * GET /api/my-schedule/day
     * GET /api/my-schedule/day?date=2025-11-20
     */
    @GetMapping("/day")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleItemResponse>> getDaySchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (date == null) {
            date = LocalDate.now();
        }
        return ResponseEntity.ok(
                studentScheduleService.getScheduleByDate(user.getId(), date)
        );
    }

    /**
     * Lịch theo TUẦN.
     * Nếu không truyền ?weekStart= thì mặc định lấy tuần hiện tại (bắt đầu từ Monday).
     * Ví dụ:
     * GET /api/my-schedule/week
     * GET /api/my-schedule/week?weekStart=2025-11-17
     */
    @GetMapping("/week")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentScheduleItemResponse>> getWeekSchedule(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam(value = "weekStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        if (weekStart == null) {
            weekStart = studentScheduleService.getCurrentWeekStart(LocalDate.now());
        }
        return ResponseEntity.ok(
                studentScheduleService.getScheduleByWeek(user.getId(), weekStart)
        );
    }
}
