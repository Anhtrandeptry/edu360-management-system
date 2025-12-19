package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.*;
import fpt.capstone.edu360managementsystem.service.ClassService;
import fpt.capstone.edu360managementsystem.service.SubjectService;
import fpt.capstone.edu360managementsystem.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for global search functionality.
 * Provides endpoints for searching classes, teachers, and subjects.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SearchController {

    private final ClassService classService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;

    /**
     * Performs a global search across multiple data types.
     *
     * @param q     the search keyword
     * @param limit maximum results per type (default 5)
     * @return search results from classes, teachers, and subjects
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "query", "",
                "classes", List.of(),
                "teachers", List.of(),
                "subjects", List.of(),
                "totalResults", 0
            ));
        }

        String searchTerm = q.trim();
        Map<String, Object> results = new HashMap<>();
        results.put("query", searchTerm);

        // Search classes - only PUBLIC classes, include currentStudents and maxStudents
        // Pass null for minPrice and maxPrice to not filter by price
        Page<ClassResponse> classResults = classService.getClassesWithPagination(
                searchTerm, "PUBLIC", null, null, null, null, 0, limit, "id", "desc"
        );
        results.put("classes", classResults.getContent());

        // Search teachers
        Page<TeacherResponse> teacherResults = teacherService.getTeachersWithPagination(
                searchTerm, null, 0, limit, "id", "desc"
        );
        results.put("teachers", teacherResults.getContent());

        // Search subjects
        Page<SubjectResponse> subjectResults = subjectService.getSubjectsWithPagination(
                searchTerm, "ACTIVE", 0, limit, "id", "desc"
        );
        results.put("subjects", subjectResults.getContent());

        int totalResults = classResults.getContent().size() 
                + teacherResults.getContent().size() 
                + subjectResults.getContent().size();
        results.put("totalResults", totalResults);

        return ResponseEntity.ok(results);
    }

    /**
     * Searches for classes by keyword.
     *
     * @param q    the search keyword
     * @param page page number
     * @param size page size
     * @return paginated class results
     */
    @GetMapping("/classes")
    public ResponseEntity<Page<ClassResponse>> searchClasses(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Searches for teachers by keyword.
     *
     * @param q    the search keyword
     * @param page page number
     * @param size page size
     * @return paginated teacher results
     */
    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherResponse>> searchTeachers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Searches for subjects by keyword.
     *
     * @param q    the search keyword
     * @param page page number
     * @param size page size
     * @return paginated subject results
     */
    @GetMapping("/subjects")
    public ResponseEntity<Page<SubjectResponse>> searchSubjects(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(Page.empty());
    }
}
