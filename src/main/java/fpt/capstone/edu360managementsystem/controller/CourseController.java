package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.ChapterCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseCreateRequest;
import fpt.capstone.edu360managementsystem.dto.request.CourseUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.request.LessonCreateRequest;
import fpt.capstone.edu360managementsystem.dto.response.ChapterResponse;
import fpt.capstone.edu360managementsystem.dto.response.CourseResponse;
import fpt.capstone.edu360managementsystem.dto.response.LessonResponse;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.service.CourseService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

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

    // Danh sách khóa học cá nhân của giáo viên hiện tại
    @GetMapping("/mine")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseResponse>> listMyCourses(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId;
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails
                && userDetails instanceof fpt.capstone.edu360managementsystem.service.UserDetailsImpl impl) {
            userId = impl.getId();
        } else {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(courseService.listCoursesOfTeacher(userId));
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

    // Admin từ chối course
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectCourse(@PathVariable Long id) {
        courseService.rejectCourse(id);
        return ResponseEntity.ok("Course rejected");
    }

    // Update course (teacher/admin edit)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseUpdateRequest req
    ) {
        courseService.updateCourse(id, req);
        return ResponseEntity.ok("Course updated and reset to PENDING");
    }

    // --- Chapter & Lesson ---
    @PostMapping("/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ChapterResponse> createChapter(
            @Valid @RequestBody ChapterCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createChapter(req));
    }

    @PutMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable Long id,
            @RequestBody ChapterCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.updateChapter(id, req));
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody LessonCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createLesson(req));
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.updateLesson(id, req));
    }

    // Remove chapter (and its lessons)
    @DeleteMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> removeChapter(@PathVariable Long id) {
        courseService.removeChapter(id);
        return ResponseEntity.noContent().build();
    }

    // Remove single lesson
    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> removeLesson(@PathVariable Long id) {
        courseService.removeLesson(id);
        return ResponseEntity.noContent().build();
    }
}
