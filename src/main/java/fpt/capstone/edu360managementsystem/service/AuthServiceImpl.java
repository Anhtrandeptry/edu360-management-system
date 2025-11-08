package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ParentRepository parentRepository;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    TeacherRepository teacherRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    EmailService emailService;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();

    @Override
    @Transactional
    public ResponseEntity<?> registerStudentWithParent(RegisterStudentWithParentRequest request) {
        // 1. Basic validations
        if (!request.getStudentPassword().equals(request.getStudentRePassword())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Student passwords do not match!"));
        }

        if (userRepository.existsByUsername(request.getStudentUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Student username is already taken!"));
        }

        if (userRepository.existsByEmail(request.getStudentEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Student email is already in use!"));
        }

        if (request.getParentEmail() != null && userRepository.existsByEmail(request.getParentEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Parent email is already in use!"));
        }

        // 2. Create parent user
        String parentUsernameBase = generateUsernameFromFullName(request.getParentFullName());
        String parentUsername = ensureUniqueUsername(parentUsernameBase);

        String parentPlainPassword = generateRandomPassword(10);
        String parentEncodedPassword = encoder.encode(parentPlainPassword);

        User parentUser = new User();
        parentUser.setUsername(parentUsername);
        parentUser.setEmail(request.getParentEmail());
        parentUser.setPassword(parentEncodedPassword);
        // assumed fields
        parentUser.setFullName(request.getParentFullName());
        parentUser.setPhoneNumber(request.getParentPhoneNumber());

        // add ROLE_PARENT
        Role parentRole = roleRepository.findByName(ERole.ROLE_PARENT)
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_PARENT not found."));
        parentUser.getRoles().add(parentRole);

        userRepository.save(parentUser);

        // 3. Create Parent entity
        Parent parent = new Parent();
        parent.setUser(parentUser);
        // optional fields left blank: occupation/address
        parentRepository.save(parent);

        // 4. Create student user
        User studentUser = new User();
        studentUser.setUsername(request.getStudentUsername());
        studentUser.setEmail(request.getStudentEmail());
        studentUser.setPassword(encoder.encode(request.getStudentPassword()));
        studentUser.setFullName(request.getStudentFullName());
        studentUser.setPhoneNumber(request.getStudentPhoneNumber());

        Role studentRole = roleRepository.findByName(ERole.ROLE_STUDENT)
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_STUDENT not found."));
        studentUser.getRoles().add(studentRole);

        userRepository.save(studentUser);

        // 5. Create Student entity and link to parent
        Student student = new Student();
        student.setUser(studentUser);
        student.setParent(parent);
        // optional fields: dob, grade, school -> keep null or set if request contains them
        studentRepository.save(student);

        // 6. Send email to parent with login info (best-effort)
        String subject = "Tài khoản phụ huynh đã được tạo trên Edu360";
        String text = String.format("Xin chào %s,\n\nTài khoản phụ huynh của bạn đã được tạo:\n\n- Tên đăng nhập: %s\n- Mật khẩu: %s\n\nVui lòng đăng nhập và đổi mật khẩu ngay lần đầu sử dụng.\n\nTrân trọng,\nĐội ngũ Edu360",
                parentUser.getFullName(), parentUsername, parentPlainPassword);

        try {
            emailService.sendSimpleMessage(parentUser.getEmail(), subject, text);
        } catch (MailException ex) {
            // Không ném lỗi để rollback DB vì gửi mail có thể thất bại; log và trả thông báo success + warning.
            // (Bạn có thể thay đổi để rollback nếu muốn)
            // dùng ResponseEntity 200 nhưng kèm thông báo
            return ResponseEntity.ok(new MessageResponse("User created but failed to send email to parent: " + ex.getMessage()));
        }

        return ResponseEntity.ok(new MessageResponse("Student and parent accounts created successfully! Parent username: " + parentUsername));
    }



    @Override
    @Transactional
    public ResponseEntity<?> registerTeacher(RegisterTeacherRequest request) {
        // 1. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
        }

        // 2. Sinh username tự động từ họ tên
        String usernameBase = generateUsernameFromFullName(request.getFullName());
        String username = ensureUniqueUsername(usernameBase);

        // 3. Sinh mật khẩu ngẫu nhiên
        String rawPassword = generateRandomPassword(10);

        // 4. Mã hóa mật khẩu
        String encodedPassword = encoder.encode(rawPassword);

        // 5. Lấy role TEACHER
        Role teacherRole = roleRepository.findByName(ERole.ROLE_TEACHER)
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_TEACHER not found."));

        // 6. Tạo user
        User teacherUser = new User();
        teacherUser.setUsername(username);
        teacherUser.setEmail(request.getEmail());
        teacherUser.setPassword(encodedPassword);
        teacherUser.setFullName(request.getFullName());
        teacherUser.setPhoneNumber(request.getPhoneNumber());
        teacherUser.getRoles().add(teacherRole);

        userRepository.save(teacherUser);

        // 7. Tạo teacher entity (chưa có thông tin chi tiết)
        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        teacherRepository.save(teacher);

        // 8. Gửi email tài khoản cho giáo viên
        String subject = "Tài khoản giáo viên đã được tạo trên Edu360";
        String text = String.format("""
            Xin chào %s,

            Tài khoản giáo viên của bạn đã được tạo:

            - Tên đăng nhập: %s
            - Mật khẩu: %s

            Vui lòng đăng nhập và hoàn thiện hồ sơ cá nhân tại hệ thống Edu360.

            Trân trọng,
            Đội ngũ Edu360
            """, request.getFullName(), username, rawPassword);

        try {
            emailService.sendSimpleMessage(request.getEmail(), subject, text);
        } catch (MailException ex) {
            return ResponseEntity.ok(new MessageResponse(
                    "Teacher created successfully but failed to send email: " + ex.getMessage()));
        }

        return ResponseEntity.ok(new MessageResponse(
                "Teacher account created successfully! Username: " + username));
    }


    // --- helper methods ---

    private String generateUsernameFromFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            // fallback
            return "user" + System.currentTimeMillis();
        }
        String[] parts = fullName.trim().toLowerCase().split("\\s+");
        String last = parts[parts.length - 1];
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            initials.append(parts[i].charAt(0));
        }
        return last + initials.toString(); // anhtq
    }

    private String ensureUniqueUsername(String base) {
        String candidate = base;
        int counter = 0;
        while (userRepository.existsByUsername(candidate)) {
            counter++;
            candidate = base + counter;
        }
        return candidate;
    }

    private String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RANDOM.nextInt(PASSWORD_CHARS.length());
            sb.append(PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }
}
