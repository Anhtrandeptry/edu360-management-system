package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.BulkEnrollRequest;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EnrollmentService.enrollBulk()
 * 
 * Tests bulk enrollment with partial success scenarios:
 * - Capacity checking
 * - Schedule conflict detection
 * - Duplicate enrollment prevention
 * - Student validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService - enrollBulk()")
class EnrollmentServiceEnrollBulkTest {

    @Mock private ClazzRepository clazzRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Clazz testClass;
    private Teacher testTeacher;
    private Student student1;
    private Student student2;
    private Student student3;
    private Semester testSemester;
    private ClassSchedule schedule1;
    private TimeSlot timeSlot1;

    @BeforeEach
    void setUp() {
        testSemester = TestDataBuilder.semester().id(1L).build();
        
        testTeacher = TestDataBuilder.teacher().id(1L).build();
        
        testClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .teacher(testTeacher)
                .maxStudents(3)
                .status(ClassStatus.PUBLIC)
                .semester(testSemester)
                .build();

        student1 = TestDataBuilder.student().id(1L).build();
        student2 = TestDataBuilder.student().id(2L).build();
        student3 = TestDataBuilder.student().id(3L).build();

        timeSlot1 = TestDataBuilder.timeSlot().id(1L).build();
        
        schedule1 = ClassSchedule.builder()
                .id(1L)
                .clazz(testClass)
                .dayOfWeek(2) // Tuesday
                .timeSlot(timeSlot1)
                .build();
    }

    // Helper method to create BulkEnrollRequest
    private BulkEnrollRequest createBulkEnrollRequest(List<Long> studentIds) {
        BulkEnrollRequest request = new BulkEnrollRequest();
        request.setStudentIds(studentIds);
        return request;
    }

