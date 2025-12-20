package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.registerTeacher() method
 * 
 * Tests cover:
 * - Successful teacher registration with multiple subjects
 * - Email already exists validation
 * - Empty subject list validation
 * - Subject not found validation
 * - Unavailable subject validation
 * - Username generation and uniqueness
 * - Password generation and encoding
 * - Email notification sending
 * - Email sending failure handling
 * - Teacher-Subject relationship setup
 * - Role assignment
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTeacherTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterTeacherRequest validRequest;
    private Role teacherRole;
    private List<Subject> validSubjects;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
        teacherRole = createRole(ERole.ROLE_TEACHER);
        validSubjects = createValidSubjects();
    }

    @Test
    void registerTeacher_ValidRequest_ShouldCreateTeacherAndSendEmail() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(validSubjects);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Teacher account created successfully!");
        assertThat(messageResponse.getMessage()).contains("Username:");

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getFullName()).isEqualTo(validRequest.getFullName());
        assertThat(savedUser.getEmail()).isEqualTo(validRequest.getEmail());
        assertThat(savedUser.getPhoneNumber()).isEqualTo(validRequest.getPhoneNumber());
        assertThat(savedUser.getRoles()).contains(teacherRole);
        assertThat(savedUser.getUsername()).isEqualTo("anv"); // Generated from "Nguyen Van A" -> "anv"

        // Verify teacher creation
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherRepository).save(teacherCaptor.capture());
        Teacher savedTeacher = teacherCaptor.getValue();
        assertThat(savedTeacher.getUser()).isEqualTo(savedUser);
        assertThat(savedTeacher.getSubject()).isEqualTo(validSubjects.get(0)); // Primary subject
        assertThat(savedTeacher.getSubjects()).containsAll(validSubjects); // All subjects

        // Verify email sent
        verify(emailService).sendSimpleMessage(
            eq(validRequest.getEmail()),
            eq("Tài khoản giáo viên đã được tạo trên Edu360"),
            contains("Tài khoản giáo viên của bạn đã được tạo")
        );

        // Verify password encoding
        verify(encoder).encode(anyString());
    }

    @Test
    void registerTeacher_EmailAlreadyExists_ShouldReturnBadRequest() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Error: Email đã được sử dụng bởi giáo viên khác!");

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void registerTeacher_EmptySubjectList_ShouldReturnBadRequest() {
        // Given
        validRequest.setSubjectIds(new ArrayList<>());
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Error: At least one subject is required");

        // Verify user was saved but teacher was not (due to subject validation failure)
        verify(userRepository).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void registerTeacher_NullSubjectList_ShouldReturnBadRequest() {
        // Given
        validRequest.setSubjectIds(null);
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Error: At least one subject is required");

        // Verify user was saved but teacher was not (due to subject validation failure)
        verify(userRepository).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void registerTeacher_SubjectNotFound_ShouldReturnBadRequest() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        // Return only 1 subject instead of 2 requested
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(Arrays.asList(validSubjects.get(0)));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Error: Some subject ids were not found");

        // Verify user was saved but teacher was not (due to subject validation failure)
        verify(userRepository).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void registerTeacher_UnavailableSubject_ShouldReturnBadRequest() {
        // Given
        Subject unavailableSubject = createSubject(1L, "Math", SubjectStatus.UNAVAILABLE);
        Subject availableSubject = createSubject(2L, "Physics", SubjectStatus.AVAILABLE);
        List<Subject> mixedSubjects = Arrays.asList(unavailableSubject, availableSubject);

        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(mixedSubjects);

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Error: Subject 1 is unavailable");

        // Verify user was saved but teacher was not (due to subject validation failure)
        verify(userRepository).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void registerTeacher_UsernameAlreadyExists_ShouldGenerateUniqueUsername() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        // First username exists, second one is unique
        when(userRepository.existsByUsername("anv")).thenReturn(true);
        when(userRepository.existsByUsername("anv1")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(validSubjects);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify username uniqueness check
        verify(userRepository).existsByUsername("anv");
        verify(userRepository).existsByUsername("anv1");
        
        // Verify user saved with unique username
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("anv1");
    }

    @Test
    void registerTeacher_EmailSendingFails_ShouldStillCreateAccountWithWarning() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(validSubjects);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service to throw exception
        doThrow(new MailException("SMTP server unavailable") {}).when(emailService)
            .sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Teacher created successfully but failed to send email");

        // Verify account was still created
        verify(userRepository).save(any(User.class));
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test
    void registerTeacher_TeacherRoleNotFound_ShouldThrowException() {
        // Given
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.empty());

        // When & Then
        try {
            authService.registerTeacher(validRequest);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Role ROLE_TEACHER not found");
        }

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }

    @Test
    void registerTeacher_MultipleSubjects_ShouldSetupCorrectRelationships() {
        // Given
        Subject math = createSubject(1L, "Math", SubjectStatus.AVAILABLE);
        Subject physics = createSubject(2L, "Physics", SubjectStatus.AVAILABLE);
        Subject chemistry = createSubject(3L, "Chemistry", SubjectStatus.AVAILABLE);
        List<Subject> multipleSubjects = Arrays.asList(math, physics, chemistry);
        
        validRequest.setSubjectIds(Arrays.asList(1L, 2L, 3L));

        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(multipleSubjects);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify teacher creation with multiple subjects
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherRepository).save(teacherCaptor.capture());
        Teacher savedTeacher = teacherCaptor.getValue();
        
        // Primary subject should be the first one
        assertThat(savedTeacher.getSubject()).isEqualTo(math);
        
        // All subjects should be in the collection
        assertThat(savedTeacher.getSubjects()).hasSize(3);
        assertThat(savedTeacher.getSubjects()).containsExactlyInAnyOrder(math, physics, chemistry);
    }

    @Test
    void registerTeacher_EmptyFullName_ShouldGenerateFallbackUsername() {
        // Given
        validRequest.setFullName(""); // Empty name
        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(validSubjects);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify user was created with fallback username pattern
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).startsWith("user"); // Fallback pattern
    }

    @Test
    void registerTeacher_SingleSubject_ShouldSetupCorrectly() {
        // Given
        validRequest.setSubjectIds(Arrays.asList(1L)); // Single subject
        List<Subject> singleSubject = Arrays.asList(validSubjects.get(0));

        when(userRepository.existsTeacherEmail(validRequest.getEmail(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsTeacherPhone(validRequest.getPhoneNumber(), null, ERole.ROLE_TEACHER)).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(subjectRepository.findAllById(validRequest.getSubjectIds())).thenReturn(singleSubject);
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerTeacher(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify teacher creation with single subject
        ArgumentCaptor<Teacher> teacherCaptor = ArgumentCaptor.forClass(Teacher.class);
        verify(teacherRepository).save(teacherCaptor.capture());
        Teacher savedTeacher = teacherCaptor.getValue();
        
        assertThat(savedTeacher.getSubject()).isEqualTo(singleSubject.get(0));
        assertThat(savedTeacher.getSubjects()).hasSize(1);
        assertThat(savedTeacher.getSubjects()).contains(singleSubject.get(0));
    }

    // Helper methods
    private RegisterTeacherRequest createValidRequest() {
        RegisterTeacherRequest request = new RegisterTeacherRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("teacher@test.com");
        request.setPhoneNumber("0123456789");
        request.setSubjectIds(Arrays.asList(1L, 2L));
        return request;
    }

    private Role createRole(ERole roleName) {
        Role role = new Role();
        role.setId(1);
        role.setName(roleName);
        return role;
    }

    private List<Subject> createValidSubjects() {
        Subject math = createSubject(1L, "Math", SubjectStatus.AVAILABLE);
        Subject physics = createSubject(2L, "Physics", SubjectStatus.AVAILABLE);
        return Arrays.asList(math, physics);
    }

    private Subject createSubject(Long id, String name, SubjectStatus status) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        subject.setStatus(status);
        subject.setTeachers(new HashSet<>()); // Initialize collection
        return subject;
    }
}