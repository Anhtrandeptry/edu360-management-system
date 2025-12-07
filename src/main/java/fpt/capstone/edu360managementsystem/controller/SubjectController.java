package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.SubjectRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.service.SubjectService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    /**
     * GET /api/subjects/paginated - Lấy subjects với phân trang và filter
     *
     * @param search Tìm kiếm theo name, code
     * @param status Filter theo status: ALL, AVAILABLE, UNAVAILABLE
     * @param page Số trang (default 0)
     * @param size Số phần tử mỗi trang (default 10)
     * @param sortBy Trường để sắp xếp (default id)
     * @param order Thứ tự sắp xếp: asc, desc (default asc)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<SubjectResponse>> getSubjectsPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(subjectService.getSubjectsWithPagination(search, status, page, size, sortBy, order));
    }

    // New endpoint: only AVAILABLE subjects (for teacher creation selections)
    @GetMapping("/available")
    public ResponseEntity<List<SubjectResponse>> getAvailableSubjects() {
        return ResponseEntity.ok(subjectService.getAvailableSubjectResponses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubjectResponse> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> disableSubject(@PathVariable Long id) {
        subjectService.disableSubject(id);
        return ResponseEntity.ok("Subject disabled successfully");
    }

    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enableSubject(@PathVariable Long id) {
        subjectService.enableSubject(id);
        return ResponseEntity.ok("Subject enabled successfully");
    }

}
