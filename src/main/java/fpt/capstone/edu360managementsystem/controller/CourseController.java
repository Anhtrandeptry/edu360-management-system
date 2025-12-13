package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

/**
 * REST controller for course management.
 * Provides endpoints for CRUD operations on courses, chapters, and lessons.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * Creates a new course.
     *
     * @param user the authenticated user
     * @param req  the course creation data
     * @return created course response
     */
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

    /**
     * Lists all courses with optional filters.
     *
     * @param subjectId optional subject filter
     * @param status    optional status filter
     * @return list of courses
     */
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

    /**
     * Retrieves paginated courses with filters and sorting.
     *
     * @param search        optional search term
     * @param status        status filter
     * @param subjectId     optional subject filter
     * @param teacherUserId optional teacher filter
     * @param page          page number
     * @param size          page size
     * @param sortBy        sort field
     * @param order         sort order
     * @return paginated course list
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<CourseResponse>> getCoursesPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(courseService.getCoursesWithPagination(
                search, status, subjectId, teacherUserId, page, size, sortBy, order
        ));
    }

    /**
     * Lists courses created by the authenticated teacher.
     *
     * @param auth the authentication object
     * @return list of teacher's courses
     */
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

    /**
     * Retrieves course details by ID.
     *
     * @param id the course ID
     * @return course details
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseDetail(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseDetail(id));
    }

    /**
     * Approves a pending course.
     *
     * @param id the course ID
     * @return success message
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveCourse(@PathVariable Long id) {
        courseService.approveCourse(id);
        return ResponseEntity.ok("Course approved");
    }

    /**
     * Rejects a pending course.
     *
     * @param id the course ID
     * @return success message
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectCourse(@PathVariable Long id) {
        courseService.rejectCourse(id);
        return ResponseEntity.ok("Course rejected");
    }

    /**
     * Updates an existing course.
     * Course status will be reset to PENDING after update.
     *
     * @param id  the course ID
     * @param req the update data
     * @return success message
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseUpdateRequest req
    ) {
        courseService.updateCourse(id, req);
        return ResponseEntity.ok("Course updated and reset to PENDING");
    }

    /**
     * Creates a new chapter for a course.
     *
     * @param req the chapter creation data
     * @return created chapter response
     */
    @PostMapping("/chapters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ChapterResponse> createChapter(
            @Valid @RequestBody ChapterCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createChapter(req));
    }

    /**
     * Updates an existing chapter.
     *
     * @param id  the chapter ID
     * @param req the update data
     * @return updated chapter response
     */
    @PutMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ChapterResponse> updateChapter(
            @PathVariable Long id,
            @RequestBody ChapterCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.updateChapter(id, req));
    }

    /**
     * Creates a new lesson for a chapter.
     *
     * @param req the lesson creation data
     * @return created lesson response
     */
    @PostMapping("/lessons")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> createLesson(
            @Valid @RequestBody LessonCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.createLesson(req));
    }

    /**
     * Updates an existing lesson.
     *
     * @param id  the lesson ID
     * @param req the update data
     * @return updated lesson response
     */
    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable Long id,
            @RequestBody LessonCreateRequest req
    ) {
        return ResponseEntity.ok(courseService.updateLesson(id, req));
    }

    /**
     * Removes a chapter and its lessons.
     *
     * @param id the chapter ID
     * @return no content response
     */
    @DeleteMapping("/chapters/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> removeChapter(@PathVariable Long id) {
        courseService.removeChapter(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a lesson.
     *
     * @param id the lesson ID
     * @return no content response
     */
    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<?> removeLesson(@PathVariable Long id) {
        courseService.removeLesson(id);
        return ResponseEntity.noContent().build();
    }
}
