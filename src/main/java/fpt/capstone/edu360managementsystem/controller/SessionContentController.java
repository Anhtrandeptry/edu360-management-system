package fpt.capstone.edu360managementsystem.controller;

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

import fpt.capstone.edu360managementsystem.dto.request.SessionContentUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.SessionContentResponse;
import fpt.capstone.edu360managementsystem.service.SessionContentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
public class SessionContentController {

    @Autowired
    private SessionContentService sessionContentService;

    @PostMapping("/by-class-date")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContentByClassDate(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long classId,
            @RequestParam String date,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        sessionContentService.upsertSessionContentByClassDate(user.getId(), classId, date, req);
        return ResponseEntity.ok("Updated session content");
    }

    @PostMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContent(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        sessionContentService.upsertSessionContent(user.getId(), sessionId, req);
        return ResponseEntity.ok("Updated session content");
    }

    @GetMapping("/content/by-class-date")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContentByClassDate(
            @RequestParam Long classId,
            @RequestParam String date
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContentByClassDate(classId, date));
    }

    @GetMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContent(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContent(sessionId));
    }
}
