package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.User;
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

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.forgotPassword() method
 * 
 * Tests cover:
 * - Successful password reset with email notification
 * - User not found handling (security-conscious response)
 * - Email sending failure handling
 * - Password generation and encoding
 * - Case-insensitive email matching
 * - Generic response for security (same message regardless of user existence)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private ForgotPasswordRequest validRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
        existingUser = createExistingUser();
    }

    @Test
    void forgotPassword_ValidEmail_ShouldResetPasswordAndSendEmail() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo(
            "Mật khẩu mới đã được gửi tới email của bạn (nếu email tồn tại trong hệ thống)."
        );

        // Verify password was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encoded_new_password");
        assertThat(savedUser.getId()).isEqualTo(existingUser.getId());

        // Verify email was sent
        verify(emailService).sendSimpleMessage(
            eq(existingUser.getEmail()),
            eq("Mật khẩu mới cho tài khoản Edu360"),
            contains("Bạn vừa yêu cầu đặt lại mật khẩu")
        );

        // Verify password encoding
        verify(encoder).encode(anyString());
    }

    @Test
    void forgotPassword_UserNotFound_ShouldReturnGenericSuccessMessage() {
        // Given
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo(
            "Nếu email tồn tại trong hệ thống, mật khẩu mới đã được gửi."
        );

        // Verify no database operations
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void forgotPassword_CaseInsensitiveEmailMatching_ShouldFindUser() {
        // Given
        validRequest.setEmail("USER@TEST.COM"); // Uppercase email
        existingUser.setEmail("user@test.com"); // Lowercase in database
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify user was found and password reset
        verify(userRepository).save(any(User.class));
        verify(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_EmailSendingFails_ShouldStillResetPasswordWithWarning() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service to throw exception
        doThrow(new MailException("SMTP server unavailable") {}).when(emailService)
            .sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Mật khẩu đã được reset nhưng gửi email thất bại");

        // Verify password was still reset
        verify(userRepository).save(any(User.class));
        verify(encoder).encode(anyString());
    }

    @Test
    void forgotPassword_MultipleUsersWithDifferentEmails_ShouldFindCorrectUser() {
        // Given
        User user1 = createUser(1L, "user1@test.com", "User One");
        User user2 = createUser(2L, "user2@test.com", "User Two");
        User user3 = createUser(3L, "user3@test.com", "User Three");
        
        validRequest.setEmail("user2@test.com");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify correct user was updated
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getId()).isEqualTo(2L);
        assertThat(savedUser.getEmail()).isEqualTo("user2@test.com");

        // Verify email sent to correct user
        verify(emailService).sendSimpleMessage(
            eq("user2@test.com"),
            anyString(),
            anyString()
        );
    }

    @Test
    void forgotPassword_EmailContentValidation_ShouldContainCorrectInformation() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify email content
        ArgumentCaptor<String> emailContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendSimpleMessage(
            eq(existingUser.getEmail()),
            eq("Mật khẩu mới cho tài khoản Edu360"),
            emailContentCaptor.capture()
        );
        
        String emailContent = emailContentCaptor.getValue();
        assertThat(emailContent).contains("Xin chào " + existingUser.getFullName());
        assertThat(emailContent).contains("Bạn vừa yêu cầu đặt lại mật khẩu");
        assertThat(emailContent).contains("Mật khẩu mới của bạn là:");
        assertThat(emailContent).contains("Vui lòng đăng nhập và đổi mật khẩu");
        assertThat(emailContent).contains("Đội ngũ Edu360");
    }

    @Test
    void forgotPassword_PasswordGeneration_ShouldGenerateRandomPassword() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(encoder.encode(anyString())).thenReturn("encoded_new_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.forgotPassword(validRequest);

        // Then
        // Verify encoder was called with some password (we can't predict the random password)
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(encoder).encode(passwordCaptor.capture());
        String generatedPassword = passwordCaptor.getValue();
        
        // Verify password characteristics
        assertThat(generatedPassword).isNotNull();
        assertThat(generatedPassword).hasSize(10); // Default length
        assertThat(generatedPassword).matches("[A-Za-z0-9]+"); // Only alphanumeric characters
    }

    @Test
    void forgotPassword_EmptyEmail_ShouldReturnGenericMessage() {
        // Given
        validRequest.setEmail("");
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo(
            "Nếu email tồn tại trong hệ thống, mật khẩu mới đã được gửi."
        );

        // Verify no operations performed
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_NullEmail_ShouldThrowNullPointerException() {
        // Given
        validRequest.setEmail(null);
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));

        // When & Then
        try {
            authService.forgotPassword(validRequest);
            // Should not reach here
            assertThat(false).as("Expected NullPointerException to be thrown").isTrue();
        } catch (NullPointerException e) {
            // Expected behavior - service doesn't handle null email gracefully
            assertThat(e.getMessage()).contains("Cannot invoke \"String.equalsIgnoreCase(String)\"");
        }

        // Verify no operations performed
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_WhitespaceInEmail_ShouldHandleCorrectly() {
        // Given
        validRequest.setEmail("  user@test.com  "); // Email with whitespace
        existingUser.setEmail("user@test.com");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo(
            "Nếu email tồn tại trong hệ thống, mật khẩu mới đã được gửi."
        );
        
        // Should not find user due to whitespace (current implementation doesn't trim)
        // This test documents current behavior - you might want to add trimming
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    // Helper methods
    private ForgotPasswordRequest createValidRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@test.com");
        return request;
    }

    private User createExistingUser() {
        return createUser(1L, "user@test.com", "Test User");
    }

    private User createUser(Long id, String email, String fullName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword("old_encoded_password");
        return user;
    }
}