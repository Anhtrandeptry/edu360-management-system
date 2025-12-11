package fpt.capstone.edu360managementsystem.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Random;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.repository.ParentRepository;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

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
    SubjectRepository subjectRepository;

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
            return ResponseEntity.badRequest().body(new MessageResponse("Mật khẩu xác nhận không khớp. Vui lòng kiểm tra lại."));
        }

        if (userRepository.existsByUsername(request.getStudentUsername())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Tên đăng nhập này đã tồn tại. Vui lòng chọn tên khác."));
        }

        if (userRepository.existsByEmail(request.getStudentEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email học sinh này đã được sử dụng. Vui lòng sử dụng email khác."));
        }

        Parent parent;
        boolean isNewParent = false;

        // 2. Kiểm tra nếu có existingParentId -> liên kết với phụ huynh đã có
        if (request.getExistingParentId() != null) {
            parent = parentRepository.findById(request.getExistingParentId())
                    .orElseThrow(() -> new RuntimeException("Error: Parent with ID " + request.getExistingParentId() + " not found."));
        } else {
            // 3. Tạo parent user mới
            if (request.getParentEmail() != null && userRepository.existsByEmail(request.getParentEmail())) {
                return ResponseEntity.badRequest().body(new MessageResponse("Email phụ huynh này đã được sử dụng. Vui lòng sử dụng email khác."));
            }

            String parentUsernameBase = generateUsernameFromFullName(request.getParentFullName());
            String parentUsername = ensureUniqueUsername(parentUsernameBase);

            String parentPlainPassword = generateRandomPassword(10);
            String parentEncodedPassword = encoder.encode(parentPlainPassword);

            User parentUser = new User();
            parentUser.setUsername(parentUsername);
            parentUser.setEmail(request.getParentEmail());
            parentUser.setPassword(parentEncodedPassword);
            parentUser.setFullName(request.getParentFullName());
            parentUser.setPhoneNumber(request.getParentPhoneNumber());

            Role parentRole = roleRepository.findByName(ERole.ROLE_PARENT)
                    .orElseThrow(() -> new RuntimeException("Error: Role ROLE_PARENT not found."));
            parentUser.getRoles().add(parentRole);

            userRepository.save(parentUser);

            parent = new Parent();
            parent.setUser(parentUser);
            parent.setPhone(request.getParentPhoneNumber()); // Lưu phone vào Parent để tìm kiếm sau này
            parentRepository.save(parent);

            isNewParent = true;

            // Gửi email thông báo tài khoản cho phụ huynh mới
            String subject = "Tài khoản phụ huynh đã được tạo trên Edu360";
            String text = String.format("Xin chào %s,\n\nTài khoản phụ huynh của bạn đã được tạo:\n\n- Tên đăng nhập: %s\n- Mật khẩu: %s\n\nVui lòng đăng nhập và đổi mật khẩu ngay lần đầu sử dụng.\n\nTrân trọng,\nĐội ngũ Edu360",
                    parentUser.getFullName(), parentUsername, parentPlainPassword);

            try {
                emailService.sendSimpleMessage(parentUser.getEmail(), subject, text);
            } catch (MailException ex) {
                // Không throw exception để tránh rollback, chỉ log lỗi
            }
        }

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
        studentRepository.save(student);

        if (isNewParent) {
            return ResponseEntity.ok(new MessageResponse("Đăng ký thành công! Thông tin đăng nhập của phụ huynh đã được gửi qua email."));
        } else {
            return ResponseEntity.ok(new MessageResponse("Đăng ký thành công! Học sinh đã được liên kết với phụ huynh hiện có trong hệ thống."));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> registerTeacher(RegisterTeacherRequest request) {
        // 1. Kiểm tra email đã tồn tại trong giáo viên khác chưa
        if (userRepository.existsTeacherEmail(request.getEmail(), null, ERole.ROLE_TEACHER)) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Email đã được sử dụng bởi giáo viên khác!"));
        }

        // 2. Kiểm tra số điện thoại đã tồn tại trong giáo viên khác chưa
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (userRepository.existsTeacherPhone(request.getPhoneNumber(), null, ERole.ROLE_TEACHER)) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Số điện thoại đã được sử dụng bởi giáo viên khác!"));
            }
        }

        // 3. Sinh username tự động từ họ tên
        String usernameBase = generateUsernameFromFullName(request.getFullName());
        String username = ensureUniqueUsername(usernameBase);

        // 4. Sinh mật khẩu ngẫu nhiên
        String rawPassword = generateRandomPassword(10);

        // 5. Mã hóa mật khẩu
        String encodedPassword = encoder.encode(rawPassword);

        // 6. Lấy role TEACHER
        Role teacherRole = roleRepository.findByName(ERole.ROLE_TEACHER)
                .orElseThrow(() -> new RuntimeException("Error: Role ROLE_TEACHER not found."));

        // 7. Tạo user
        User teacherUser = new User();
        teacherUser.setUsername(username);
        teacherUser.setEmail(request.getEmail());
        teacherUser.setPassword(encodedPassword);
        teacherUser.setFullName(request.getFullName());
        teacherUser.setPhoneNumber(request.getPhoneNumber());
        teacherUser.getRoles().add(teacherRole);

        userRepository.save(teacherUser);

        // 8. Load subjects & validate (multi-subject)
        if (request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: At least one subject is required"));
        }
        var subjects = subjectRepository.findAllById(request.getSubjectIds());
        if (subjects.size() != request.getSubjectIds().size()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Some subject ids were not found"));
        }
        for (Subject s : subjects) {
            if (s.getStatus() != null && s.getStatus().name().equals("UNAVAILABLE")) {
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Subject " + s.getId() + " is unavailable"));
            }
        }

        // 9. Tạo teacher entity với danh sách subjects + subject chính (lấy môn đầu tiên)
        Teacher teacher = new Teacher();
        teacher.setUser(teacherUser);
        // Chọn môn đầu tiên làm subject chính để thỏa mãn NOT NULL cột subject_id cũ
        teacher.setSubject(subjects.get(0));
        // Đồng bộ 2 chiều để Hibernate dễ dàng đồng bộ join table trong cùng transaction
        for (Subject s : subjects) {
            teacher.getSubjects().add(s);
            if (s.getTeachers() != null) {
                s.getTeachers().add(teacher);
            }
        }
        teacherRepository.save(teacher);

        // 10. Gửi email tài khoản cho giáo viên
        String emailSubject = "Tài khoản giáo viên đã được tạo trên Edu360";
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
            emailService.sendSimpleMessage(request.getEmail(), emailSubject, text);
        } catch (MailException ex) {
            return ResponseEntity.ok(new MessageResponse(
                    "Teacher created successfully but failed to send email: " + ex.getMessage()));
        }

        return ResponseEntity.ok(new MessageResponse(
                "Teacher account created successfully! Username: " + username));
    }

    @Override
    @Transactional
    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        // Có thể cho tìm theo email unique
        User user = userRepository.findAll().stream()
                .filter(u -> request.getEmail().equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);

        // Vì lý do bảo mật, có thể trả message chung kể cả khi không tìm được user
        if (user == null) {
            // Lựa chọn 1: message chung
            return ResponseEntity.ok(new MessageResponse(
                    "Nếu email tồn tại trong hệ thống, mật khẩu mới đã được gửi."));
            // Lựa chọn 2: báo lỗi rõ (nếu bạn muốn):
            // return ResponseEntity.badRequest().body(new MessageResponse("Không tìm thấy tài khoản với email này."));
        }

        // 1. Sinh mật khẩu mới
        String newPlainPassword = generateRandomPassword(10);
        String encoded = encoder.encode(newPlainPassword);

        // 2. Lưu vào DB
        user.setPassword(encoded);
        userRepository.save(user);

        // 3. Gửi email mật khẩu mới
        String subject = "Mật khẩu mới cho tài khoản Edu360";
        String text = String.format(
                "Xin chào %s,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản trên hệ thống Edu360.\n\n"
                + "Mật khẩu mới của bạn là: %s\n\n"
                + "Vui lòng đăng nhập và đổi mật khẩu ngay sau khi đăng nhập để đảm bảo an toàn.\n\n"
                + "Trân trọng,\nĐội ngũ Edu360",
                user.getFullName(), newPlainPassword
        );

        try {
            emailService.sendSimpleMessage(user.getEmail(), subject, text);
        } catch (MailException ex) {
            // Có thể chọn rollback hoặc không, ở đây giữ mật khẩu mới và báo lỗi gửi mail
            return ResponseEntity.ok(new MessageResponse(
                    "Mật khẩu đã được reset nhưng gửi email thất bại: " + ex.getMessage()));
        }

        return ResponseEntity.ok(new MessageResponse(
                "Mật khẩu mới đã được gửi tới email của bạn (nếu email tồn tại trong hệ thống)."));
    }

    // --- helper methods ---
    /**
     * Loại bỏ dấu tiếng Việt và chuyển thành ký tự ASCII Ví dụ: "Nguyễn Văn Ân"
     * -> "nguyen van an"
     */
    private String removeVietnameseAccents(String str) {
        if (str == null) {
            return null;
        }
        // Chuẩn hóa Unicode NFD để tách dấu ra khỏi ký tự gốc
        String normalized = Normalizer.normalize(str, Normalizer.Form.NFD);
        // Loại bỏ các dấu combining (dấu thanh)
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(normalized).replaceAll("");
        // Xử lý các ký tự đặc biệt tiếng Việt (đ, Đ)
        result = result.replace('đ', 'd').replace('Đ', 'D');
        return result;
    }

    /**
     * Tạo username từ họ tên đầy đủ Format: tên + chữ cái đầu của họ và tên đệm
     * (không dấu) Ví dụ: "Trần Quốc Anh" -> "anhtq" "Nguyễn Thị Hương" ->
     * "huongnth"
     */
    private String generateUsernameFromFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            // fallback
            return "user" + System.currentTimeMillis();
        }
        // Loại bỏ dấu tiếng Việt trước khi xử lý
        String normalizedName = removeVietnameseAccents(fullName);
        String[] parts = normalizedName.trim().toLowerCase().split("\\s+");
        String last = parts[parts.length - 1];
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (!parts[i].isEmpty()) {
                initials.append(parts[i].charAt(0));
            }
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
