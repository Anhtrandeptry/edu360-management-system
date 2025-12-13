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

/**
 * REST controller for subject management.
 * Provides endpoints for CRUD operations on academic subjects.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    /**
     * Retrieves all subjects.
     *
     * @return list of all subjects
     */
    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllSubjects() {
        return ResponseEntity.ok(subjectService.getAllSubjects());
    }

    /**
     * Retrieves subjects with pagination and filtering.
     *
     * @param search optional search term
     * @param status status filter (ALL, AVAILABLE, DISABLED)
     * @param page   page number
     * @param size   page size
     * @param sortBy sort field
     * @param order  sort order (asc/desc)
     * @return paginated list of subjects
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

    /**
     * Retrieves only available subjects.
     *
     * @return list of available subjects
     */
    @GetMapping("/available")
    public ResponseEntity<List<SubjectResponse>> getAvailableSubjects() {
        return ResponseEntity.ok(subjectService.getAvailableSubjectResponses());
    }

    /**
     * Retrieves a subject by ID.
     *
     * @param id the subject ID
     * @return subject details
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<SubjectResponse> getSubject(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    /**
     * Creates a new subject.
     *
     * @param request subject data
     * @return created subject
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }

    /**
     * Updates an existing subject.
     *
     * @param id      the subject ID
     * @param request updated subject data
     * @return updated subject
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    /**
     * Disables a subject.
     *
     * @param id the subject ID
     * @return success message
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> disableSubject(@PathVariable Long id) {
        subjectService.disableSubject(id);
        return ResponseEntity.ok("Subject disabled successfully");
    }

    /**
     * Enables a subject.
     *
     * @param id the subject ID
     * @return success message
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enableSubject(@PathVariable Long id) {
        subjectService.enableSubject(id);
        return ResponseEntity.ok("Subject enabled successfully");
    }

}
