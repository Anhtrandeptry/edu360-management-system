package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.TeacherClassAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherListForAttendanceResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherWorkSummaryResponse;
import fpt.capstone.edu360managementsystem.service.TeacherAttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceService teacherAttendanceService;


    @GetMapping("/teachers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TeacherListForAttendanceResponse>> getAllTeachers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(teacherAttendanceService.getAllTeachersForAttendancePaginated(search, pageable));
    }


    @GetMapping("/teachers/{teacherId}/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherWorkSummaryResponse> getTeacherSummary(
            @PathVariable Long teacherId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        return ResponseEntity.ok(teacherAttendanceService.getTeacherWorkSummary(teacherId, month, year));
    }


    @GetMapping("/teachers/{teacherId}/classes/{classId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeacherClassAttendanceResponse> getTeacherClassAttendance(
            @PathVariable Long teacherId,
            @PathVariable Long classId
    ) {
        return ResponseEntity.ok(teacherAttendanceService.getTeacherClassAttendance(teacherId, classId));
    }
}
