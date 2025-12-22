package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.PasswordResetToken;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.PasswordResetTokenRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.forgotPassword() method
 * 
 * Tests cover:
 * - Successful password reset link generation with email notification
 * - User not found handling (security-conscious response)
 * - Email sending failure handling
 * - Case-insensitive email matching
 * - Generic response for security (same message regardless of user existence)
 * - Token generation and storage
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private ForgotPasswordRequest validRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        validRequest = createValidRequest();
        existingUser = createExistingUser();
        // Set frontend URL for reset link generation
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void forgotPassword_ValidEmail_ShouldSendResetLinkEmail() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("link đặt lại mật khẩu");

        // Verify old tokens were invalidated
        verify(passwordResetTokenRepository).invalidateAllTokensForUser(existingUser);

        // Verify new token was saved
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getUser()).isEqualTo(existingUser);
        assertThat(savedToken.getExpiryDate()).isNotNull();

        // Verify email was sent with reset link
        ArgumentCaptor<String> emailContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendSimpleMessage(
            eq(existingUser.getEmail()),
            eq("Đặt lại mật khẩu tài khoản Edu360"),
            emailContentCaptor.capture()
        );
        String emailContent = emailContentCaptor.getValue();
        assertThat(emailContent).contains("reset-password?token=");
        assertThat(emailContent).contains(existingUser.getFullName());
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
            "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi."
        );

        // Verify no database operations
        verify(passwordResetTokenRepository, never()).invalidateAllTokensForUser(any());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_CaseInsensitiveEmailMatching_ShouldFindUser() {
        // Given
        validRequest.setEmail("USER@TEST.COM"); // Uppercase email
        existingUser.setEmail("user@test.com"); // Lowercase in database
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify token was created and email was sent
        verify(passwordResetTokenRepository).invalidateAllTokensForUser(existingUser);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_EmailSendingFails_ShouldReturnErrorMessage() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock email service to throw exception
        doThrow(new MailException("SMTP server unavailable") {}).when(emailService)
            .sendSimpleMessage(anyString(), anyString(), anyString());

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).contains("Không thể gửi email");

        // Verify token was still created before email failed
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void forgotPassword_MultipleUsersWithDifferentEmails_ShouldFindCorrectUser() {
        // Given
        User user1 = createUser(1L, "user1@test.com", "User One");
        User user2 = createUser(2L, "user2@test.com", "User Two");
        User user3 = createUser(3L, "user3@test.com", "User Three");
        
        validRequest.setEmail("user2@test.com");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify correct user's tokens were invalidated
        verify(passwordResetTokenRepository).invalidateAllTokensForUser(user2);

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
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify email content
        ArgumentCaptor<String> emailContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendSimpleMessage(
            eq(existingUser.getEmail()),
            eq("Đặt lại mật khẩu tài khoản Edu360"),
            emailContentCaptor.capture()
        );
        
        String emailContent = emailContentCaptor.getValue();
        assertThat(emailContent).contains("Xin chào " + existingUser.getFullName());
        assertThat(emailContent).contains("Bạn vừa yêu cầu đặt lại mật khẩu");
        assertThat(emailContent).contains("reset-password?token=");
        assertThat(emailContent).contains("Đội ngũ Edu360");
    }

    @Test
    void forgotPassword_TokenGeneration_ShouldGenerateUniqueToken() {
        // Given
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        authService.forgotPassword(validRequest);

        // Then
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        
        // Verify token characteristics (UUID format)
        assertThat(savedToken.getToken()).isNotNull();
        assertThat(savedToken.getToken()).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
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
            "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi."
        );

        // Verify no operations performed
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_NullEmail_ShouldThrowNullPointerException() {
        // Given
        validRequest.setEmail(null);

        // When & Then
        try {
            authService.forgotPassword(validRequest);
            // Should not reach here
            assertThat(false).as("Expected NullPointerException to be thrown").isTrue();
        } catch (NullPointerException e) {
            // Expected behavior - service doesn't handle null email gracefully
            assertThat(e.getMessage()).contains("Cannot invoke \"String.toLowerCase()\"");
        }

        // Verify no operations performed
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test
    void forgotPassword_WhitespaceInEmail_ShouldTrimAndFindUser() {
        // Given - new implementation trims whitespace
        validRequest.setEmail("  user@test.com  "); // Email with whitespace
        existingUser.setEmail("user@test.com");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(existingUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<?> response = authService.forgotPassword(validRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Current implementation trims email, so user should be found
        verify(passwordResetTokenRepository).invalidateAllTokensForUser(existingUser);
        verify(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
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
