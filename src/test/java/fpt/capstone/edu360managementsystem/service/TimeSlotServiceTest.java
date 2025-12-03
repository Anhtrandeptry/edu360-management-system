package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.TimeSlotResponse;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.sql.Time;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeSlotServiceTest {
    @Mock private TimeSlotRepository timeSlotRepository;
    @InjectMocks private TimeSlotService timeSlotService;

    private TimeSlot timeSlot;

    @BeforeEach
    void setUp() {
        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setStartTime(Time.valueOf("07:00:00"));
        timeSlot.setEndTime(Time.valueOf("08:30:00"));
    }

    // getAll - 5 cases
    @Test void test01_getAll_emptyDatabase() {
        when(timeSlotRepository.findAll()).thenReturn(List.of());
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result).isEmpty();
    }

    @Test void test02_getAll_hasTimeSlots() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result).hasSize(1);
    }

    @Test void test03_getAll_mapping_correct() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        TimeSlotResponse resp = result.get(0);
        assertThat(resp.getStartTime()).isEqualTo("07:00");
        assertThat(resp.getEndTime()).isEqualTo("08:30");
    }

    @Test void test04_getAll_timeFormat_HHmm() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).contains(":");
        assertThat(result.get(0).getStartTime()).hasSize(5);
    }

    @Test void test05_getAll_multipleSlots() {
        TimeSlot slot2 = new TimeSlot();
        slot2.setId(2L);
        slot2.setStartTime(Time.valueOf("08:45:00"));
        slot2.setEndTime(Time.valueOf("10:15:00"));
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot, slot2));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result).hasSize(2);
    }

    // formatTimeToHHMM - 5 cases
    @Test void test06_formatTime_validTime() {
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).isEqualTo("07:00");
    }

    @Test void test07_formatTime_07_00_00_to_07_00() {
        timeSlot.setStartTime(Time.valueOf("07:00:00"));
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).isEqualTo("07:00");
    }

    @Test void test08_formatTime_23_59_59_to_23_59() {
        timeSlot.setStartTime(Time.valueOf("23:59:59"));
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).isEqualTo("23:59");
    }

    @Test void test09_formatTime_00_00_00_to_00_00() {
        timeSlot.setStartTime(Time.valueOf("00:00:00"));
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).isEqualTo("00:00");
    }

    @Test void test10_formatTime_edgeCases_handled() {
        timeSlot.setStartTime(Time.valueOf("12:34:56"));
        timeSlot.setEndTime(Time.valueOf("13:45:01"));
        when(timeSlotRepository.findAll()).thenReturn(List.of(timeSlot));
        List<TimeSlotResponse> result = timeSlotService.getAll();
        assertThat(result.get(0).getStartTime()).isEqualTo("12:34");
        assertThat(result.get(0).getEndTime()).isEqualTo("13:45");
    }
}
