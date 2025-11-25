package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.ChapterCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.LessonCreateRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.CourseResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.service.CourseService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<CourseResponse> createCourse(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CourseCreateRequest req
    ) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        CourseResponse resp = courseService.createCourse(user.getId(), isAdmin, req);
        return ResponseEntity.ok(resp);
    }

    // list course (lọc theo subject & status)
    @GetMapping
    public ResponseEntity<List<CourseResponse>> listCourses(
            @RequestParam(name = "subjectId", required = false) Long subjectId,
            @RequestParam(name = "status", required = false) String status
    ) {
        CourseStatus st = null;
        if (status != null) {
            st = CourseStatus.valueOf(status.toUpperCase());
        }
        return ResponseEntity.ok(courseService.listCourses(subjectId, st));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseDetail(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseDetail(id));
    }

    // Admin duyệt course
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveCourse(@PathVariable Long id) {
        courseService.approveCourse(id);
        return ResponseEntity.ok("Course approved");
    }

    // --- Chapter & Lesson ---

    @PostMapping("/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ChapterResponse> createChapter(
            @Valid @RequestBody ChapterCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createChapter(req));
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody LessonCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createLesson(req));
    }
}
