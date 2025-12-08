package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.BulkEnrollRequest;
import fpt.capstone.edu360managementsystem.dto.request.EnrollStudentRequest;
import fpt.capstone.edu360managementsystem.dto.response.EnrolledStudentResponse;
import fpt.capstone.edu360managementsystem.service.EnrollmentService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes/{classId}/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }


    @GetMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrolledStudentResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId) {
        return ResponseEntity.ok(
                enrollmentService.listStudents(classId, user.getId(), isAdmin(user))
        );
    }


    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> enrollOne(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @RequestBody EnrollStudentRequest req) {
        enrollmentService.enrollOne(classId, req, user.getId(), isAdmin(user));
        return ResponseEntity.ok("Enrolled");
    }


    @PostMapping("/bulk")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<Map<Long, String>> enrollBulk(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @RequestBody BulkEnrollRequest req) {
        return ResponseEntity.ok(
                enrollmentService.enrollBulk(classId, req, user.getId(), isAdmin(user))
        );
    }


    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> remove(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        enrollmentService.removeOne(classId, studentId, user.getId(), isAdmin(user));
        return ResponseEntity.ok("Removed");
    }


    @PostMapping("/self")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> selfEnroll(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId) {
        try {
            enrollmentService.selfEnroll(classId, user.getId());
            return ResponseEntity.ok("Enrolled");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

}
