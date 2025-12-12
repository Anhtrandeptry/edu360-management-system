package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.repository.*;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrollmentService.enrollAfterPayment()
 * 
 * Test coverage:
 * - First enrollment → success + notification
 * - Already enrolled → skip (idempotent)
 * - Class full → error but payment kept
 * - Schedule conflict → error but payment kept
 * - Notification fails → enrollment still succeeds
 * - No semester → skip conflict check
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService - enrollAfterPayment()")
class EnrollmentServiceEnrollAfterPaymentTest {

    @Mock
    private ClazzRepository clazzRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private ClassScheduleRepository classScheduleRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    @Captor
    private ArgumentCaptor<ClassEnrollment> enrollmentCaptor;

    private Clazz testClass;
    private Student testStudent;
    private Semester testSemester;

    @BeforeEach
    void setUp() {
        testSemester = TestDataBuilder.semester().id(1L).build();

        testStudent = TestDataBuilder.student()
                .id(1L)
                .user(TestDataBuilder.user().id(2L).fullName("Student User").build())
                .build();

        testClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .maxStudents(30)
                .semester(testSemester)
                .build();
    }

    @Test
    @DisplayName("Should enroll student and send notification when first enrollment")
    void enrollAfterPayment_FirstEnrollment_ShouldEnrollAndNotify() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then
        verify(classEnrollmentRepository).save(enrollmentCaptor.capture());
        ClassEnrollment savedEnrollment = enrollmentCaptor.getValue();

        assertThat(savedEnrollment.getClazz()).isEqualTo(testClass);
        assertThat(savedEnrollment.getStudent()).isEqualTo(testStudent);

        // Verify notification was sent
        verify(notificationService).notifyEnrolledNewClass(
                eq(testStudent.getUser().getId()),
                eq(testClass.getName()),
                eq(testClass.getId())
        );
    }

    @Test
    @DisplayName("Should skip enrollment when student is already enrolled (idempotent)")
    void enrollAfterPayment_AlreadyEnrolled_ShouldSkip() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(true); // Already enrolled

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then - should return early without error
        verify(classEnrollmentRepository, never()).save(any());
        verify(notificationService, never()).notifyEnrolledNewClass(any(), any(), any());
        verify(classEnrollmentRepository, never()).countByClazz_Id(any());
    }

    @Test
    @DisplayName("Should throw error when class is full")
    void enrollAfterPayment_ClassFull_ShouldThrowError() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId))
                .thenReturn(30); // Full (maxStudents = 30)

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollAfterPayment(classId, studentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class is full - cannot auto-enroll after payment");

        verify(classEnrollmentRepository, never()).save(any());
        verify(notificationService, never()).notifyEnrolledNewClass(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw error when schedule conflict exists")
    void enrollAfterPayment_ScheduleConflict_ShouldThrowError() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        ClassSchedule schedule = new ClassSchedule();
        schedule.setDayOfWeek(2);
        TimeSlot timeSlot = TestDataBuilder.timeSlot().id(1L).build();
        schedule.setTimeSlot(timeSlot);

        Clazz conflictingClass = TestDataBuilder.clazz().id(2L).name("Conflict Class").build();

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.singletonList(schedule));
        when(classEnrollmentRepository.findScheduleConflicts(
                eq(testStudent.getId()),
                eq(testSemester.getId()),
                any(Set.class),
                any(Set.class)))
                .thenReturn(Collections.singletonList(conflictingClass));

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollAfterPayment(classId, studentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Schedule conflict - cannot auto-enroll after payment");

        verify(classEnrollmentRepository, never()).save(any());
        verify(notificationService, never()).notifyEnrolledNewClass(any(), any(), any());
    }

    @Test
    @DisplayName("Should enroll successfully even if notification fails")
    void enrollAfterPayment_NotificationFails_ShouldStillEnroll() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Notification fails
        doThrow(new RuntimeException("Notification service unavailable"))
                .when(notificationService).notifyEnrolledNewClass(any(), any(), any());

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then - enrollment should still succeed
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
        verify(notificationService).notifyEnrolledNewClass(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip conflict check when class has no semester")
    void enrollAfterPayment_NoSemester_ShouldSkipConflictCheck() {
        // Given
        testClass.setSemester(null); // No semester
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
        // Conflict check should be skipped
        verify(classScheduleRepository, never()).findByClazz_Id(any());
        verify(classEnrollmentRepository, never()).findScheduleConflicts(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should throw error when class not found")
    void enrollAfterPayment_ClassNotFound_ShouldThrowError() {
        // Given
        Long classId = 999L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollAfterPayment(classId, studentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class not found");

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw error when student not found")
    void enrollAfterPayment_StudentNotFound_ShouldThrowError() {
        // Given
        Long classId = 1L;
        Long studentId = 999L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.enrollAfterPayment(classId, studentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student not found");

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle capacity at exact limit")
    void enrollAfterPayment_CapacityAtLimit_ShouldEnroll() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId))
                .thenReturn(29); // One spot left
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then - should enroll successfully (29 < 30)
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
        verify(notificationService).notifyEnrolledNewClass(any(), any(), any());
    }

    @Test
    @DisplayName("Should handle empty schedule gracefully")
    void enrollAfterPayment_EmptySchedule_ShouldEnroll() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.emptyList()); // No schedule
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then - should enroll successfully
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
        verify(classEnrollmentRepository).findScheduleConflicts(
                eq(testStudent.getId()),
                eq(testSemester.getId()),
                eq(Collections.emptySet()), // Empty dows
                eq(Collections.emptySet())  // Empty slotIds
        );
    }

    @Test
    @DisplayName("Should be idempotent when called multiple times")
    void enrollAfterPayment_CalledMultipleTimes_ShouldBeIdempotent() {
        // Given
        Long classId = 1L;
        Long studentId = 1L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(testStudent));

        // First call: not enrolled
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent))
                .thenReturn(false)
                .thenReturn(true); // Second call: already enrolled

        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(15);
        when(classScheduleRepository.findByClazz_Id(classId))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any(ClassEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When - call twice
        enrollmentService.enrollAfterPayment(classId, studentId);
        enrollmentService.enrollAfterPayment(classId, studentId);

        // Then - should only enroll once
        verify(classEnrollmentRepository, times(1)).save(any(ClassEnrollment.class));
        verify(notificationService, times(1)).notifyEnrolledNewClass(any(), any(), any());
    }
}
