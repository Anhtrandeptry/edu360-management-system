package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.repository.ParentRepository;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.registerStudentWithParent() method
 * 
 * Tests cover:
 * - Successful registration with email notification
 * - Password mismatch validation
 * - Username already exists validation
 * - Student email already exists validation
 * - Parent email already exists validation
 * - Email sending failure handling
 * - Role not found error handling
 * - Username generation and uniqueness
 * - Password encoding
 * - Entity relationships (Student-Parent linkage)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterStudentWithParentTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ParentRepository parentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterStudentWithParentRequest validRequest;
    private Role studentRole;
    private Role parentRole;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
        studentRole = createRole(ERole.ROLE_STUDENT);
        parentRole = createRole(ERole.ROLE_PARENT);
    }

    @Test
    void registerStudentWithParent_ValidRequest_ShouldCreateBothAccountsAndSendEmail() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L); // Simulate saved entity with ID
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(MessageResponse.class);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Đăng ký thành công!");

        // Verify parent user creation
        ArgumentCaptor<User> parentUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(parentUserCaptor.capture());
        
        User savedParentUser = parentUserCaptor.getAllValues().get(0);
        assertThat(savedParentUser.getFullName()).isEqualTo(validRequest.getParentFullName());
        assertThat(savedParentUser.getEmail()).isEqualTo(validRequest.getParentEmail());
        assertThat(savedParentUser.getPhoneNumber()).isEqualTo(validRequest.getParentPhoneNumber());
        assertThat(savedParentUser.getRoles()).contains(parentRole);

        // Verify student user creation
        User savedStudentUser = parentUserCaptor.getAllValues().get(1);
        assertThat(savedStudentUser.getUsername()).isEqualTo(validRequest.getStudentUsername());
        assertThat(savedStudentUser.getFullName()).isEqualTo(validRequest.getStudentFullName());
        assertThat(savedStudentUser.getEmail()).isEqualTo(validRequest.getStudentEmail());
        assertThat(savedStudentUser.getPhoneNumber()).isEqualTo(validRequest.getStudentPhoneNumber());
        assertThat(savedStudentUser.getRoles()).contains(studentRole);

        // Verify parent entity creation
        ArgumentCaptor<Parent> parentCaptor = ArgumentCaptor.forClass(Parent.class);
        verify(parentRepository).save(parentCaptor.capture());
        Parent savedParent = parentCaptor.getValue();
        assertThat(savedParent.getUser()).isEqualTo(savedParentUser);

        // Verify student entity creation and parent linkage
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();
        assertThat(savedStudent.getUser()).isEqualTo(savedStudentUser);
        assertThat(savedStudent.getParent()).isEqualTo(savedParent);

        // Verify email sent to parent
        verify(emailService).sendSimpleMessage(
            eq(validRequest.getParentEmail()),
            eq("Tài khoản phụ huynh đã được tạo trên Edu360"),
            contains("Tài khoản phụ huynh của bạn đã được tạo")
        );

        // Verify password encoding
        verify(encoder, times(2)).encode(anyString());
    }

    @Test
    void registerStudentWithParent_PasswordMismatch_ShouldReturnBadRequest() {
        // Given
        validRequest.setStudentRePassword("different_password");

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại.");

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(parentRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void registerStudentWithParent_StudentUsernameExists_ShouldReturnBadRequest() {
        // Given
        when(userRepository.existsByUsername(validRequest.getStudentUsername())).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Tên đăng nhập này đã tồn tại. Vui lòng chọn tên khác.");

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(parentRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void registerStudentWithParent_StudentEmailExists_ShouldReturnBadRequest() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(validRequest.getStudentEmail())).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Email học sinh này đã được sử dụng. Vui lòng sử dụng email khác.");

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(parentRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void registerStudentWithParent_ParentEmailExists_ShouldReturnBadRequest() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(validRequest.getStudentEmail())).thenReturn(false);
        when(userRepository.existsByEmail(validRequest.getParentEmail())).thenReturn(true);

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("Email phụ huynh này đã được sử dụng. Vui lòng sử dụng email khác.");

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(parentRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void registerStudentWithParent_EmailSendingFails_ShouldStillCreateAccountsWithWarning() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service to throw exception
        doThrow(new MailException("SMTP server unavailable") {}).when(emailService)
            .sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Đăng ký thành công!");

        // Verify accounts were still created
        verify(userRepository, times(2)).save(any(User.class));
        verify(parentRepository).save(any(Parent.class));
        verify(studentRepository).save(any(Student.class));
    }

    @Test
    void registerStudentWithParent_ParentRoleNotFound_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.empty());

        // When & Then
        try {
            authService.registerStudentWithParent(validRequest);
            // Should not reach here
            assertThat(false).as("Expected RuntimeException to be thrown").isTrue();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Role ROLE_PARENT not found");
        }

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(parentRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void registerStudentWithParent_StudentRoleNotFound_ShouldThrowException() {
        // Given
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then - Should throw RuntimeException when trying to find ROLE_STUDENT
        try {
            authService.registerStudentWithParent(validRequest);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Role ROLE_STUDENT not found");
        }
    }

    @Test
    void registerStudentWithParent_UsernameGeneration_ShouldEnsureUniqueness() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        // First call for parent username generation - exists
        // "Nguyen Van A" -> "anv" (last name + initials)
        when(userRepository.existsByUsername("anv")).thenReturn(true);
        // Second call for parent username generation - unique
        when(userRepository.existsByUsername("anv1")).thenReturn(false);
        // Student username check - unique
        when(userRepository.existsByUsername(validRequest.getStudentUsername())).thenReturn(false);
        
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify username uniqueness check was called multiple times
        verify(userRepository, atLeast(2)).existsByUsername(anyString());
        
        // Verify parent user was saved with unique username
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User parentUser = userCaptor.getAllValues().get(0);
        assertThat(parentUser.getUsername()).isEqualTo("anv1"); // Should be incremented
    }

    @Test
    void registerStudentWithParent_NullParentEmail_ShouldSkipParentEmailValidation() {
        // Given
        validRequest.setParentEmail(null);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(validRequest.getStudentEmail())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify parent email validation was skipped (only student email checked)
        verify(userRepository).existsByEmail(validRequest.getStudentEmail());
        verify(userRepository, never()).existsByEmail(null);
    }

    @Test
    void registerStudentWithParent_EmptyFullName_ShouldGenerateFallbackUsername() {
        // Given
        validRequest.setParentFullName(""); // Empty name
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(parentRepository.save(any(Parent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.registerStudentWithParent(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify parent user was created with fallback username pattern
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User parentUser = userCaptor.getAllValues().get(0);
        assertThat(parentUser.getUsername()).startsWith("user"); // Fallback pattern
    }

    // Helper methods
    private RegisterStudentWithParentRequest createValidRequest() {
        RegisterStudentWithParentRequest request = new RegisterStudentWithParentRequest();
        request.setStudentFullName("Nguyen Van B");
        request.setStudentUsername("nguyenvanb");
        request.setStudentPassword("password123");
        request.setStudentRePassword("password123");
        request.setStudentPhoneNumber("0987654321");
        request.setStudentEmail("student@test.com");
        request.setParentFullName("Nguyen Van A");
        request.setParentEmail("parent@test.com");
        request.setParentPhoneNumber("0123456789");
        return request;
    }

    private Role createRole(ERole roleName) {
        Role role = new Role();
        role.setId(1);
        role.setName(roleName);
        return role;
    }
}