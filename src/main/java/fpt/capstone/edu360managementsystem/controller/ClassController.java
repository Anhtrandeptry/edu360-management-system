package fpt.capstone.edu360managementsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.CreateClassRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.service.ClassService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    @Autowired
    private ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClassResponse> create(@Valid @RequestBody CreateClassRequest request) {
        return ResponseEntity.ok(classService.createClass(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<ClassResponse>> list(
            @RequestParam(name = "teacherUserId", required = false) Long teacherUserId,
            @RequestParam(name = "timeSlotId", required = false) Long timeSlotId
    ) {
        return ResponseEntity.ok(classService.listClasses(teacherUserId, timeSlotId));
    }

    // (mở rộng sau) GET danh sách lớp, GET chi tiết lớp, GET sessions, v.v.
}
