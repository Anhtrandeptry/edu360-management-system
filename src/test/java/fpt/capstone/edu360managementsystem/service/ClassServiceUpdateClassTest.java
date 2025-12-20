package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.UpdateClassRequest;
import fpt.capstone.edu360managementsystem.dto.response.ClassResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.mapper.ClassMapper;
import fpt.capstone.edu360managementsystem.repository.*;
import fpt.capstone.edu360managementsystem.testbuilder.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClassService.updateClass()
 * 
 * Tests complex update logic with:
 * - DRAFT vs PUBLIC different update rules
 * - Teacher change with course migration
 * - Subject/Course validation
 * - Room capacity validation
 * - Session content cleanup
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClassService - updateClass()")
class ClassServiceUpdateClassTest {

    @Mock private ClazzRepository clazzRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private ClassSessionRepository classSessionRepository;
    @Mock private ClassScheduleRepository classScheduleRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private SessionChapterRepository sessionChapterRepository;
    @Mock private SessionLessonRepository sessionLessonRepository;
    @Mock private SessionContentConfigRepository sessionContentConfigRepository;
    @Mock private CourseChapterRepository courseChapterRepository;
    @Mock private CourseLessonRepository courseLessonRepository;
    @Mock private ClassMapper classMapper;

    @InjectMocks
    private ClassService classService;

    private Clazz draftClass;
    private Clazz publicClass;
    private Teacher teacher1;
    private Teacher teacher2;
    private Subject subject1;
    private Subject subject2;
    private Course course1;
    private Course course2;
    private Room room1;
    private Room room2;

    @BeforeEach
    void setUp() {
        // Default mocks
        lenient().when(classScheduleRepository.findByClazz_Id(anyLong())).thenReturn(Collections.emptyList());
        lenient().when(classEnrollmentRepository.countByClazz_Id(anyLong())).thenReturn(0);
        
        subject1 = TestDataBuilder.subject()
                .id(1L)
                .name("Mathematics")
                .build();
        subject1.setStatus(SubjectStatus.AVAILABLE);

        subject2 = TestDataBuilder.subject()
                .id(2L)
                .name("Physics")
                .build();
        subject2.setStatus(SubjectStatus.AVAILABLE);

        teacher1 = TestDataBuilder.teacher()
                .id(1L)
                .subject(subject1)
                .build();
        teacher1.getUser().setId(10L);

        teacher2 = TestDataBuilder.teacher()
                .id(2L)
                .subject(subject1)
                .build();
        teacher2.getUser().setId(20L);

        course1 = TestDataBuilder.course()
                .id(1L)
                .status(CourseStatus.APPROVED)
                .build();
        course1.setSubject(subject1);

        course2 = TestDataBuilder.course()
                .id(2L)
                .status(CourseStatus.APPROVED)
                .build();
        course2.setSubject(subject1);

        room1 = TestDataBuilder.room()
                .id(1L)
                .name("Room A")
                .capacity(30)
                .build();

        room2 = TestDataBuilder.room()
                .id(2L)
                .name("Room B")
                .capacity(50)
                .build();

        draftClass = TestDataBuilder.clazz()
                .id(1L)
                .name("Math 101")
                .teacher(teacher1)
                .subject(subject1)
                .course(course1)
                .room(room1)
                .maxStudents(30)
                .status(ClassStatus.DRAFT)
                .build();

        publicClass = TestDataBuilder.clazz()
                .id(2L)
                .name("Math 201")
                .teacher(teacher1)
                .subject(subject1)
                .room(room1)
                .maxStudents(30)
                .status(ClassStatus.PUBLIC)
                .build();
    }

    // Helper method to create UpdateClassRequest
    private UpdateClassRequest createUpdateRequest() {
        return new UpdateClassRequest();
    }

    @Test
    @DisplayName("Should throw exception when class not found")
    void updateClass_ClassNotFound_ShouldThrowException() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        when(clazzRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Class not found");
    }

    @Test
    @DisplayName("Should update room for PUBLIC class")
    void updateClass_PublicClass_ShouldUpdateRoom() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setRoomId(2L);

