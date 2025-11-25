package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.SessionContentUpsertRequest;
import fpt.capstone.edu360managementsystem.dto.response.SessionContentResponse;
import fpt.capstone.edu360managementsystem.service.SessionContentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
public class SessionContentController {

    @Autowired
    private SessionContentService sessionContentService;

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

    @GetMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContent(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContent(sessionId));
    }
}
