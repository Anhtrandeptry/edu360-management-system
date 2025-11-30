package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.CreateTeacherCourseVersionRequest;
import fpt.capstone.edu360managementsystem.dto.response.TeacherCourseVersionResponse;
import fpt.capstone.edu360managementsystem.service.TeacherCourseVersionService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/course-versions")
public class TeacherCourseVersionController {

    @Autowired
    private TeacherCourseVersionService teacherCourseVersionService;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<TeacherCourseVersionResponse> createMapping(
            @AuthenticationPrincipal UserDetailsImpl user,
            @Valid @RequestBody CreateTeacherCourseVersionRequest req) {
        // Truyền userId, service sẽ ánh xạ sang teacherId
        TeacherCourseVersionResponse resp = teacherCourseVersionService.createMapping(user.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<TeacherCourseVersionResponse>> listMappings(
            @AuthenticationPrincipal UserDetailsImpl user,
            @RequestParam Long baseCourseId) {
        List<TeacherCourseVersionResponse> list = teacherCourseVersionService.listMappings(user.getId(), baseCourseId);
        return ResponseEntity.ok(list);
    }
}
