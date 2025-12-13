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
 * REST controller for teacher management.
 * Provides endpoints for teacher CRUD operations, profile management, and subject assignments.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private TeacherService teacherService;

    /**
     * Retrieves list of teachers with optional subject filter.
     *
     * @param subjectId optional subject ID to filter teachers
     * @return list of teachers
     */
    @GetMapping
    public ResponseEntity<List<TeacherResponse>> getTeachers(
            @RequestParam(name = "subjectId", required = false) Long subjectId
    ) {
        List<TeacherResponse> teachers = teacherService.getTeachers(subjectId);
        return ResponseEntity.ok(teachers);
    }

    /**
     * Retrieves teachers with pagination and filtering.
     *
     * @param search    optional search term
     * @param subjectId optional subject ID filter
     * @param page      page number
     * @param size      page size
     * @param sortBy    sort field
     * @param order     sort order (asc/desc)
     * @return paginated list of teachers
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
     * Retrieves busy time slots for a teacher within a date range.
     *
     * @param userId the teacher user ID
     * @param from   start date
     * @param to     end date
     * @return list of busy time slots
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
     * Retrieves teacher information by user ID.
     *
     * @param userId the user ID
     * @return teacher details
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<TeacherResponse> getTeacherByUserId(@PathVariable Long userId) {
        TeacherResponse resp = teacherService.getByUserId(userId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Retrieves teacher profile by user ID.
     *
     * @param userId the user ID
     * @return teacher profile details
     */
    @GetMapping("/by-user/{userId}/profile")
    public ResponseEntity<TeacherProfileResponse> getTeacherProfileByUserId(@PathVariable Long userId) {
        TeacherProfileResponse profile = teacherService.getTeacherProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Retrieves the authenticated teacher's own profile.
     *
     * @param auth authentication object containing user details
     * @return teacher profile for the authenticated user
     */
    @GetMapping("/profile")
    public ResponseEntity<TeacherProfileResponse> getMyProfile(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

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

        TeacherProfileResponse profile = teacherService.getTeacherProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Updates the authenticated teacher's own profile.
     *
     * @param auth    authentication object containing user details
     * @param request profile update data
     * @return updated teacher profile
     */
    @PutMapping("/profile")
    public ResponseEntity<TeacherProfileResponse> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody TeacherProfileUpdateRequest request
    ) {
        if (auth == null || auth.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

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
     * Retrieves subjects assigned to a teacher.
     *
     * @param teacherId the teacher ID
     * @return list of subjects taught by the teacher
     */
    @GetMapping("/{teacherId}/subjects")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByTeacherId(@PathVariable Long teacherId) {
        List<SubjectResponse> subjects = teacherService.getSubjectsByTeacherId(teacherId);
        return ResponseEntity.ok(subjects);
    }

    /**
     * Updates the subjects assigned to a teacher.
     *
     * @param teacherId the teacher ID
     * @param body      request body containing subjectIds list
     * @return updated list of subjects
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
     * Updates the primary subject for a teacher.
     *
     * @param teacherId the teacher ID
     * @param body      request body containing subjectId
     * @return updated primary subject
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
