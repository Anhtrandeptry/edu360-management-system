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

/**
 * REST controller for session content management.
 * Provides endpoints for managing lesson content within class sessions.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionContentController {

    @Autowired
    private SessionContentService sessionContentService;
    private static final Logger log = LoggerFactory.getLogger(SessionContentController.class);

    /**
     * Creates or updates session content by class and date.
     *
     * @param user    the authenticated teacher
     * @param classId the class ID
     * @param date    the session date
     * @param slotId  optional time slot ID
     * @param req     the session content data
     * @return success message
     */
    @PostMapping("/by-class-date")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContentByClassDate(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        log.info("API upsertSessionContentByClassDate userId={}, classId={}, date={}, slotId={}, chapters={}, lessons={}, contentLength={}",
                user.getId(), classId, date, slotId,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]",
                req.getContent() != null ? req.getContent().length() : 0);
        sessionContentService.upsertSessionContentByClassDate(user.getId(), classId, date, slotId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body("Session content saved");
    }

    /**
     * Creates or updates session content by session ID.
     *
     * @param user      the authenticated teacher
     * @param sessionId the session ID
     * @param req       the session content data
     * @return success message
     */
    @PostMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> upsertSessionContent(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionContentUpsertRequest req
    ) {
        log.info("API upsertSessionContent userId={}, sessionId={}, chapters={}, lessons={}, contentLength={}",
                user.getId(), sessionId,
                req.getChapterIds() != null ? req.getChapterIds() : "[]",
                req.getLessonIds() != null ? req.getLessonIds() : "[]",
                req.getContent() != null ? req.getContent().length() : 0);
        sessionContentService.upsertSessionContent(user.getId(), sessionId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body("Session content saved");
    }

    /**
     * Retrieves session content by class and date.
     *
     * @param classId the class ID
     * @param date    the session date
     * @param slotId  optional time slot ID
     * @return session content details
     */
    @GetMapping("/content/by-class-date")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContentByClassDate(
            @RequestParam Long classId,
            @RequestParam String date,
            @RequestParam(required = false) Long slotId
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContentByClassDate(classId, date, slotId));
    }

    /**
     * Retrieves session content by session ID.
     *
     * @param sessionId the session ID
     * @return session content details
     */
    @GetMapping("/{sessionId}/content")
    @PreAuthorize("hasRole('TEACHER') or hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<SessionContentResponse> getSessionContent(
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok(sessionContentService.getSessionContent(sessionId));
    }
}
