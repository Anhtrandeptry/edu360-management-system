package fpt.capstone.edu360managementsystem.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.scheduler.DraftClassReminderScheduler;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin scheduler operations.
 * Provides endpoints to manually trigger scheduled jobs.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class AdminSchedulerController {

    @Autowired
    private DraftClassReminderScheduler draftClassReminderScheduler;

    /**
     * Manually triggers the draft class reminder job.
     * Sends notifications for DRAFT classes approaching their start date.
     *
     * @return success message with job execution status
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
