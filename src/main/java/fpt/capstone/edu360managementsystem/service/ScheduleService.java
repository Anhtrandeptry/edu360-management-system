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
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
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
        // Resolve teacher by userId using repository method
        Optional<Teacher> optTeacher = teacherRepository.findByUserId(userId);

        if (optTeacher.isEmpty()) {
            // Return empty list instead of throwing exception to prevent 500 error
            return List.of();
        }

        Teacher teacher = optTeacher.get();

        LocalDateTime from = parseIsoDateTime(fromStr);
        LocalDateTime to = parseIsoDateTime(toStr);

        // Find all class schedules where this teacher is teaching
        // Only include classes that are active (exclude ARCHIVED)
        List<ClassSchedule> schedules = classScheduleRepository.findAll().stream()
                .filter(cs -> {
                    Clazz clazz = cs.getClazz();
                    return clazz != null
                            && clazz.getTeacher() != null
                            && teacher.getId().equals(clazz.getTeacher().getId())
                            && clazz.getStatus() != ClassStatus.ARCHIVED;
                })
                .toList();

        System.out.println("Teacher ID: " + teacher.getId() + " | Found " + schedules.size() + " active schedules");

        List<BusySlotResponse> busySlots = expandSchedulesToSlots(schedules, from, to);

        System.out.println("Expanded to " + busySlots.size() + " busy slots");
        if (!busySlots.isEmpty()) {
            System.out.println("First slot: " + busySlots.get(0).getStart() + " -> " + busySlots.get(0).getEnd());
        }

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
        // Only include classes that are active (exclude ARCHIVED)
        List<ClassSchedule> schedules = classScheduleRepository.findAll().stream()
                .filter(cs -> {
                    Clazz clazz = cs.getClazz();
                    return clazz != null
                            && clazz.getRoom() != null
                            && roomId.equals(clazz.getRoom().getId())
                            && clazz.getStatus() != ClassStatus.ARCHIVED;
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

            // dayOfWeek stored in DB: might be 0-6 (JS convention) or 1-7 (ISO convention)
            // 0 = Sunday (JS) → convert to 7 (ISO)
            // 1-6 remain the same
            int dbDay = cs.getDayOfWeek();
            int isoDay = (dbDay == 0) ? 7 : dbDay; // Convert Sunday: 0 → 7

            // Validate range before creating DayOfWeek
            if (isoDay < 1 || isoDay > 7) {
                System.err.println("Invalid dayOfWeek: " + dbDay + " for class: " + clazz.getName());
                continue;
            }

            DayOfWeek javaDow = DayOfWeek.of(isoDay);

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
     * Parse ISO date-time string. Handle both full ISO and simple date strings.
     */
    private LocalDateTime parseIsoDateTime(String str) {
        if (str == null || str.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            // e.g. "2025-11-03T00:00:00"
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignore) {
        }
        try {
            // e.g. "2025-11-03T00:00:00Z" or with offset "+07:00"
            return java.time.OffsetDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
        } catch (Exception ignore) {
        }
        try {
            // Instant form
            return LocalDateTime.ofInstant(java.time.Instant.parse(str), java.time.ZoneId.systemDefault());
        } catch (Exception ignore) {
        }
        try {
            // Date only
            return LocalDate.parse(str, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (Exception ignore) {
        }
        return LocalDateTime.now();
    }
}
