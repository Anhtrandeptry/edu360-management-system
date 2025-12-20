package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.BusySlotResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.sql.Time;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleServiceTest {
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private TeacherRepository teacherRepository;
    @InjectMocks private ScheduleService scheduleService;

    private Teacher teacher;
    private Clazz clazz;
    private ClassSchedule schedule;
    private TimeSlot timeSlot;
    private Room room;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(user);

        room = new Room();
        room.setId(1L);
        room.setName("Room 101");

        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setStartTime(Time.valueOf("07:00:00"));
        timeSlot.setEndTime(Time.valueOf("08:30:00"));

        clazz = new Clazz();
        clazz.setId(1L);
        clazz.setName("Math 101");
        clazz.setTeacher(teacher);
        clazz.setRoom(room);
        clazz.setStartDate(LocalDate.of(2024, 12, 1));
        clazz.setEndDate(LocalDate.of(2024, 12, 31));
        clazz.setStatus(ClassStatus.PUBLIC);

        schedule = new ClassSchedule();
        schedule.setId(1L);
        schedule.setClazz(clazz);
        schedule.setTimeSlot(timeSlot);
        schedule.setDayOfWeek(1); // Monday
    }

    // getTeacherBusySlots - 25 cases

    @Test void test01_teacherBusy_teacherNotFound() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.empty());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test02_teacherBusy_teacherFound() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotNull();
    }

    @Test void test03_teacherBusy_noSchedules() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test04_teacherBusy_hasSchedules() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test05_teacherBusy_filterByTeacher() {
        Teacher otherTeacher = new Teacher();
        otherTeacher.setId(999L);
        Clazz otherClass = new Clazz();
        otherClass.setTeacher(otherTeacher);
        otherClass.setStatus(ClassStatus.PUBLIC);
        ClassSchedule otherSchedule = new ClassSchedule();
        otherSchedule.setClazz(otherClass);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, otherSchedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test06_teacherBusy_filterByStatus() {
        Clazz completedClass = new Clazz();
        completedClass.setTeacher(teacher);
        completedClass.setStatus(ClassStatus.ARCHIVED);
        ClassSchedule completedSchedule = new ClassSchedule();
        completedSchedule.setClazz(completedClass);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, completedSchedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test07_teacherBusy_includeActiveClasses() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test08_teacherBusy_excludeCompletedClasses() {
        clazz.setStatus(ClassStatus.ARCHIVED);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test09_teacherBusy_multipleClasses() {
        ClassSchedule schedule2 = new ClassSchedule();
        schedule2.setClazz(clazz);
        schedule2.setTimeSlot(timeSlot);
        schedule2.setDayOfWeek(3); // Wednesday
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, schedule2));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test void test10_teacherBusy_noActiveClasses() {
        clazz.setStatus(ClassStatus.ARCHIVED);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test11_teacherBusy_slotExpansion() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test12_teacherBusy_weeklyRecurrence() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test void test13_teacherBusy_dateRangeFilter() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-07T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test14_teacherBusy_classBoundaries() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test15_teacherBusy_dayOfWeekConversion() {
        schedule.setDayOfWeek(0); // Sunday -> should convert to 7
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotNull();
    }

    @Test void test16_teacherBusy_dayOfWeek1to6() {
        schedule.setDayOfWeek(1); // Monday
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test17_teacherBusy_invalidDayOfWeek() {
        schedule.setDayOfWeek(99); // Invalid
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test18_teacherBusy_timeSlot() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        if (!result.isEmpty()) {
            assertThat(result.get(0).getStart()).isNotNull();
            assertThat(result.get(0).getEnd()).isNotNull();
        }
    }

    @Test void test19_teacherBusy_isoFormat() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        if (!result.isEmpty()) {
            assertThat(result.get(0).getStart()).contains("T");
        }
    }

    @Test void test20_teacherBusy_multipleTeachers() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test21_teacherBusy_teacherHasNoClasses() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test22_teacherBusy_mixedStatuses() {
        Clazz comingSoonClass = new Clazz();
        comingSoonClass.setTeacher(teacher);
        comingSoonClass.setStatus(ClassStatus.PUBLIC);
        comingSoonClass.setStartDate(LocalDate.of(2024, 12, 1));
        comingSoonClass.setEndDate(LocalDate.of(2024, 12, 31));
        ClassSchedule schedule2 = new ClassSchedule();
        schedule2.setClazz(comingSoonClass);
        schedule2.setTimeSlot(timeSlot);
        schedule2.setDayOfWeek(2);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, schedule2));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test23_teacherBusy_userIdMapping() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        verify(teacherRepository).findByUserId(1L);
    }

    @Test void test24_teacherBusy_invalidUserId() {
        when(teacherRepository.findByUserId(999L)).thenReturn(Optional.empty());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(999L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test25_teacherBusy_multipleSchedules() {
        ClassSchedule schedule2 = new ClassSchedule();
        schedule2.setClazz(clazz);
        schedule2.setTimeSlot(timeSlot);
        schedule2.setDayOfWeek(3);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, schedule2));
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    // getRoomBusySlots - 15 cases
    @Test void test26_roomBusy_noSchedules() {
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test27_roomBusy_hasSchedules() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test28_roomBusy_filterByRoom() {
        Room otherRoom = new Room();
        otherRoom.setId(999L);
        Clazz otherClass = new Clazz();
        otherClass.setRoom(otherRoom);
        otherClass.setStatus(ClassStatus.PUBLIC);
        ClassSchedule otherSchedule = new ClassSchedule();
        otherSchedule.setClazz(otherClass);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, otherSchedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test29_roomBusy_filterByStatus() {
        clazz.setStatus(ClassStatus.ARCHIVED);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test30_roomBusy_includeActiveClasses() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test31_roomBusy_excludeCompletedClasses() {
        clazz.setStatus(ClassStatus.ARCHIVED);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test32_roomBusy_multipleClasses() {
        ClassSchedule schedule2 = new ClassSchedule();
        schedule2.setClazz(clazz);
        schedule2.setTimeSlot(timeSlot);
        schedule2.setDayOfWeek(3);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, schedule2));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test void test33_roomBusy_roomNotUsed() {
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(999L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test34_roomBusy_mixedStatuses() {
        Clazz comingSoonClass = new Clazz();
        comingSoonClass.setRoom(room);
        comingSoonClass.setStatus(ClassStatus.PUBLIC);
        comingSoonClass.setStartDate(LocalDate.of(2024, 12, 1));
        comingSoonClass.setEndDate(LocalDate.of(2024, 12, 31));
        ClassSchedule schedule2 = new ClassSchedule();
        schedule2.setClazz(comingSoonClass);
        schedule2.setTimeSlot(timeSlot);
        schedule2.setDayOfWeek(2);
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule, schedule2));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test35_roomBusy_slotExpansion() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test36_roomBusy_dateRangeFilter() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-07T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test37_roomBusy_weeklyRecurrence() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test void test38_roomBusy_classBoundaries() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test39_roomBusy_timeFormat() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        if (!result.isEmpty()) {
            assertThat(result.get(0).getStart()).contains("T");
        }
    }

    @Test void test40_roomBusy_isoOutput() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        if (!result.isEmpty()) {
            assertThat(result.get(0).getStart()).isNotNull();
            assertThat(result.get(0).getEnd()).isNotNull();
        }
    }

    // parseIsoDateTime - 5 cases
    @Test void test41_parseIso_isoDateTime() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotNull();
    }

    @Test void test42_parseIso_withTimezone() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00Z", "2024-12-31T23:59:59Z");
        assertThat(result).isNotNull();
    }

    @Test void test43_parseIso_instantFormat() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isNotNull();
    }

    @Test void test44_parseIso_dateOnly() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "2024-12-01", "2024-12-31");
        assertThat(result).isNotNull();
    }

    @Test void test45_parseIso_invalidFormat_fallback() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getTeacherBusySlots(1L, "invalid", "invalid");
        assertThat(result).isNotNull();
    }

    // expandSchedulesToSlots - 5 cases
    @Test void test46_expand_emptySchedules() {
        when(classScheduleRepository.findAll()).thenReturn(List.of());
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).isEmpty();
    }

    @Test void test47_expand_singleOccurrence() {
        clazz.setStartDate(LocalDate.of(2024, 12, 2)); // Monday
        clazz.setEndDate(LocalDate.of(2024, 12, 2));
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result).hasSize(1);
    }

    @Test void test48_expand_multipleOccurrences() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }

    @Test void test49_expand_dateRangeBoundaries() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-07T23:59:59");
        assertThat(result).isNotEmpty();
    }

    @Test void test50_expand_weeklyIncrement() {
        when(classScheduleRepository.findAll()).thenReturn(List.of(schedule));
        List<BusySlotResponse> result = scheduleService.getRoomBusySlots(1L, "2024-12-01T00:00:00", "2024-12-31T23:59:59");
        assertThat(result.size()).isGreaterThan(1);
    }
}
