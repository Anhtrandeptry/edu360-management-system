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
 * Controller cho tính năng tìm kiếm tổng hợp
 * Cho phép tìm kiếm đồng thời nhiều loại dữ liệu: classes, teachers, subjects
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
     * Tìm kiếm tổng hợp - trả về kết quả từ nhiều nguồn
     * @param q Từ khóa tìm kiếm
     * @param limit Số lượng kết quả tối đa mỗi loại (mặc định 5)
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

        // Temporary stubbed results until service filters are available
        results.put("classes", List.of());
        results.put("teachers", List.of());
        results.put("subjects", List.of());

        // Calculate total results
        results.put("totalResults", 0);

        return ResponseEntity.ok(results);
    }

    /**
     * Tìm kiếm lớp học
     */
    @GetMapping("/classes")
    public ResponseEntity<Page<ClassResponse>> searchClasses(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Temporary stub: return empty page until service API is aligned
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Tìm kiếm giáo viên
     */
    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherResponse>> searchTeachers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Temporary stub: return empty page until service API is aligned
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * Tìm kiếm môn học
     */
    @GetMapping("/subjects")
    public ResponseEntity<Page<SubjectResponse>> searchSubjects(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Temporary stub: return empty page until service API is aligned
        return ResponseEntity.ok(Page.empty());
    }
}
