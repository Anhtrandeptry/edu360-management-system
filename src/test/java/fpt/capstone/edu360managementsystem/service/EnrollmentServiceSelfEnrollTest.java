package fpt.capstone.edu360managementsystem.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Semester;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import fpt.capstone.edu360managementsystem.repository.ClassEnrollmentRepository;
import fpt.capstone.edu360managementsystem.repository.ClassScheduleRepository;
import fpt.capstone.edu360managementsystem.repository.ClassSessionRepository;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.PaymentRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;

/**
 * Unit tests for EnrollmentService.selfEnroll()
 * 
 * Test coverage:
 * - Free class (totalFee = 0) → enroll without payment check
 * - Paid class with payment → enroll successfully
 * - Paid class without payment → reject
 * - Class not PUBLIC → reject
 * - Class full → reject
 * - Already enrolled → reject
 * - Schedule conflict → reject
 * - Student not found → reject
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService - selfEnroll()")
class EnrollmentServiceSelfEnrollTest {

    @Mock private ClazzRepository clazzRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Clazz testClass;
    private Student testStudent;
    private User testUser;
    private Semester testSemester;

    @BeforeEach
    void setUp() {
        testSemester = TestDataBuilder.semester().id(1L).build();

        testUser = TestDataBuilder.user()
                .id(10L)
                .fullName("Student User")
                .build();

        testStudent = TestDataBuilder.student()
                .id(1L)
                .user(testUser)
                .build();

        testClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .maxStudents(30)
                .pricePerSession(100000L) // 100,000 VND per session
                .status(ClassStatus.PUBLIC)
                .semester(testSemester)
                .build();
    }

    // ==================== FREE CLASS TESTS ====================

    @Test
    @DisplayName("Should enroll without payment check when class is free (pricePerSession = 0)")
    void selfEnroll_FreeClass_ShouldEnrollWithoutPaymentCheck() {
        // Given - Free class
        testClass.setPricePerSession(0L);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(5);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then - Should NOT check payment
        verify(paymentRepository, never()).existsByClazz_IdAndStudent_IdAndStatus(any(), any(), any());
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should enroll without payment check when class has no sessions")
    void selfEnroll_NoSessions_ShouldEnrollWithoutPaymentCheck() {
        // Given - Class has price but no sessions → totalFee = 0
        testClass.setPricePerSession(100000L);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(0L); // No sessions
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(5);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then - Should NOT check payment (totalFee = 0)
        verify(paymentRepository, never()).existsByClazz_IdAndStudent_IdAndStatus(any(), any(), any());
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should enroll without payment check when pricePerSession is null")
    void selfEnroll_NullPrice_ShouldEnrollWithoutPaymentCheck() {
        // Given - pricePerSession is null
        testClass.setPricePerSession(null);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(5);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then - Should NOT check payment (unitPrice = 0 when null)
        verify(paymentRepository, never()).existsByClazz_IdAndStudent_IdAndStatus(any(), any(), any());
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    // ==================== PAID CLASS TESTS ====================

    @Test
    @DisplayName("Should enroll successfully when paid class and payment confirmed")
    void selfEnroll_PaidClassWithPayment_ShouldEnroll() {
        // Given - Paid class, student has paid
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L); // totalFee = 1,000,000
        when(paymentRepository.existsByClazz_IdAndStudent_IdAndStatus(classId, testStudent.getId(), PaymentStatus.PAID))
                .thenReturn(true); // Payment confirmed
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(5);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then
        verify(paymentRepository).existsByClazz_IdAndStudent_IdAndStatus(classId, testStudent.getId(), PaymentStatus.PAID);
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should reject when paid class but no payment")
    void selfEnroll_PaidClassWithoutPayment_ShouldThrowException() {
        // Given - Paid class, student has NOT paid
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(paymentRepository.existsByClazz_IdAndStudent_IdAndStatus(classId, testStudent.getId(), PaymentStatus.PAID))
                .thenReturn(false); // Not paid!

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa thanh toán");

        verify(classEnrollmentRepository, never()).save(any());
    }

    // ==================== CLASS STATUS TESTS ====================

    @Test
    @DisplayName("Should reject when class is not PUBLIC")
    void selfEnroll_ClassNotPublic_ShouldThrowException() {
        // Given - Class is DRAFT
        testClass.setStatus(ClassStatus.DRAFT);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không ở trạng thái PUBLIC");

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject when class not found")
    void selfEnroll_ClassNotFound_ShouldThrowException() {
        // Given
        Long classId = 999L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class not found");
    }

    // ==================== STUDENT VALIDATION TESTS ====================

    @Test
    @DisplayName("Should reject when student profile not found")
    void selfEnroll_StudentNotFound_ShouldThrowException() {
        // Given
        Long classId = 1L;
        Long userId = 999L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Student profile not found");
    }

    // ==================== CAPACITY TESTS ====================

    @Test
    @DisplayName("Should reject when class is full")
    void selfEnroll_ClassFull_ShouldThrowException() {
        // Given - Free class but full
        testClass.setPricePerSession(0L);
        testClass.setMaxStudents(30);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(30); // Full!

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class is full");

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should enroll when class is at capacity - 1")
    void selfEnroll_ClassAlmostFull_ShouldEnroll() {
        // Given - Free class, 1 spot left
        testClass.setPricePerSession(0L);
        testClass.setMaxStudents(30);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(29); // 1 spot left
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    // ==================== DUPLICATE ENROLLMENT TESTS ====================

    @Test
    @DisplayName("Should reject when already enrolled")
    void selfEnroll_AlreadyEnrolled_ShouldThrowException() {
        // Given - Free class, student already enrolled
        testClass.setPricePerSession(0L);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(10);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(true); // Already enrolled!

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already enrolled");

        verify(classEnrollmentRepository, never()).save(any());
    }

    // ==================== SCHEDULE CONFLICT TESTS ====================

    @Test
    @DisplayName("Should reject when schedule conflicts exist")
    void selfEnroll_ScheduleConflict_ShouldThrowException() {
        // Given - Free class with schedule conflict, MUST have semester for conflict check
        testClass.setPricePerSession(0L);
        testClass.setSemester(TestDataBuilder.semester().id(1L).build()); // Required for conflict check!
        Long classId = 1L;
        Long userId = 10L;

        TimeSlot timeSlot = TestDataBuilder.timeSlot().id(1L).build();
        ClassSchedule schedule = ClassSchedule.builder()
                .id(1L)
                .clazz(testClass)
                .dayOfWeek(2) // Tuesday
                .timeSlot(timeSlot)
                .build();

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(10);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(List.of(schedule));
        ClassEnrollment conflictingEnrollment = ClassEnrollment.builder()
                .id(99L)
                .clazz(testClass)
                .student(testStudent)
                .build();
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(List.of(conflictingEnrollment)); // Has conflict!

        // When & Then
        assertThatThrownBy(() -> enrollmentService.selfEnroll(classId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Schedule conflict with your other enrolled classes");

        verify(classEnrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip conflict check when class has no semester")
    void selfEnroll_NoSemester_ShouldSkipConflictCheck() {
        // Given - Free class with no semester
        testClass.setPricePerSession(0L);
        testClass.setSemester(null);
        Long classId = 1L;
        Long userId = 10L;

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(10);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then - Should NOT check schedule conflicts
        verify(classScheduleRepository, never()).findByClazz_Id(any());
        verify(classEnrollmentRepository, never()).findScheduleConflicts(any(), any(), any(), any());
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should enroll when no schedule conflicts")
    void selfEnroll_NoConflicts_ShouldEnroll() {
        // Given - Free class with schedules but no conflicts
        testClass.setPricePerSession(0L);
        Long classId = 1L;
        Long userId = 10L;

        TimeSlot timeSlot = TestDataBuilder.timeSlot().id(1L).build();
        ClassSchedule schedule = ClassSchedule.builder()
                .id(1L)
                .clazz(testClass)
                .dayOfWeek(2)
                .timeSlot(timeSlot)
                .build();

        when(clazzRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(studentRepository.findByUser_Id(userId)).thenReturn(Optional.of(testStudent));
        when(classSessionRepository.countByClazz_Id(classId)).thenReturn(10L);
        when(classEnrollmentRepository.countByClazz_Id(classId)).thenReturn(10);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, testStudent)).thenReturn(false);
        when(classScheduleRepository.findByClazz_Id(classId)).thenReturn(List.of(schedule));
        when(classEnrollmentRepository.findScheduleConflicts(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList()); // No conflicts
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        enrollmentService.selfEnroll(classId, userId);

        // Then
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }
}
