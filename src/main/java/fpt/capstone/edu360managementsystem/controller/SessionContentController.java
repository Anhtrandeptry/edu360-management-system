package fpt.capstone.edu360managementsystem.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private static final Logger log = LoggerFactory.getLogger(SessionContentController.class);

    @PostMapping("/by-class-date")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContentByClassDate(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        log.info("➡️ API upsertSessionContentByClassDate userId={}, classId={}, date={}, slotId={}, chapters={}, lessons={}, contentLength={}",
                user.getId(), classId, date, slotId,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]",
                req.getContent() != null ? req.getContent().length() : 0);
        sessionContentService.upsertSessionContentByClassDate(user.getId(), classId, date, slotId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body("Session content saved");
    }

    @PostMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContent(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        log.info("➡️ API upsertSessionContent userId={}, sessionId={}, chapters={}, lessons={}, contentLength={}",
                user.getId(), sessionId,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]",
                req.getContent() != null ? req.getContent().length() : 0);
        sessionContentService.upsertSessionContent(user.getId(), sessionId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body("Session content saved");
    }

    @GetMapping("/content/by-class-date")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
        public ResponseEntity<SessionContentResponse> getSessionContentByClassDate(
                        @RequestParam Long classId,
                        @RequestParam String date,
                        @RequestParam(required = false) Long slotId
    ) {
                return ResponseEntity.ok(sessionContentService.getSessionContentByClassDate(classId, date, slotId));
    }

    @GetMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContent(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContent(sessionId));
    }
}