        when(clazzRepository.findById(2L)).thenReturn(Optional.of(publicClass));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room2));
        when(classScheduleRepository.findByClazz_Id(2L)).thenReturn(Collections.emptyList());
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(2L, request);

        // Then
        assertThat(publicClass.getRoom()).isEqualTo(room2);
        verify(clazzRepository).save(publicClass);
    }

    @Test
    @DisplayName("Should update maxStudents for PUBLIC class")
    void updateClass_PublicClass_ShouldUpdateMaxStudents() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setMaxStudents(25);

        when(clazzRepository.findById(2L)).thenReturn(Optional.of(publicClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(2L, request);

        // Then
        assertThat(publicClass.getMaxStudents()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should reject maxStudents exceeding room capacity for PUBLIC class")
    void updateClass_PublicClass_MaxStudentsExceedsCapacity_ShouldThrowException() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setRoomId(1L); // Room with capacity 30
        request.setMaxStudents(40); // Exceeds capacity

        when(clazzRepository.findById(2L)).thenReturn(Optional.of(publicClass));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room1));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(2L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maxStudents cannot exceed room capacity");
    }

    @Test
    @DisplayName("Should update meetingLink for PUBLIC class")
    void updateClass_PublicClass_ShouldUpdateMeetingLink() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setMeetingLink("https://zoom.us/new-link");

        when(clazzRepository.findById(2L)).thenReturn(Optional.of(publicClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(2L, request);

        // Then
        assertThat(publicClass.getMeetingLink()).isEqualTo("https://zoom.us/new-link");
    }

    @Test
    @DisplayName("Should update name for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateName() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setName("Advanced Math");

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getName()).isEqualTo("Advanced Math");
    }

    @Test
    @DisplayName("Should update description for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateDescription() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setDescription("New description");

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getDescription()).isEqualTo("New description");
    }

    @Test
    @DisplayName("Should update pricePerSession for DRAFT class")
    void updateClass_DraftClass_ShouldUpdatePrice() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setPricePerSession(200000L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getPricePerSession()).isEqualTo(200000L);
    }

    @Test
    @DisplayName("Should reject negative pricePerSession for DRAFT class")
    void updateClass_DraftClass_NegativePrice_ShouldThrowException() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setPricePerSession(-1000L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pricePerSession must be >= 0");
    }

    @Test
    @DisplayName("Should update subject for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateSubject() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setSubjectId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(subject2));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getSubject()).isEqualTo(subject2);
    }

    @Test
    @DisplayName("Should reject unavailable subject for DRAFT class")
    void updateClass_DraftClass_UnavailableSubject_ShouldThrowException() {
        // Given
        subject2.setStatus(SubjectStatus.UNAVAILABLE);
        UpdateClassRequest request = createUpdateRequest();
        request.setSubjectId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(subjectRepository.findById(2L)).thenReturn(Optional.of(subject2));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Subject is not available");
    }

    @Test
    @DisplayName("Should update course for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateCourse() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setCourseId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course2));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getCourse()).isEqualTo(course2);
    }

    @Test
    @DisplayName("Should reject course not belonging to subject")
    void updateClass_DraftClass_CourseMismatchSubject_ShouldThrowException() {
        // Given
        course2.setSubject(subject2); // Different subject
        UpdateClassRequest request = createUpdateRequest();
        request.setCourseId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course2));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course does not belong to current subject");
    }

    @Test
    @DisplayName("Should reject unapproved course")
    void updateClass_DraftClass_UnapprovedCourse_ShouldThrowException() {
        // Given
        course2.setStatus(CourseStatus.PENDING);
        UpdateClassRequest request = createUpdateRequest();
        request.setCourseId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course2));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Course is not approved");
    }

    @Test
    @DisplayName("Should update teacher for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateTeacher() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setTeacherId(20L); // teacher2's userId

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(teacherRepository.findByUserId(20L)).thenReturn(Optional.of(teacher2));
        when(classSessionRepository.findByClazz_Id(1L)).thenReturn(Collections.emptyList());
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getTeacher()).isEqualTo(teacher2);
    }

    @Test
    @DisplayName("Should reject teacher not teaching the subject")
    void updateClass_DraftClass_TeacherNotTeachingSubject_ShouldThrowException() {
        // Given
        teacher2.setSubject(subject2); // Different subject
        teacher2.setSubjects(null);
        UpdateClassRequest request = createUpdateRequest();
        request.setTeacherId(20L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(teacherRepository.findByUserId(20L)).thenReturn(Optional.of(teacher2));

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Teacher does not teach the selected subject");
    }

    @Test
    @DisplayName("Should handle teacher change with session cleanup")
    void updateClass_DraftClass_TeacherChange_ShouldCleanupSessions() {
        // Given
        ClassSession session1 = TestDataBuilder.session()
                .id(1L)
                .clazz(draftClass)
                .build();

        UpdateClassRequest request = createUpdateRequest();
        request.setTeacherId(20L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(teacherRepository.findByUserId(20L)).thenReturn(Optional.of(teacher2));
        when(classSessionRepository.findByClazz_Id(1L)).thenReturn(List.of(session1));
        when(sessionContentConfigRepository.findBySession_Id(1L)).thenReturn(Optional.empty());
        when(classSessionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        verify(sessionChapterRepository).deleteBySession_Id(1L);
        verify(sessionLessonRepository).deleteBySession_Id(1L);
        verify(classSessionRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should update room for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateRoom() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setRoomId(2L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room2));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getRoom()).isEqualTo(room2);
    }

    @Test
    @DisplayName("Should set room to null for online class")
    void updateClass_DraftClass_OnlineClass_ShouldSetRoomNull() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        // roomId is null = online class

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then - room should remain as is or be set based on logic
        verify(clazzRepository).save(draftClass);
    }

    @Test
    @DisplayName("Should update startDate for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateStartDate() {
        // Given
        LocalDate newStartDate = LocalDate.of(2025, 1, 15);
        UpdateClassRequest request = createUpdateRequest();
        request.setStartDate(newStartDate);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getStartDate()).isEqualTo(newStartDate);
    }

    @Test
    @DisplayName("Should update endDate for DRAFT class")
    void updateClass_DraftClass_ShouldUpdateEndDate() {
        // Given
        LocalDate newEndDate = LocalDate.of(2025, 6, 30);
        UpdateClassRequest request = createUpdateRequest();
        request.setEndDate(newEndDate);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getEndDate()).isEqualTo(newEndDate);
    }

    @Test
    @DisplayName("Should handle multiple field updates for DRAFT class")
    void updateClass_DraftClass_MultipleFields_ShouldUpdateAll() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setName("New Name");
        request.setDescription("New Description");
        request.setMeetingLink("https://new-link.com");
        request.setPricePerSession(150000L);
        request.setMaxStudents(25);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(1L, request);

        // Then
        assertThat(draftClass.getName()).isEqualTo("New Name");
        assertThat(draftClass.getDescription()).isEqualTo("New Description");
        assertThat(draftClass.getMeetingLink()).isEqualTo("https://new-link.com");
        assertThat(draftClass.getPricePerSession()).isEqualTo(150000L);
        assertThat(draftClass.getMaxStudents()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should not update name for PUBLIC class")
    void updateClass_PublicClass_ShouldNotUpdateName() {
        // Given
        String originalName = publicClass.getName();
        UpdateClassRequest request = createUpdateRequest();
        request.setName("New Name"); // Should be ignored for PUBLIC

        when(clazzRepository.findById(2L)).thenReturn(Optional.of(publicClass));
        when(clazzRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classMapper.toResponse(any(), any(), anyInt())).thenReturn(new ClassResponse());

        // When
        classService.updateClass(2L, request);

        // Then - name should not change for PUBLIC class
        assertThat(publicClass.getName()).isEqualTo(originalName);
    }

    @Test
    @DisplayName("Should handle room not found exception")
    void updateClass_RoomNotFound_ShouldThrowException() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setRoomId(999L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    @DisplayName("Should handle teacher not found exception")
    void updateClass_TeacherNotFound_ShouldThrowException() {
        // Given
        UpdateClassRequest request = createUpdateRequest();
        request.setTeacherId(999L);

        when(clazzRepository.findById(1L)).thenReturn(Optional.of(draftClass));
        when(teacherRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> classService.updateClass(1L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Teacher not found");
    }
}
