package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.response.SemesterResponse;
import fpt.capstone.edu360managementsystem.service.SemesterService;

/**
 * REST controller for semester management.
 * Provides endpoints for retrieving semester information.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/semesters")
@CrossOrigin(origins = "*")
public class SemesterController {

    @Autowired
    private SemesterService semesterService;

    /**
     * Retrieves all semesters with optional status filter.
     *
     * @param status optional status filter
     * @return list of semesters
     */
    @GetMapping
    public ResponseEntity<List<SemesterResponse>> getAllSemesters(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(semesterService.getAll(status));
    }

    /**
     * Retrieves semester details by ID.
     *
     * @param id the semester ID
     * @return semester details
     */
    @GetMapping("/{id}")
    public ResponseEntity<SemesterResponse> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getById(id));
    }
}
