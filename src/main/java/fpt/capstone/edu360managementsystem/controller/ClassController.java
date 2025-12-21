package fpt.capstone.edu360managementsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.request.UpdateClassRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassPublicDetailResponse;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.service.ClassService;
import jakarta.validation.Valid;

/**
 * REST controller for class management. Provides endpoints for CRUD operations
 * on classes including publish/draft workflows.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    /**
     * Creates a new class.
     *
     * @param request the class creation data
     * @return created class response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody CreateClassRequest request) {
        try {
            ClassResponse resp = classService.createClass(request);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "message", "Dữ liệu không hợp lệ",
                    "detail", ex.getMessage()
            ));
        }
    }

    /**
     * Lists all classes with optional filters.
     *
     * @param teacherUserId optional teacher filter
     * @param timeSlotId optional time slot filter
     * @return list of classes
     */
    @GetMapping
    public ResponseEntity<java.util.List<ClassResponse>> list(
            @RequestParam(name = "teacherUserId", required = false) Long teacherUserId,
            @RequestParam(name = "timeSlotId", required = false) Long timeSlotId
    ) {
        return ResponseEntity.ok(classService.listClasses(teacherUserId, timeSlotId));
    }

    /**
     * Retrieves paginated classes with filters and sorting.
     *
     * @param search optional search term
     * @param status status filter
     * @param online online/offline filter
     * @param teacherUserId optional teacher filter
     * @param subjectId optional subject filter
     * @param page page number
     * @param size page size
     * @param sortBy sort field
     * @param order sort order
     * @param excludeHidden exclude hidden classes (for public listing)
     * @return paginated class list
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<ClassResponse>> getClassesPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String online,
            @RequestParam(required = false) Long teacherUserId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) Boolean excludeHidden
    ) {
        return ResponseEntity.ok(classService.getClassesWithPagination(
                search, status, online, teacherUserId, subjectId, minPrice, maxPrice, page, size, sortBy, order, excludeHidden
        ));
    }

    /**
     * Retrieves class details by ID.
     *
     * @param id the class ID
     * @return class details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClassResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getClassById(id));
    }

    /**
     * Retrieves public class details for guest/student view.
     *
     * @param id the class ID
     * @return public class details
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<ClassPublicDetailResponse> getPublicDetail(@PathVariable Long id) {
        return ResponseEntity.ok(classService.getClassPublicDetail(id));
    }

    /**
     * Publishes a draft class making it available for enrollment.
     *
     * @param id the class ID
     * @return success or error response
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishClass(@PathVariable Long id) {
        try {
            classService.publishClass(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    /**
     * Reverts a published class back to draft status.
     *
     * @param id the class ID
     * @return success or error response
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/revert-draft")
    public ResponseEntity<?> revertToDraft(@PathVariable Long id) {
        try {
            classService.revertToDraft(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    /**
     * Updates an existing class.
     *
     * @param id the class ID
     * @param req the update data
     * @return updated class response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody UpdateClassRequest req) {
        try {
            ClassResponse resp = classService.updateClass(id, req);
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    /**
     * Deletes a DRAFT class permanently. Only classes with status DRAFT can be
     * deleted. All related data (schedules, sessions, enrollments) will be
     * removed.
     *
     * @param id the class ID
     * @return success or error response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteClass(@PathVariable Long id) {
        try {
            classService.deleteClass(id);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }

    /**
     * Retrieves DRAFT classes approaching their start date (within 3 days).
     * Used for admin warning/reminder on the class management page.
     *
     * @return list of draft classes approaching start date
     */
    @GetMapping("/draft-approaching")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<ClassResponse>> getDraftClassesApproachingStartDate() {
        return ResponseEntity.ok(classService.getDraftClassesApproachingStartDate());
    }

    /**
     * Toggles the hidden status of a class. Hidden classes won't appear on
     * landing page for guests/students.
     *
     * @param id the class ID
     * @param hidden true to hide, false to show
     * @return updated class response
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/hidden")
    public ResponseEntity<?> toggleHidden(
            @PathVariable Long id,
            @RequestParam boolean hidden
    ) {
        try {
            ClassResponse resp = classService.toggleClassHidden(id, hidden);
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
        }
    }
}
