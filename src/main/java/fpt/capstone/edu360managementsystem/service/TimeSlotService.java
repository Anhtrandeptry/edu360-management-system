package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.TimeSlotResponse;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.repository.TimeSlotRepository;

@Service
public class TimeSlotService {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<TimeSlotResponse> getAll() {
        return timeSlotRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TimeSlotResponse mapToResponse(TimeSlot ts) {
        return TimeSlotResponse.builder()
                .id(ts.getId())
                .startTime(ts.getStartTime() != null ? formatTimeToHHMM(ts.getStartTime()) : "")
                .endTime(ts.getEndTime() != null ? formatTimeToHHMM(ts.getEndTime()) : "")
                .build();
    }

    /**
     * Format SQL Time to HH:mm string (without seconds). Example: 07:00:00 ->
     * 07:00
     */
    private String formatTimeToHHMM(java.sql.Time time) {
        String fullTime = time.toString(); // "HH:mm:ss"
        return fullTime.substring(0, 5);   // "HH:mm"
    }
}