    @Test
    @DisplayName("Should enroll all students when all valid and capacity available")
    void enrollBulk_AllValid_ShouldEnrollAll() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 2L, 3L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
        
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        when(classEnrollmentRepository.findScheduleConflicts(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(1L)).isEqualTo("OK");
        assertThat(result.get(2L)).isEqualTo("OK");
        assertThat(result.get(3L)).isEqualTo("OK");
        verify(classEnrollmentRepository, times(3)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should reject when class not found")
    void enrollBulk_ClassNotFound_ShouldThrowException() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(List.of(1L));
        when(clazzRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> enrollmentService.enrollBulk(999L, request, 1L, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class not found");
    }

    @Test
    @DisplayName("Should reject when not owner and not admin")
    void enrollBulk_NotOwnerNotAdmin_ShouldThrowException() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(List.of(1L));
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));

        // When/Then - userId 999 is not the teacher's userId (1L) and isAdmin=false
        assertThatThrownBy(() -> enrollmentService.enrollBulk(1L, request, 999L, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    @DisplayName("Should handle partial success when class reaches capacity")
    void enrollBulk_ClassFull_ShouldRejectRemaining() {
        // Given - class has capacity 3, already has 2 students
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 2L, 3L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(2); // 2 already enrolled
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        // Students 2 and 3 won't be processed due to capacity
        
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        when(classEnrollmentRepository.findScheduleConflicts(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(1L)).isEqualTo("OK"); // First one enrolled
        assertThat(result.get(2L)).isEqualTo("Class is full"); // Capacity reached
        assertThat(result.get(3L)).isEqualTo("Class is full");
        verify(classEnrollmentRepository, times(1)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should skip student not found")
    void enrollBulk_StudentNotFound_ShouldSkipWithMessage() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 999L, 2L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        when(classEnrollmentRepository.findScheduleConflicts(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(1L)).isEqualTo("OK");
        assertThat(result.get(999L)).isEqualTo("Student not found");
        assertThat(result.get(2L)).isEqualTo("OK");
        verify(classEnrollmentRepository, times(2)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should skip already enrolled students")
    void enrollBulk_AlreadyEnrolled_ShouldSkipWithMessage() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 2L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        
        // Student 1 already enrolled
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student1)).thenReturn(true);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student2)).thenReturn(false);
        
        when(classEnrollmentRepository.findScheduleConflicts(eq(2L), eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo("Already enrolled");
        assertThat(result.get(2L)).isEqualTo("OK");
        verify(classEnrollmentRepository, times(1)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should skip students with schedule conflicts")
    void enrollBulk_ScheduleConflict_ShouldSkipWithMessage() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 2L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        
        // Student 1 has conflict
        ClassEnrollment conflictEnrollment = ClassEnrollment.builder().id(99L).build();
        when(classEnrollmentRepository.findScheduleConflicts(eq(1L), eq(1L), any(), any()))
                .thenReturn(List.of(conflictEnrollment)); // Conflict found
        when(classEnrollmentRepository.findScheduleConflicts(eq(2L), eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo("Schedule conflict");
        assertThat(result.get(2L)).isEqualTo("OK");
        verify(classEnrollmentRepository, times(1)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should handle mixed scenarios - some succeed, some fail")
    void enrollBulk_MixedScenarios_ShouldHandlePartialSuccess() {
        // Given - 5 students with different issues
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(1L, 2L, 999L, 3L, 4L));
        
        Student student4 = TestDataBuilder.student().id(4L).build();
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(1); // 1 already enrolled, capacity 3
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.findById(999L)).thenReturn(Optional.empty()); // Not found
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
        // Student 4: checked but class full before enrollment
        lenient().when(studentRepository.findById(4L)).thenReturn(Optional.of(student4));
        
        // Student 1: already enrolled
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student1)).thenReturn(true);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student2)).thenReturn(false);
        when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student3)).thenReturn(false);
        // Student 4: class full before checking existsByClazzAndStudent
        lenient().when(classEnrollmentRepository.existsByClazzAndStudent(testClass, student4)).thenReturn(false);
        
        // Student 2: OK
        when(classEnrollmentRepository.findScheduleConflicts(eq(2L), eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        // Student 3: OK
        when(classEnrollmentRepository.findScheduleConflicts(eq(3L), eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        // Student 4: would be OK but class full after student 2 and 3 (not checked due to capacity)
        lenient().when(classEnrollmentRepository.findScheduleConflicts(eq(4L), eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then
        assertThat(result).hasSize(5);
        assertThat(result.get(1L)).isEqualTo("Already enrolled");
        assertThat(result.get(2L)).isEqualTo("OK");
        assertThat(result.get(999L)).isEqualTo("Student not found");
        assertThat(result.get(3L)).isEqualTo("OK");
        assertThat(result.get(4L)).isEqualTo("Class is full"); // Capacity reached after 2 enrollments
        verify(classEnrollmentRepository, times(2)).save(any(ClassEnrollment.class));
    }

    @Test
    @DisplayName("Should maintain order in result map")
    void enrollBulk_ShouldMaintainOrderInResult() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(Arrays.asList(3L, 1L, 2L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(studentRepository.findById(2L)).thenReturn(Optional.of(student2));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student3));
        
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        when(classEnrollmentRepository.findScheduleConflicts(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 1L, true);

        // Then - order should match request order
        assertThat(result).hasSize(3);
        List<Long> keys = new ArrayList<>(result.keySet());
        assertThat(keys).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("Should allow admin to enroll regardless of teacher ownership")
    void enrollBulk_AdminUser_ShouldBypassOwnershipCheck() {
        // Given
        BulkEnrollRequest request = createBulkEnrollRequest(List.of(1L));
        
        when(clazzRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(classScheduleRepository.findByClazz_Id(1L)).thenReturn(List.of(schedule1));
        when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));
        when(classEnrollmentRepository.existsByClazzAndStudent(any(), any())).thenReturn(false);
        when(classEnrollmentRepository.findScheduleConflicts(anyLong(), anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(classEnrollmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When - userId 999 is not owner but isAdmin=true
        Map<Long, String> result = enrollmentService.enrollBulk(1L, request, 999L, true);

        // Then
        assertThat(result.get(1L)).isEqualTo("OK");
        verify(classEnrollmentRepository).save(any(ClassEnrollment.class));
    }
}
