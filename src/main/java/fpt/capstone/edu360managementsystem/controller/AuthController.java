package fpt.capstone.edu360managementsystem.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fpt.capstone.edu360managementsystem.dto.request.LoginRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.security.jwt.JwtUtils;
import fpt.capstone.edu360managementsystem.service.AuthService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    AuthService authService;

//Login
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // Get additional user info from Student/Teacher entities
        String fullName = null;
        String avatarUrl = null;
        
        User user = userRepository.findById(userDetails.getId()).orElse(null);
        if (user != null) {
            fullName = user.getFullName();
            
            // Check if user is a student and get avatar from Student entity
            Student student = studentRepository.findByUser_Id(userDetails.getId()).orElse(null);
            if (student != null && student.getAvatarUrl() != null) {
                avatarUrl = student.getAvatarUrl();
            } else {
                // Check if user is a teacher and get avatar from Teacher entity  
                Teacher teacher = teacherRepository.findByUserId(userDetails.getId()).orElse(null);
                if (teacher != null && teacher.getAvatarUrl() != null) {
                    avatarUrl = teacher.getAvatarUrl();
                }
            }
        }

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(new UserInfoResponse(userDetails.getId(),
                        userDetails.getUsername(),
                        userDetails.getEmail(),
                        fullName,
                        avatarUrl,
                        roles));
    }

//logout
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been signed out!"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@org.springframework.security.core.annotation.AuthenticationPrincipal fpt.capstone.edu360managementsystem.service.UserDetailsImpl user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        var roles = user.getAuthorities().stream().map(a -> a.getAuthority()).toList();
        
        // Get additional user info from Student/Teacher entities
        String fullName = null;
        String avatarUrl = null;
        
        User userEntity = userRepository.findById(user.getId()).orElse(null);
        if (userEntity != null) {
            fullName = userEntity.getFullName();
            
            // Check if user is a student and get avatar from Student entity
            Student student = studentRepository.findByUser_Id(user.getId()).orElse(null);
            if (student != null && student.getAvatarUrl() != null) {
                avatarUrl = student.getAvatarUrl();
            } else {
                // Check if user is a teacher and get avatar from Teacher entity  
                Teacher teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
                if (teacher != null && teacher.getAvatarUrl() != null) {
                    avatarUrl = teacher.getAvatarUrl();
                }
            }
        }
        
        return ResponseEntity.ok(new fpt.capstone.edu360managementsystem.dto.response.UserInfoResponse(
                user.getId(), user.getUsername(), user.getEmail(), fullName, avatarUrl, roles
        ));
    }

// Create Teacher
    @PostMapping("/register-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerTeacher(@Valid @RequestBody RegisterTeacherRequest request) {
        return authService.registerTeacher(request);
    }

// Register Student and Parent account
    @PostMapping("/signup")
    public ResponseEntity<?> registerStudentWithParent(@Valid @RequestBody RegisterStudentWithParentRequest request) {
        return authService.registerStudentWithParent(request);
    }

}
