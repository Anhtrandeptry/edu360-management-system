package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.LoginRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.security.jwt.JwtUtils;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController login functionality
 * 
 * Tests cover:
 * - Successful login with JWT cookie generation
 * - User info response with roles and avatar
 * - Student avatar retrieval
 * - Teacher avatar retrieval
 * - User without avatar
 * - Authentication failure handling
 * - Security context setup
 * - JWT cookie configuration
 * - Multiple roles handling
 * - Logout functionality
 * - /me endpoint for current user info
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthController authController;

    private LoginRequest validLoginRequest;
    private UserDetailsImpl userDetails;
    private User user;
    private ResponseCookie jwtCookie;

    @BeforeEach
    void setUp() {
        validLoginRequest = createValidLoginRequest();
        userDetails = createUserDetails();
        user = createUser();
        jwtCookie = createJwtCookie();
        
        // Setup SecurityContextHolder mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void authenticateUser_ValidCredentials_ShouldReturnUserInfoWithJwtCookie() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.empty());
        when(teacherRepository.findByUserId(userDetails.getId())).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).contains(jwtCookie.toString());
        
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getId()).isEqualTo(userDetails.getId());
        assertThat(userInfo.getUsername()).isEqualTo(userDetails.getUsername());
        assertThat(userInfo.getEmail()).isEqualTo(userDetails.getEmail());
        assertThat(userInfo.getFullName()).isEqualTo(user.getFullName());
        assertThat(userInfo.getAvatarUrl()).isNull(); // No avatar
        assertThat(userInfo.getRoles()).containsExactly("ROLE_STUDENT");

        // Verify security context was set
        verify(securityContext).setAuthentication(authentication);
        
        // Verify JWT cookie generation
        verify(jwtUtils).generateJwtCookie(userDetails);
    }

    @Test
    void authenticateUser_StudentWithAvatar_ShouldReturnAvatarUrl() {
        // Given
        Student student = createStudentWithAvatar();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.of(student));

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getAvatarUrl()).isEqualTo("http://example.com/student-avatar.jpg");
    }

    @Test
    void authenticateUser_TeacherWithAvatar_ShouldReturnAvatarUrl() {
        // Given
        Teacher teacher = createTeacherWithAvatar();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.empty());
        when(teacherRepository.findByUserId(userDetails.getId())).thenReturn(Optional.of(teacher));

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getAvatarUrl()).isEqualTo("http://example.com/teacher-avatar.jpg");
    }

    @Test
    void authenticateUser_StudentAvatarTakesPrecedenceOverTeacher_ShouldReturnStudentAvatar() {
        // Given - User is both student and teacher (edge case)
        Student student = createStudentWithAvatar();
        Teacher teacher = createTeacherWithAvatar();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.of(student));
        // Teacher repository should not be called since student avatar is found first

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getAvatarUrl()).isEqualTo("http://example.com/student-avatar.jpg");
        
        // Student avatar takes precedence, so teacher repository should not be called
    }

    @Test
    void authenticateUser_InvalidCredentials_ShouldThrowBadCredentialsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        try {
            authController.authenticateUser(validLoginRequest);
        } catch (BadCredentialsException e) {
            assertThat(e.getMessage()).isEqualTo("Invalid credentials");
        }

        // Verify no JWT cookie generation
        verify(jwtUtils, never()).generateJwtCookie(any());
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    void authenticateUser_UserNotFoundInDatabase_ShouldReturnNullFullNameAndAvatar() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getFullName()).isNull();
        assertThat(userInfo.getAvatarUrl()).isNull();
        
        // Verify student/teacher repositories were not called
        verify(studentRepository, never()).findByUser_Id(any());
        verify(teacherRepository, never()).findByUserId(any());
    }

    @Test
    void authenticateUser_MultipleRoles_ShouldReturnAllRoles() {
        // Given
        UserDetailsImpl multiRoleUserDetails = createMultiRoleUserDetails();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(multiRoleUserDetails);
        when(jwtUtils.generateJwtCookie(multiRoleUserDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(multiRoleUserDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(multiRoleUserDetails.getId())).thenReturn(Optional.empty());
        when(teacherRepository.findByUserId(multiRoleUserDetails.getId())).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_TEACHER");
    }

    @Test
    void logoutUser_ShouldReturnCleanCookieAndSuccessMessage() {
        // Given
        ResponseCookie cleanCookie = ResponseCookie.from("jwt", "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .build();
        when(jwtUtils.getCleanJwtCookie()).thenReturn(cleanCookie);

        // When
        ResponseEntity<?> response = authController.logoutUser();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().get(HttpHeaders.SET_COOKIE)).contains(cleanCookie.toString());
        
        MessageResponse messageResponse = (MessageResponse) response.getBody();
        assertThat(messageResponse.getMessage()).isEqualTo("You've been signed out!");
        
        verify(jwtUtils).getCleanJwtCookie();
    }

    @Test
    void me_AuthenticatedUser_ShouldReturnUserInfo() {
        // Given
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.empty());
        when(teacherRepository.findByUserId(userDetails.getId())).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = authController.me(userDetails);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getId()).isEqualTo(userDetails.getId());
        assertThat(userInfo.getUsername()).isEqualTo(userDetails.getUsername());
        assertThat(userInfo.getEmail()).isEqualTo(userDetails.getEmail());
        assertThat(userInfo.getFullName()).isEqualTo(user.getFullName());
        assertThat(userInfo.getRoles()).containsExactly("ROLE_STUDENT");
    }

    @Test
    void me_UnauthenticatedUser_ShouldReturnUnauthorized() {
        // Given - null user (not authenticated)

        // When
        ResponseEntity<?> response = authController.me(null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Verify no database calls
        verify(userRepository, never()).findById(any());
        verify(studentRepository, never()).findByUser_Id(any());
        verify(teacherRepository, never()).findByUserId(any());
    }

    @Test
    void me_StudentWithAvatar_ShouldReturnAvatarUrl() {
        // Given
        Student student = createStudentWithAvatar();
        
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.of(student));

        // When
        ResponseEntity<?> response = authController.me(userDetails);

        // Then
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getAvatarUrl()).isEqualTo("http://example.com/student-avatar.jpg");
    }

    @Test
    void authenticateUser_StudentWithNullAvatar_ShouldCheckTeacherAvatar() {
        // Given
        Student studentWithoutAvatar = new Student();
        studentWithoutAvatar.setAvatarUrl(null);
        
        Teacher teacher = createTeacherWithAvatar();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtils.generateJwtCookie(userDetails)).thenReturn(jwtCookie);
        when(userRepository.findById(userDetails.getId())).thenReturn(Optional.of(user));
        when(studentRepository.findByUser_Id(userDetails.getId())).thenReturn(Optional.of(studentWithoutAvatar));
        when(teacherRepository.findByUserId(userDetails.getId())).thenReturn(Optional.of(teacher));

        // When
        ResponseEntity<?> response = authController.authenticateUser(validLoginRequest);

        // Then
        UserInfoResponse userInfo = (UserInfoResponse) response.getBody();
        assertThat(userInfo.getAvatarUrl()).isEqualTo("http://example.com/teacher-avatar.jpg");
        
        // Verify both repositories were called
        verify(studentRepository).findByUser_Id(userDetails.getId());
        verify(teacherRepository).findByUserId(userDetails.getId());
    }

    // Helper methods
    private LoginRequest createValidLoginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        return request;
    }

    private UserDetailsImpl createUserDetails() {
        return new UserDetailsImpl(
            1L,
            "testuser",
            "test@example.com",
            "encoded_password",
            Arrays.asList(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    private UserDetailsImpl createMultiRoleUserDetails() {
        return new UserDetailsImpl(
            1L,
            "testuser",
            "test@example.com",
            "encoded_password",
            Arrays.asList(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_TEACHER")
            )
        );
    }

    private User createUserForUserDetails() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encoded_password");
        user.setFullName("Test User");
        return user;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        return user;
    }

    private Student createStudentWithAvatar() {
        Student student = new Student();
        student.setAvatarUrl("http://example.com/student-avatar.jpg");
        return student;
    }

    private Teacher createTeacherWithAvatar() {
        Teacher teacher = new Teacher();
        teacher.setAvatarUrl("http://example.com/teacher-avatar.jpg");
        return teacher;
    }

    private ResponseCookie createJwtCookie() {
        return ResponseCookie.from("jwt", "jwt-token-value")
            .httpOnly(true)
            .path("/")
            .maxAge(86400)
            .build();
    }
}