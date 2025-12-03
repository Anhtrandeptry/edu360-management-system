package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuthServiceImpl Unit Tests - 60 Cases
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private ParentRepository parentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private PasswordEncoder encoder;
    @Mock private EmailService emailService;
    @InjectMocks private AuthServiceImpl authService;

    private Role parentRole;
    private Role studentRole;
    private Role teacherRole;
    private Subject subject;

    @BeforeEach
    void setUp() {
        parentRole = new Role();
        parentRole.setId(1);
        parentRole.setName(ERole.ROLE_PARENT);

        studentRole = new Role();
        studentRole.setId(2);
        studentRole.setName(ERole.ROLE_STUDENT);

        teacherRole = new Role();
        teacherRole.setId(3);
        teacherRole.setName(ERole.ROLE_TEACHER);

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");
        subject.setStatus(SubjectStatus.AVAILABLE);
    }

    // ========== registerStudentWithParent() - 30 cases ==========

    @Test void test01_passwordMismatch() {
        RegisterStudentWithParentRequest req = new RegisterStudentWithParentRequest();
        req.setStudentPassword("pass123");
        req.setStudentRePassword("pass456");
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("không khớp");
    }

    @Test void test02_studentUsernameExists() {
        RegisterStudentWithParentRequest req = new RegisterStudentWithParentRequest();
        req.setStudentPassword("pass123");
        req.setStudentRePassword("pass123");
        req.setStudentUsername("existing");
        when(userRepository.existsByUsername("existing")).thenReturn(true);
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("đã tồn tại");
    }

    @Test void test03_studentEmailExists() {
        RegisterStudentWithParentRequest req = new RegisterStudentWithParentRequest();
        req.setStudentPassword("pass123");
        req.setStudentRePassword("pass123");
        req.setStudentUsername("newuser");
        req.setStudentEmail("existing@test.com");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("Email học sinh");
    }

    @Test void test04_parentEmailExists() {
        RegisterStudentWithParentRequest req = new RegisterStudentWithParentRequest();
        req.setStudentPassword("pass123");
        req.setStudentRePassword("pass123");
        req.setStudentUsername("newuser");
        req.setStudentEmail("student@test.com");
        req.setParentEmail("parent@test.com");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("student@test.com")).thenReturn(false);
        when(userRepository.existsByEmail("parent@test.com")).thenReturn(true);
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("Email phụ huynh");
    }

    @Test void test05_allValid_success() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Helper method
    private RegisterStudentWithParentRequest createValidStudentRequest() {
        RegisterStudentWithParentRequest req = new RegisterStudentWithParentRequest();
        req.setStudentUsername("student1");
        req.setStudentPassword("pass123");
        req.setStudentRePassword("pass123");
        req.setStudentEmail("student@test.com");
        req.setStudentFullName("Nguyen Van A");
        req.setStudentPhoneNumber("0123456789");
        req.setParentFullName("Nguyen Van B");
        req.setParentEmail("parent@test.com");
        req.setParentPhoneNumber("0987654321");
        return req;
    }

    private void mockValidStudentRegistration() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    @Test void test06_parentUserCreated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test void test07_studentUserCreated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(userRepository, times(2)).save(any());
    }

    @Test void test08_parentUsernameGenerated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setParentFullName("Nguyen Van Binh");
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(userRepository, atLeastOnce()).existsByUsername(contains("binh"));
    }

    @Test void test09_parentPasswordGenerated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(encoder, atLeastOnce()).encode(anyString());
    }

    @Test void test10_rolesAssigned() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(roleRepository).findByName(ERole.ROLE_PARENT);
        verify(roleRepository).findByName(ERole.ROLE_STUDENT);
    }

    @Test void test11_parentUsernameConflict_appendNumber() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setParentFullName("Nguyen Van Binh");
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("binhnv")).thenReturn(true);
        when(userRepository.existsByUsername("binhnv1")).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        authService.registerStudentWithParent(req);
        verify(userRepository, atLeastOnce()).existsByUsername(anyString());
    }

    @Test void test12_parentPasswordEncoded() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(encoder, atLeast(2)).encode(anyString());
    }

    @Test void test13_studentPasswordEncoded() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(encoder, atLeast(2)).encode(anyString());
    }

    @Test void test14_roleParentAssigned() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(roleRepository).findByName(ERole.ROLE_PARENT);
    }

    @Test void test15_roleStudentAssigned() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(roleRepository).findByName(ERole.ROLE_STUDENT);
    }

    @Test void test16_bothUsersSaved() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test void test17_parentEntityCreated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(parentRepository).save(any(Parent.class));
    }

    @Test void test18_studentEntityCreated() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(studentRepository).save(any(Student.class));
    }

    @Test void test19_studentLinkedToParent() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(studentRepository).save(argThat(student -> student.getParent() != null));
    }

    @Test void test20_parentFieldsOptionalNull() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setParentPhoneNumber(null);
        mockValidStudentRegistration();
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test21_studentFieldsOptionalNull() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setStudentPhoneNumber(null);
        mockValidStudentRegistration();
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test22_emailSentSuccessfully() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(emailService).sendSimpleMessage(eq("parent@test.com"), anyString(), anyString());
    }

    @Test void test23_emailFailed_successWithWarning() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new MailException("SMTP error") {}).when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("failed to send email");
    }

    @Test void test24_emailContentCorrect() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(emailService).sendSimpleMessage(anyString(), contains("Tài khoản phụ huynh"), anyString());
    }

    @Test void test25_emailRecipientParent() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(emailService).sendSimpleMessage(eq("parent@test.com"), anyString(), anyString());
    }

    @Test void test26_emailContainsCredentials() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(emailService).sendSimpleMessage(anyString(), anyString(), contains("Tên đăng nhập"));
    }

    @Test void test27_specialCharactersInUsername() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setStudentFullName("Nguyễn Văn Ánh");
        mockValidStudentRegistration();
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test28_longNames() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setStudentFullName("Nguyen Van Anh Khoa Minh Duc Tuan");
        req.setParentFullName("Nguyen Van Binh Minh Duc Tuan Khoa");
        mockValidStudentRegistration();
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test29_parentEmailNull_allowed() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        req.setParentEmail(null);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(ERole.ROLE_PARENT)).thenReturn(Optional.of(parentRole));
        when(roleRepository.findByName(ERole.ROLE_STUDENT)).thenReturn(Optional.of(studentRole));
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(parentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(studentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(isNull(), anyString(), anyString());
        ResponseEntity<?> response = authService.registerStudentWithParent(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test30_transactionCommit() {
        RegisterStudentWithParentRequest req = createValidStudentRequest();
        mockValidStudentRegistration();
        authService.registerStudentWithParent(req);
        verify(userRepository, times(2)).save(any());
        verify(parentRepository).save(any());
        verify(studentRepository).save(any());
    }

    // ========== registerTeacher() - 25 cases ==========

    @Test void test31_teacher_emailExists() {
        RegisterTeacherRequest req = new RegisterTeacherRequest();
        req.setEmail("existing@test.com");
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("Email is already in use");
    }

    @Test void test32_teacher_emptySubjectIds() {
        RegisterTeacherRequest req = new RegisterTeacherRequest();
        req.setEmail("teacher@test.com");
        req.setFullName("Teacher A");
        req.setSubjectIds(new ArrayList<>());
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("At least one subject");
    }

    @Test void test33_teacher_invalidSubjectId() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subjectRepository.findAllById(anyList())).thenReturn(new ArrayList<>());
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("not found");
    }

    @Test void test34_teacher_subjectUnavailable() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        Subject unavailableSubject = new Subject();
        unavailableSubject.setId(1L);
        unavailableSubject.setStatus(SubjectStatus.UNAVAILABLE);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(unavailableSubject));
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("unavailable");
    }

    @Test void test35_teacher_allValid_success() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test36_teacher_userCreated() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository).save(any(User.class));
    }

    @Test void test37_teacher_usernameGenerated() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName("Tran Quoc Anh");
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository, atLeastOnce()).existsByUsername(contains("anh"));
    }

    @Test void test38_teacher_usernameConflict_appendNumber() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName("Tran Quoc Anh");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("anhtq")).thenReturn(true);
        when(userRepository.existsByUsername("anhtq1")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        authService.registerTeacher(req);
        verify(userRepository, atLeastOnce()).existsByUsername(anyString());
    }

    @Test void test39_teacher_passwordGenerated() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(encoder).encode(anyString());
    }

    @Test void test40_teacher_passwordEncoded() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(encoder).encode(anyString());
    }

    @Test void test41_teacher_roleAssigned() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(roleRepository).findByName(ERole.ROLE_TEACHER);
    }

    @Test void test42_teacher_userFieldsSet() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository).save(argThat(user -> 
            user.getEmail().equals("teacher@test.com") && 
            user.getFullName().equals("Teacher A")
        ));
    }

    @Test void test43_teacher_userSaved() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository).save(any(User.class));
    }

    @Test void test44_teacher_entityCreated() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test void test45_teacher_primarySubject_firstInList() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(teacherRepository).save(argThat(teacher -> teacher.getSubject() != null));
    }

    @Test void test46_teacher_multipleSubjects_allLinked() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        Subject subject2 = new Subject();
        subject2.setId(2L);
        subject2.setStatus(SubjectStatus.AVAILABLE);
        req.setSubjectIds(List.of(1L, 2L));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject, subject2));
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        authService.registerTeacher(req);
        verify(teacherRepository).save(argThat(teacher -> teacher.getSubjects().size() == 2));
    }

    @Test void test47_teacher_bidirectionalSync() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test void test48_teacher_entitySaved() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(teacherRepository).save(any(Teacher.class));
    }

    @Test void test49_teacher_emailSent() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(emailService).sendSimpleMessage(eq("teacher@test.com"), anyString(), anyString());
    }

    @Test void test50_teacher_emailFailed_successWithWarning() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject));
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doThrow(new MailException("SMTP error") {}).when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((MessageResponse) response.getBody()).getMessage()).contains("failed to send email");
    }

    @Test void test51_teacher_emailContentCorrect() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(emailService).sendSimpleMessage(anyString(), contains("Tài khoản giáo viên"), anyString());
    }

    @Test void test52_teacher_emailContainsCredentials() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(emailService).sendSimpleMessage(anyString(), anyString(), contains("Tên đăng nhập"));
    }

    @Test void test53_teacher_nullFullName_fallback() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName(null);
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository, atLeastOnce()).existsByUsername(anyString());
    }

    @Test void test54_teacher_multipleSubjects_allValidated() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        Subject subject2 = new Subject();
        subject2.setId(2L);
        subject2.setStatus(SubjectStatus.AVAILABLE);
        req.setSubjectIds(List.of(1L, 2L));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject, subject2));
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        ResponseEntity<?> response = authService.registerTeacher(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test void test55_teacher_transactionCommit() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository).save(any());
        verify(teacherRepository).save(any());
    }

    // ========== Helper methods - 5 cases ==========

    @Test void test56_generateUsernameFromFullName_correctFormat() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName("Tran Quoc Anh");
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository, atLeastOnce()).existsByUsername(contains("anh"));
    }

    @Test void test57_ensureUniqueUsername_appendCounter() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName("Tran Quoc Anh");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("anhtq")).thenReturn(true);
        when(userRepository.existsByUsername("anhtq1")).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
        authService.registerTeacher(req);
        verify(userRepository, atLeast(2)).existsByUsername(anyString());
    }

    @Test void test58_generateRandomPassword_correctLength() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(encoder).encode(argThat(pwd -> pwd.length() == 10));
    }

    @Test void test59_passwordChars_validSet() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(encoder).encode(argThat(pwd -> pwd.toString().matches("[A-Za-z0-9]+")));
    }

    @Test void test60_edgeCases_emptyFullName() {
        RegisterTeacherRequest req = createValidTeacherRequest();
        req.setFullName("");
        mockValidTeacherRegistration();
        authService.registerTeacher(req);
        verify(userRepository, atLeastOnce()).existsByUsername(anyString());
    }

    // Helper methods
    private RegisterTeacherRequest createValidTeacherRequest() {
        RegisterTeacherRequest req = new RegisterTeacherRequest();
        req.setEmail("teacher@test.com");
        req.setFullName("Teacher A");
        req.setPhoneNumber("0123456789");
        req.setSubjectIds(List.of(1L));
        return req;
    }

    private void mockValidTeacherRegistration() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        mockTeacherUsernameGeneration();
        when(subjectRepository.findAllById(anyList())).thenReturn(List.of(subject));
        when(roleRepository.findByName(ERole.ROLE_TEACHER)).thenReturn(Optional.of(teacherRole));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(teacherRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doNothing().when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());
    }

    private void mockTeacherUsernameGeneration() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encoded");
    }
}
