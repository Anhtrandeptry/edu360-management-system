package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.response.BusySlotResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.service.ScheduleService;
import fpt.capstone.edu360managementsystem.service.TeacherService;

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
}
