package fpt.capstone.edu360managementsystem.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.BusySlotResponse;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.repository.ClassScheduleRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;

/**
 * Service to compute teacher and room busy schedules based on existing classes.
 * Returns time slots when a teacher or room is occupied during a given date
 * range.
 */
@Service
public class ScheduleService {

    @Autowired
    private ClassScheduleRepository classScheduleRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * Get all busy time slots for a teacher (by user ID) in the given date
     * range. FE passes userId (not teacher.id), so we resolve teacher first.
     *
     * @param userId The user ID associated with the teacher
     * @param fromStr Start date in ISO format (e.g., "2025-08-01T00:00:00")
     * @param toStr End date in ISO format
     * @return List of busy slots with isoStart and isoEnd
     */
    public List<BusySlotResponse> getTeacherBusySlots(Long userId, String fromStr, String toStr) {
        System.out.println("[DEBUG] getTeacherBusySlots called with userId: " + userId);

        // Resolve teacher by userId using repository method
        Optional<Teacher> optTeacher = teacherRepository.findByUserId(userId);

        System.out.println("[DEBUG] Teacher found: " + optTeacher.isPresent());

        if (optTeacher.isEmpty()) {
            System.err.println("[ERROR] Teacher not found with userId: " + userId);
            // Return empty list instead of throwing exception to prevent 500 error
            return List.of();
        }

        Teacher teacher = optTeacher.get();
        System.out.println("[DEBUG] Teacher ID: " + teacher.getId() + ", User ID: " + teacher.getUser().getId());

        LocalDateTime from = parseIsoDateTime(fromStr);
        LocalDateTime to = parseIsoDateTime(toStr);
        System.out.println("[DEBUG] Date range: " + from + " to " + to);

        // Find all class schedules where this teacher is teaching
        List<ClassSchedule> schedules = classScheduleRepository.findAll().stream()
                .filter(cs -> {
                    Clazz clazz = cs.getClazz();
                    return clazz != null
                            && clazz.getTeacher() != null
                            && teacher.getId().equals(clazz.getTeacher().getId());
                })
                .toList();

        System.out.println("[DEBUG] Found " + schedules.size() + " class schedules for teacher");

        List<BusySlotResponse> busySlots = expandSchedulesToSlots(schedules, from, to);
        System.out.println("[DEBUG] Expanded to " + busySlots.size() + " busy slots");

        return busySlots;
    }

    /**
     * Get all busy time slots for a room in the given date range.
     *
     * @param roomId The room ID
     * @param fromStr Start date in ISO format
     * @param toStr End date in ISO format
     * @return List of busy slots
     */
    public List<BusySlotResponse> getRoomBusySlots(Long roomId, String fromStr, String toStr) {
        LocalDateTime from = parseIsoDateTime(fromStr);
        LocalDateTime to = parseIsoDateTime(toStr);

        // Find all class schedules using this room
        List<ClassSchedule> schedules = classScheduleRepository.findAll().stream()
                .filter(cs -> {
                    Clazz clazz = cs.getClazz();
                    return clazz != null
                            && clazz.getRoom() != null
                            && roomId.equals(clazz.getRoom().getId());
                })
                .toList();

        return expandSchedulesToSlots(schedules, from, to);
    }

    /**
     * Expand class schedules (recurring weekly patterns) into concrete time
     * slots within the specified date range.
     *
     * For each ClassSchedule (dayOfWeek + timeSlot), generate all occurrences
     * between from and to dates, constrained by the class's start/end dates.
     */
    private List<BusySlotResponse> expandSchedulesToSlots(
            List<ClassSchedule> schedules,
            LocalDateTime from,
            LocalDateTime to
    ) {
        List<BusySlotResponse> result = new ArrayList<>();

        for (ClassSchedule cs : schedules) {
            Clazz clazz = cs.getClazz();
            if (clazz == null || cs.getTimeSlot() == null) {
                continue;
            }

            // Class date boundaries
            LocalDate classStart = clazz.getStartDate();
            LocalDate classEnd = clazz.getEndDate();

            // Time slot details
            LocalTime slotStart = cs.getTimeSlot().getStartTime().toLocalTime();
            LocalTime slotEnd = cs.getTimeSlot().getEndTime().toLocalTime();

            // dayOfWeek: business uses 2-8 (Mon-Sun), but ClassSchedule might use 1-7 or 2-8
            // Assuming ClassSchedule.dayOfWeek is already 2-8 as per business doc
            // Convert to Java DayOfWeek: Mon=1, ..., Sun=7
            int bizDay = cs.getDayOfWeek(); // 2-8
            DayOfWeek javaDow = convertBizDayToJavaDayOfWeek(bizDay);

            // Find first occurrence of this day in the range
            LocalDate current = from.toLocalDate();
            while (!current.getDayOfWeek().equals(javaDow) && current.isBefore(to.toLocalDate())) {
                current = current.plusDays(1);
            }

            // Generate all weekly recurrences
            while (!current.isAfter(to.toLocalDate()) && !current.isAfter(classEnd)) {
                if (!current.isBefore(classStart) && !current.isBefore(from.toLocalDate())) {
                    LocalDateTime slotStartDt = LocalDateTime.of(current, slotStart);
                    LocalDateTime slotEndDt = LocalDateTime.of(current, slotEnd);

                    // Only include if within query range
                    if (!slotStartDt.isAfter(to) && !slotEndDt.isBefore(from)) {
                        result.add(new BusySlotResponse(
                                slotStartDt.toString(),
                                slotEndDt.toString()
                        ));
                    }
                }
                current = current.plusWeeks(1);
            }
        }

        return result;
    }

    /**
     * Convert business day convention (2-8) to Java DayOfWeek (1-7). Business:
     * Mon=2, Tue=3, ..., Sun=8 Java: Mon=1, Tue=2, ..., Sun=7
     */
    private DayOfWeek convertBizDayToJavaDayOfWeek(int bizDay) {
        if (bizDay == 8) {
            return DayOfWeek.SUNDAY;
        }
        return DayOfWeek.of(bizDay - 1); // 2->1, 3->2, ..., 7->6
    }

    /**
     * Parse ISO date-time string. Handle both full ISO and simple date strings.
     */
    private LocalDateTime parseIsoDateTime(String str) {
        if (str == null || str.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            // Fallback: try parsing as date only
            try {
                return LocalDate.parse(str, DateTimeFormatter.ISO_DATE).atStartOfDay();
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }
}
