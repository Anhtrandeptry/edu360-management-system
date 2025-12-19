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

/**
 * REST controller for class enrollment management.
 * Provides endpoints for enrolling students in classes.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/classes/{classId}/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    private boolean isAdmin(UserDetailsImpl user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Lists all enrolled students for a class.
     *
     * @param user    the authenticated user
     * @param classId the class ID
     * @return list of enrolled students
     */
    @GetMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrolledStudentResponse>> list(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId) {
        return ResponseEntity.ok(
                enrollmentService.listStudents(classId, user.getId(), isAdmin(user))
        );
    }

    /**
     * Enrolls a single student in a class.
     *
     * @param user    the authenticated user
     * @param classId the class ID
     * @param req     enrollment request data
     * @return success message
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> enrollOne(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @RequestBody EnrollStudentRequest req) {
        enrollmentService.enrollOne(classId, req, user.getId(), isAdmin(user));
        return ResponseEntity.ok("Enrolled");
    }

    /**
     * Enrolls multiple students in a class.
     *
     * @param user    the authenticated user
     * @param classId the class ID
     * @param req     bulk enrollment request data
     * @return map of student IDs to enrollment status
     */
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

    /**
     * Removes a student from a class.
     *
     * @param user      the authenticated user
     * @param classId   the class ID
     * @param studentId the student ID to remove
     * @return success message
     */
    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN')")
    public ResponseEntity<?> remove(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        enrollmentService.removeOne(classId, studentId, user.getId(), isAdmin(user));
        return ResponseEntity.ok("Removed");
    }

    /**
     * Allows a student to self-enroll in a class.
     *
     * @param user    the authenticated student
     * @param classId the class ID
     * @return success message or error
     */
    @PostMapping("/self")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> selfEnroll(
            @AuthenticationPrincipal UserDetailsImpl user,
            @PathVariable Long classId) {
        try {
            enrollmentService.selfEnroll(classId, user.getId());
            return ResponseEntity.ok("Enrolled");
        } catch (RuntimeException e) {
            String internalMsg = e.getMessage();
            String safeMessage = getSafeEnrollmentErrorMessage(internalMsg);
            
            // Return 402 Payment Required if payment is needed
            if (internalMsg != null && (internalMsg.contains("chưa thanh toán") || internalMsg.contains("payment"))) {
                return ResponseEntity.status(402).body(java.util.Map.of("message", safeMessage));
            }
            
            return ResponseEntity.badRequest().body(java.util.Map.of("message", safeMessage));
        }
    }
    
    /**
     * Maps internal error messages to safe, user-facing messages.
     */
    private String getSafeEnrollmentErrorMessage(String internalMessage) {
        if (internalMessage == null) return "Không thể đăng ký lớp học. Vui lòng thử lại.";
        
        if (internalMessage.contains("chưa thanh toán") || internalMessage.contains("payment")) {
            return "Bạn chưa thanh toán học phí cho lớp này.";
        }
        if (internalMessage.contains("full") || internalMessage.contains("đầy")) {
            return "Lớp học đã đủ số lượng học sinh.";
        }
        if (internalMessage.contains("already enrolled") || internalMessage.contains("đã đăng ký")) {
            return "Bạn đã đăng ký lớp học này rồi.";
        }
        if (internalMessage.contains("PUBLIC") || internalMessage.contains("chưa được mở")) {
            return "Lớp học chưa được mở đăng ký.";
        }
        if (internalMessage.contains("conflict") || internalMessage.contains("trùng lịch")) {
            return "Lịch học bị trùng với lớp khác bạn đã đăng ký.";
        }
        if (internalMessage.contains("not found")) {
            return "Không tìm thấy thông tin. Vui lòng thử lại.";
        }
        
        // Default safe message
        return "Không thể đăng ký lớp học. Vui lòng thử lại.";
    }

}
