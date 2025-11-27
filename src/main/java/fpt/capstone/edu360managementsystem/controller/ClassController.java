package fpt.capstone.edu360managementsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.service.ClassService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(classService.createClass(request));
    }

    @GetMapping
    // Cho phép tất cả user xem danh sách lớp (bao gồm guest)
    public ResponseEntity<java.util.List<ClassResponse>> list(
            @RequestParam(name = "teacherUserId", required = false) Long teacherUserId,
            @RequestParam(name = "timeSlotId", required = false) Long timeSlotId
    ) {
        return ResponseEntity.ok(classService.listClasses(teacherUserId, timeSlotId));
    }

    // Publish class: DRAFT -> PUBLIC
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishClass(@PathVariable Long id) {
        System.out.println("\uD83D\uDD14 [ClassController] Publish request for classId=" + id);
        try {
            classService.publishClass(id);
            System.out.println("✅ [ClassController] Publish completed for classId=" + id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            System.out.println("❌ [ClassController] Publish failed: " + ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            System.out.println("❌ [ClassController] Publish error: " + ex.getMessage());
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    // Revert PUBLIC -> DRAFT if no past sessions occurred
    @PostMapping("/{id}/revert-draft")
    public ResponseEntity<?> revertToDraft(@PathVariable Long id) {
        System.out.println("\uD83D\uDD14 [ClassController] Revert-to-draft request for classId=" + id);
        try {
            classService.revertToDraft(id);
            System.out.println("✅ [ClassController] Revert-to-draft completed for classId=" + id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            System.out.println("❌ [ClassController] Revert-to-draft blocked: " + ex.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            System.out.println("❌ [ClassController] Revert-to-draft error: " + ex.getMessage());
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    // (mở rộng sau) GET danh sách lớp, GET chi tiết lớp, GET sessions, v.v.
}
