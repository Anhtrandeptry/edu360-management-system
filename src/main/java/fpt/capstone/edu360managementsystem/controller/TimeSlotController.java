package fpt.capstone.edu360managementsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.response.TimeSlotResponse;
import fpt.capstone.edu360managementsystem.service.TimeSlotService;

/**
 * REST controller for time slot management.
 * Provides endpoints for retrieving available time slots for scheduling.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/timeslots")
@CrossOrigin(origins = "*")
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    /**
     * Retrieves all available time slots.
     *
     * @return list of all time slots
     */
    @GetMapping
    public ResponseEntity<List<TimeSlotResponse>> getAllTimeSlots() {
        return ResponseEntity.ok(timeSlotService.getAll());
    }
}
