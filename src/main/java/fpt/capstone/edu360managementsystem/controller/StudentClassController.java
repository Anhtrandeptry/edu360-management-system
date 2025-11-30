package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.StudentClassResponse;
import fpt.capstone.edu360managementsystem.service.StudentClassService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments/me")
public class StudentClassController {

    private static final Logger log = LoggerFactory.getLogger(StudentClassController.class);

    @Autowired
    private StudentClassService studentClassService;

    /**
     * Danh sách lớp học mà học sinh (đang đăng nhập) đã đăng ký
     */
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
}
