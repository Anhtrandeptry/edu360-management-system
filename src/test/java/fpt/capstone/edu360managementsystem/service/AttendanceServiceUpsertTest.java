package fpt.capstone.edu360managementsystem.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import fpt.capstone.edu360managementsystem.dto.request.AttendanceUpsertRequest;
import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.enums.AttendanceStatus;
import fpt.capstone.edu360managementsystem.exception.SessionNotFoundException;
import fpt.capstone.edu360managementsystem.repository.AttendanceRepository;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;

/**
 * Unit tests for AttendanceService.upsertAttendanceByClassAndDate()
 * 
 * Tests attendance marking with:
 * - Session finding by date and slot
 * - Teacher ownership verification
 * - Student enrollment validation
 * - Create/update attendance records
 * - Notification sending
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService - upsertAttendanceByClassAndDate()")
class AttendanceServiceUpsertTest {

    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Clazz testClass;
    private Teacher testTeacher;
    private Student student1;
    private Student student2;
    private ClassSession testSession;
    private ClassEnrollment enrollment1;
    private ClassEnrollment enrollment2;
    private TimeSlot timeSlot;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 12, 10);
        
        testTeacher = TestDataBuilder.teacher().id(1L).build();
        testTeacher.getUser().setId(1L);
        
        testClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .teacher(testTeacher)
                .build();

        student1 = TestDataBuilder.student().id(1L).build();
        student1.getUser().setId(10L);
        
        student2 = TestDataBuilder.student().id(2L).build();
        student2.getUser().setId(20L);

        timeSlot = TestDataBuilder.timeSlot().id(1L).build();
        
        testSession = TestDataBuilder.session()
                .id(1L)
                .clazz(testClass)
                .date(testDate)
                .timeSlot(timeSlot)
                .build();

        enrollment1 = ClassEnrollment.builder()
                .id(1L)
                .clazz(testClass)
                .student(student1)
                .build();
                
        enrollment2 = ClassEnrollment.builder()
                .id(2L)
                .clazz(testClass)
                .student(student2)
                .build();
    }

    // Helper method to create AttendanceUpsertRequest
    private AttendanceUpsertRequest createAttendanceRequest(List<AttendanceUpsertRequest.Item> items) {
        return new AttendanceUpsertRequest(items);
    }

    private AttendanceUpsertRequest.Item createAttendanceItem(Long studentId, AttendanceStatus status, String note) {
        return new AttendanceUpsertRequest.Item(studentId, status, note);
    }

    @Test
    @DisplayName("Should create new attendance records when none exist")
    void upsertAttendance_NewRecords_ShouldCreateSuccessfully() {
        // Given
        List<AttendanceUpsertRequest.Item> items = Arrays.asList(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null),
                createAttendanceItem(2L, AttendanceStatus.ABSENT, "Sick")
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));  //tìm được session
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(Arrays.asList(enrollment1, enrollment2)); // 2 học sinh đã đăng ký
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1)); // Tìm được student1
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2)); // Tìm được student2
        when(attendanceRepository.findBySessionAndStudent(testSession, student1))
                .thenReturn(Optional.empty()); // Chưa có attendance cho student1
        when(attendanceRepository.findBySessionAndStudent(testSession, student2))
                .thenReturn(Optional.empty()); // Chưa có attendance cho student2
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
        // Note: sendAttendanceNotification is private, cannot verify
    }

    @Test
    @DisplayName("Should update existing attendance records")
    void upsertAttendance_ExistingRecords_ShouldUpdateSuccessfully() {
        // Given
        Attendance existingAttendance = Attendance.builder()
                .id(1L)
                .session(testSession)
                .student(student1)
                .status(AttendanceStatus.UNMARKED)
                .build();

        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, "On time")
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(List.of(enrollment1));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(attendanceRepository.findBySessionAndStudent(testSession, student1))
                .thenReturn(Optional.of(existingAttendance));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        assertThat(existingAttendance.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(existingAttendance.getNote()).isEqualTo("On time");
        verify(attendanceRepository).save(existingAttendance);
    }

    @Test
    @DisplayName("Should throw exception when session not found with slotId")
    void upsertAttendance_SessionNotFoundWithSlot_ShouldThrowException() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> 
                attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Không có buổi học nào");
    }

    @Test
    @DisplayName("Should use first session when slotId is null")
    void upsertAttendance_NoSlotId_ShouldUseFirstSession() {
        // Given
        ClassSession session2 = TestDataBuilder.session()
                .id(2L)
                .clazz(testClass)
                .date(testDate)
                .build();

        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(1L, testDate))
                .thenReturn(Arrays.asList(testSession, session2));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(List.of(enrollment1));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(attendanceRepository.findBySessionAndStudent(testSession, student1))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", null, request);

        // Then
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Should throw exception when no sessions found for date")
    void upsertAttendance_NoSessionsForDate_ShouldThrowException() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(1L, testDate))
                .thenReturn(Collections.emptyList());

        // When/Then
        assertThatThrownBy(() -> 
                attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", null, request))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining("Không có buổi học nào");
    }

    @Test
    @DisplayName("Should throw exception when not class owner")
    void upsertAttendance_NotOwner_ShouldThrowException() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));

        // When/Then - userId 999 is not the teacher's userId (1L)
        assertThatThrownBy(() -> 
                attendanceService.upsertAttendanceByClassAndDate(999L, 1L, "2024-12-10", 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not owner");
    }

    @Test
    @DisplayName("Should throw exception when student not enrolled")
    void upsertAttendance_StudentNotEnrolled_ShouldThrowException() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(999L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(Arrays.asList(enrollment1, enrollment2)); // Only student 1 and 2

        // When/Then
        assertThatThrownBy(() -> 
                attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not enrolled");
    }

    @Test
    @DisplayName("Should throw exception when student not found")
    void upsertAttendance_StudentNotFound_ShouldThrowException() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(List.of(enrollment1));
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> 
                attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    @DisplayName("Should handle mixed attendance statuses")
    void upsertAttendance_MixedStatuses_ShouldHandleAll() {
        // Given
        List<AttendanceUpsertRequest.Item> items = Arrays.asList(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, "On time"),
                createAttendanceItem(2L, AttendanceStatus.LATE, "15 minutes late")
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(Arrays.asList(enrollment1, enrollment2));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(attendanceRepository.findBySessionAndStudent(any(), any()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Should handle attendance with notes")
    void upsertAttendance_WithNotes_ShouldSaveNotes() {
        // Given
        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.ABSENT, "Medical certificate provided")
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(List.of(enrollment1));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(attendanceRepository.findBySessionAndStudent(testSession, student1))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    @DisplayName("Should update attendance when status changes")
    void upsertAttendance_StatusChanged_ShouldUpdate() {
        // Given
        Attendance existingAttendance = Attendance.builder()
                .id(1L)
                .session(testSession)
                .student(student1)
                .status(AttendanceStatus.UNMARKED)
                .build();

        List<AttendanceUpsertRequest.Item> items = List.of(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null)
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(List.of(enrollment1));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(attendanceRepository.findBySessionAndStudent(testSession, student1))
                .thenReturn(Optional.of(existingAttendance));
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        assertThat(existingAttendance.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        verify(attendanceRepository).save(existingAttendance);
    }

    @Test
    @DisplayName("Should handle multiple students in single request")
    void upsertAttendance_MultipleStudents_ShouldProcessAll() {
        // Given
        Student student3 = TestDataBuilder.student().id(3L).build();
        ClassEnrollment enrollment3 = ClassEnrollment.builder()
                .id(3L)
                .clazz(testClass)
                .student(student3)
                .build();

        List<AttendanceUpsertRequest.Item> items = Arrays.asList(
                createAttendanceItem(1L, AttendanceStatus.PRESENT, null),
                createAttendanceItem(2L, AttendanceStatus.ABSENT, "Sick"),
                createAttendanceItem(3L, AttendanceStatus.LATE, "Traffic")
        );
        AttendanceUpsertRequest request = createAttendanceRequest(items);

        when(classSessionRepository.findByClazz_IdAndDateAndTimeSlot_Id(1L, testDate, 1L))
                .thenReturn(Optional.of(testSession));
        when(classEnrollmentRepository.findByClazz_Id(1L))
                .thenReturn(Arrays.asList(enrollment1, enrollment2, enrollment3));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
        when(attendanceRepository.findBySessionAndStudent(any(), any()))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        attendanceService.upsertAttendanceByClassAndDate(1L, 1L, "2024-12-10", 1L, request);

        // Then
        verify(attendanceRepository, times(3)).save(any(Attendance.class));
    }
}
