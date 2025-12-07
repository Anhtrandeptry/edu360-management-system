package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.TeacherProfileUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.response.BusySlotResponse;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherProfileResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.service.ScheduleService;
import fpt.capstone.edu360managementsystem.service.TeacherService;
import jakarta.validation.Valid;

/**
 * Controller for teacher-related operations. Provides free-busy schedule
 * endpoint to check teacher availability. FE passes USER ID (not teacher.id),
 * service resolves teacher internally.
 */
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private TeacherService teacherService;

    /**
     * GET /api/teachers?subjectId=... Returns list of teachers, optionally
     * filtered by subject.
     *
     * @param subjectId Optional subject ID to filter teachers
     * @return List of teachers with user information
     */
    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getTeachers(
            @RequestParam(name = "subjectId", required = false) Long subjectId
    ) {
        List<TeacherResponse> teachers = teacherService.getTeachers(subjectId);
        return ResponseEntity.ok(teachers);
    }

    /**
     * GET /api/teachers/paginated - Lấy teachers với phân trang và filter
     *
     * @param search Tìm kiếm theo fullName, email, phone
     * @param subjectId Filter theo môn học
     * @param page Số trang (default 0)
     * @param size Số phần tử mỗi trang (default 10)
     * @param sortBy Trường để sắp xếp (default id)
     * @param order Thứ tự sắp xếp: asc, desc (default asc)
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<TeacherResponse>> getTeachersPaginated(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        return ResponseEntity.ok(teacherService.getTeachersWithPagination(
                search, subjectId, page, size, sortBy, order
        ));
    }

    /**
     * GET /api/teachers/{id}/free-busy?from=...&to=... Returns all busy time
     * slots for a teacher in the given date range.
     *
     * @param userId User ID associated with the teacher (FE passes this)
     * @param from Start date-time in ISO format
     * @param to End date-time in ISO format
     * @return List of busy slots with start/end times
     */
    @GetMapping("/{id}/free-busy")
    public ResponseEntity<List<BusySlotResponse>> getTeacherFreeBusy(
            @PathVariable("id") Long userId,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to
    ) {
        List<BusySlotResponse> busySlots = scheduleService.getTeacherBusySlots(userId, from, to);
        return ResponseEntity.ok(busySlots);
    }

    /**
     * GET /api/teachers/by-user/{userId} Trả về thông tin teacher (kèm
     * classCount) theo userId để FE có thể kiểm tra trước khi vô hiệu hóa user
     * có role TEACHER.
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<TeacherResponse> getTeacherByUserId(@PathVariable Long userId) {
        TeacherResponse resp = teacherService.getByUserId(userId);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /api/teachers/by-user/{userId}/profile Get public teacher profile by
     * userId
     *
     * @param userId User ID of the teacher
     * @return Teacher profile response with full details
     */
    @GetMapping("/by-user/{userId}/profile")
    public ResponseEntity<TeacherProfileResponse> getTeacherProfileByUserId(@PathVariable Long userId) {
        TeacherProfileResponse profile = teacherService.getTeacherProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/teachers/profile Get current teacher's profile information
     *
     * @param auth Spring Security authentication object
     * @return Teacher profile response
     */
    @GetMapping("/profile")
    public ResponseEntity<TeacherProfileResponse> getMyProfile(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        // Extract userId from UserDetailsImpl
        Long userId;
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails
                    = (org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal();

            // UserDetailsImpl has getId() method
            if (userDetails instanceof fpt.capstone.edu360managementsystem.service.UserDetailsImpl) {
                userId = ((fpt.capstone.edu360managementsystem.service.UserDetailsImpl) userDetails).getId();
            } else {
                return ResponseEntity.status(401).build();
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        TeacherProfileResponse profile = teacherService.getTeacherProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * PUT /api/teachers/profile Update current teacher's profile information
     *
     * @param auth Spring Security authentication object
     * @param request Profile update request
     * @return Updated teacher profile response
     */
    @PutMapping("/profile")
    public ResponseEntity<TeacherProfileResponse> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody TeacherProfileUpdateRequest request
    ) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        // Extract userId from UserDetailsImpl
        Long userId;
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails
                    = (org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal();

            if (userDetails instanceof fpt.capstone.edu360managementsystem.service.UserDetailsImpl) {
                userId = ((fpt.capstone.edu360managementsystem.service.UserDetailsImpl) userDetails).getId();
            } else {
                return ResponseEntity.status(401).build();
            }
        } else {
            return ResponseEntity.status(401).build();
        }

        TeacherProfileResponse updated = teacherService.updateTeacherProfile(userId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/teachers/{teacherId}/subjects Return all subjects taught by a
     * teacher (primary + additional).
     */
    @GetMapping("/{teacherId}/subjects")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByTeacherId(@PathVariable Long teacherId) {
        List<SubjectResponse> subjects = teacherService.getSubjectsByTeacherId(teacherId);
        return ResponseEntity.ok(subjects);
    }

    /**
     * PUT /api/teachers/{teacherId}/subjects Update subjects taught by a
     * teacher (replace additional subjects set).
     */
    @PutMapping("/{teacherId}/subjects")
    public ResponseEntity<List<SubjectResponse>> updateTeacherSubjects(
            @PathVariable Long teacherId,
            @RequestBody java.util.Map<String, java.util.List<Long>> body
    ) {
        java.util.List<Long> subjectIds = body != null ? body.getOrDefault("subjectIds", java.util.List.of()) : java.util.List.of();
        try {
            List<SubjectResponse> updated = teacherService.updateTeacherSubjects(teacherId, subjectIds);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException ex) {
            // Vi phạm nghiệp vụ: đang dạy lớp nên không thể chuyển môn
            return ResponseEntity.badRequest().body(java.util.List.of(
                    SubjectResponse.builder()
                            .id(-1L)
                            .name(ex.getMessage())
                            .status(null)
                            .classCount(0)
                            .build()
            ));
        }
    }

    /**
     * PUT /api/teachers/{teacherId}/primary-subject Change the primary subject
     * for a teacher with business rule enforcement.
     */
    @PutMapping("/{teacherId}/primary-subject")
    public ResponseEntity<SubjectResponse> updatePrimarySubject(
            @PathVariable Long teacherId,
            @RequestBody java.util.Map<String, Long> body
    ) {
        Long subjectId = body != null ? body.get("subjectId") : null;
        if (subjectId == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            SubjectResponse updated = teacherService.updatePrimarySubject(teacherId, subjectId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(SubjectResponse.builder()
                    .id(-1L)
                    .name(ex.getMessage())
                    .status(null)
                    .classCount(0)
                    .build());
        }
    }
}
