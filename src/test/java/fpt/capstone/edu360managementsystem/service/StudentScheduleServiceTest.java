package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.StudentScheduleItemResponse;
import fpt.capstone.edu360managementsystem.entity.*;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StudentScheduleServiceTest {
    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @InjectMocks private StudentScheduleService studentScheduleService;

    private Student student;
    private User user;
    private Clazz clazz;
    private ClassEnrollment enrollment;
    private ClassSession session;
    private TimeSlot timeSlot;
    private Room room;
    private Subject subject;
    private Teacher teacher;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Teacher One");

        student = new Student();
        student.setId(1L);
        student.setUser(user);

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");

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
        clazz.setSubject(subject);
        clazz.setTeacher(teacher);

        enrollment = new ClassEnrollment();
        enrollment.setId(1L);
        enrollment.setStudent(student);
        enrollment.setClazz(clazz);

        session = new ClassSession();
        session.setId(1L);
        session.setClazz(clazz);
        session.setDate(LocalDate.of(2024, 12, 1));
        session.setTimeSlot(timeSlot);
        session.setRoom(room);
    }

    // getScheduleByDate - 7 cases
    @Test void test01_getScheduleByDate_studentNotFound() {
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> studentScheduleService.getScheduleByDate(1L, LocalDate.now()))
            .hasMessageContaining("Student profile not found");
    }

    @Test void test02_getScheduleByDate_noEnrollments() {
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of());
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(1L, LocalDate.now());
        assertThat(result).isEmpty();
    }

    @Test void test03_getScheduleByDate_hasEnrollments() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        )).thenReturn(List.of(session));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(1L, date);
        assertThat(result).hasSize(1);
    }

    @Test void test04_getScheduleByDate_filterByDate() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        )).thenReturn(List.of(session));
        studentScheduleService.getScheduleByDate(1L, date);
        verify(classSessionRepository).findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        );
    }

    @Test void test05_getScheduleByDate_multipleSessions() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        ClassSession session2 = new ClassSession();
        session2.setId(2L);
        session2.setClazz(clazz);
        session2.setDate(date);
        session2.setTimeSlot(timeSlot);
        session2.setRoom(room);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        )).thenReturn(List.of(session, session2));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(1L, date);
        assertThat(result).hasSize(2);
    }

    @Test void test06_getScheduleByDate_mapping_allFields() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        )).thenReturn(List.of(session));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByDate(1L, date);
        StudentScheduleItemResponse item = result.get(0);
        assertThat(item.getClassName()).isEqualTo("Math 101");
        assertThat(item.getSubjectName()).isEqualTo("Math");
        assertThat(item.getRoomName()).isEqualTo("Room 101");
    }

    @Test void test07_getScheduleByDate_orderByTime() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(date), eq(date)
        )).thenReturn(List.of(session));
        studentScheduleService.getScheduleByDate(1L, date);
        verify(classSessionRepository).findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), any(), any()
        );
    }

    // getScheduleByWeek - 7 cases
    @Test void test08_getScheduleByWeek_studentNotFound() {
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> studentScheduleService.getScheduleByWeek(1L, LocalDate.now()))
            .hasMessageContaining("Student profile not found");
    }

    @Test void test09_getScheduleByWeek_noEnrollments() {
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of());
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(1L, LocalDate.now());
        assertThat(result).isEmpty();
    }

    @Test void test10_getScheduleByWeek_hasEnrollments() {
        LocalDate weekStart = LocalDate.of(2024, 12, 2); // Monday
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(weekStart), eq(weekStart.plusDays(6))
        )).thenReturn(List.of(session));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(1L, weekStart);
        assertThat(result).hasSize(1);
    }

    @Test void test11_getScheduleByWeek_weekRange_7days() {
        LocalDate weekStart = LocalDate.of(2024, 12, 2);
        LocalDate weekEnd = weekStart.plusDays(6);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(weekStart), eq(weekEnd)
        )).thenReturn(List.of(session));
        studentScheduleService.getScheduleByWeek(1L, weekStart);
        verify(classSessionRepository).findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), eq(weekStart), eq(weekEnd)
        );
    }

    @Test void test12_getScheduleByWeek_multipleSessions() {
        LocalDate weekStart = LocalDate.of(2024, 12, 2);
        ClassSession session2 = new ClassSession();
        session2.setId(2L);
        session2.setClazz(clazz);
        session2.setDate(weekStart.plusDays(2));
        session2.setTimeSlot(timeSlot);
        session2.setRoom(room);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), any(), any()
        )).thenReturn(List.of(session, session2));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(1L, weekStart);
        assertThat(result).hasSize(2);
    }

    @Test void test13_getScheduleByWeek_mapping_correct() {
        LocalDate weekStart = LocalDate.of(2024, 12, 2);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), any(), any()
        )).thenReturn(List.of(session));
        List<StudentScheduleItemResponse> result = studentScheduleService.getScheduleByWeek(1L, weekStart);
        assertThat(result.get(0).getClassName()).isEqualTo("Math 101");
    }

    @Test void test14_getScheduleByWeek_orderByDateTime() {
        LocalDate weekStart = LocalDate.of(2024, 12, 2);
        when(studentRepository.findByUser_Id(1L)).thenReturn(Optional.of(student));
        when(classEnrollmentRepository.findByStudent_Id(1L)).thenReturn(List.of(enrollment));
        when(classSessionRepository.findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), any(), any()
        )).thenReturn(List.of(session));
        studentScheduleService.getScheduleByWeek(1L, weekStart);
        verify(classSessionRepository).findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            anyList(), any(), any()
        );
    }

    // getCurrentWeekStart - 1 case
    @Test void test15_getCurrentWeekStart_calculateMonday() {
        LocalDate wednesday = LocalDate.of(2024, 12, 4); // Wednesday
        LocalDate monday = studentScheduleService.getCurrentWeekStart(wednesday);
        assertThat(monday.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(monday).isEqualTo(LocalDate.of(2024, 12, 2));
    }
}
