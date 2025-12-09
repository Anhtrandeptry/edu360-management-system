package fpt.capstone.edu360managementsystem.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.scheduler.DraftClassReminderScheduler;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class AdminSchedulerController {

    private final DraftClassReminderScheduler draftClassReminderScheduler;

    /**
     * Chạy thủ công job nhắc nhở lớp DRAFT sắp đến ngày bắt đầu
     */
    @PostMapping("/draft-class-reminder/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runDraftClassReminder() {
        draftClassReminderScheduler.runManually();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Draft class reminder job executed successfully. Check notifications."
        ));
    }
}
