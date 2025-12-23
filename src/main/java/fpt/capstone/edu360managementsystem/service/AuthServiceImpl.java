package fpt.capstone.edu360managementsystem.service;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.ForgotPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterStudentWithParentRequest;
import fpt.capstone.edu360managementsystem.dto.request.RegisterTeacherRequest;
import fpt.capstone.edu360managementsystem.dto.request.ResetPasswordRequest;
import fpt.capstone.edu360managementsystem.dto.response.MessageResponse;
import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.PasswordResetToken;
import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.entity.Student;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.repository.ParentRepository;
import fpt.capstone.edu360managementsystem.repository.PasswordResetTokenRepository;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

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
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    EmailService emailService;

    @Value("${app.frontend.url:http://localhost:8386}")
    private String frontendUrl;

    // Token expiry time: 15 minutes
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();

    @Override
    @Transactional
    public ResponseEntity<?> registerStudentWithParent(RegisterStudentWithParentRequest request) {
        log.info("registerStudentWithParent called with existingParentId: {}", request.getExistingParentId());

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

        // 6. Nếu là phụ huynh đã tồn tại, gửi email thông báo có học sinh mới liên kết
        log.info("Checking email notification: isNewParent={}, parentUser={}, parentEmail={}",
                isNewParent,
                parent.getUser() != null ? parent.getUser().getId() : "null",
                parent.getUser() != null ? parent.getUser().getEmail() : "null");

        if (!isNewParent && parent.getUser() != null && parent.getUser().getEmail() != null) {
            try {
                String parentEmail = parent.getUser().getEmail();
                String parentName = parent.getUser().getFullName();

                log.info("Sending notification email to parent: {} ({})", parentName, parentEmail);

                String subject = "🔔 Thông báo: Có học sinh mới liên kết với tài khoản của bạn - Edu360";
                String text = String.format(
                        "Xin chào %s,\n\n"
                        + "Chúng tôi xin thông báo rằng có một học sinh mới vừa đăng ký và liên kết với tài khoản phụ huynh của bạn trên hệ thống Edu360.\n\n"
                        + "📚 THÔNG TIN HỌC SINH MỚI:\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                        + "• Họ và tên: %s\n"
                        + "• Email: %s\n"
                        + "• Số điện thoại: %s\n"
                        + "• Tên đăng nhập: %s\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                        + "⚠️ LƯU Ý QUAN TRỌNG:\n"
                        + "Nếu bạn KHÔNG biết học sinh này hoặc đây không phải con em của bạn, vui lòng:\n"
                        + "1. Đăng nhập vào hệ thống Edu360\n"
                        + "2. Liên hệ với quản trị viên để được hỗ trợ\n\n"
                        + "Nếu đây đúng là con em của bạn, bạn có thể theo dõi tiến độ học tập của học sinh thông qua tài khoản phụ huynh.\n\n"
                        + "Trân trọng,\n"
                        + "Đội ngũ Edu360\n\n"
                        + "---\n"
                        + "Email này được gửi tự động. Vui lòng không trả lời trực tiếp.",
                        parentName,
                        studentUser.getFullName(),
                        studentUser.getEmail() != null ? studentUser.getEmail() : "Chưa cung cấp",
                        studentUser.getPhoneNumber() != null ? studentUser.getPhoneNumber() : "Chưa cung cấp",
                        studentUser.getUsername()
                );

                emailService.sendSimpleMessage(parentEmail, subject, text);
                log.info("Successfully sent notification email to parent: {}", parentEmail);
            } catch (MailException ex) {
                // Không throw exception để tránh rollback, chỉ log lỗi
                log.error("Failed to send notification email to parent: {}", ex.getMessage(), ex);
            } catch (Exception ex) {
                log.error("Unexpected error sending notification email: {}", ex.getMessage(), ex);
            }
        } else {
            log.info("Skipping email notification: isNewParent={}", isNewParent);
        }

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
        String email = request.getEmail().toLowerCase().trim();

        // Tìm user theo email
        User user = userRepository.findAll().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);

        // Vì lý do bảo mật, trả message chung kể cả khi không tìm được user
        if (user == null) {
            return ResponseEntity.ok(new MessageResponse(
                    "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi."));
        }

        // Vô hiệu hóa tất cả token cũ của user này
        passwordResetTokenRepository.invalidateAllTokensForUser(user);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Tạo link reset password
        String resetLink = frontendUrl + "/home/reset-password?token=" + token;

        // Gửi email với link reset
        String subject = "Đặt lại mật khẩu tài khoản Edu360";
        String text = String.format(
                "Xin chào %s,\n\n"
                + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản trên hệ thống Edu360.\n\n"
                + "Vui lòng click vào link sau để đặt lại mật khẩu:\n%s\n\n"
                + "Link này sẽ hết hạn sau %d phút.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n"
                + "Trân trọng,\nĐội ngũ Edu360",
                user.getFullName(), resetLink, TOKEN_EXPIRY_MINUTES
        );

        try {
            emailService.sendSimpleMessage(user.getEmail(), subject, text);
        } catch (MailException ex) {
            return ResponseEntity.internalServerError().body(new MessageResponse(
                    "Không thể gửi email. Vui lòng thử lại sau."));
        }

        return ResponseEntity.ok(new MessageResponse(
                "Nếu email tồn tại trong hệ thống, link đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư."));
    }

    @Override
    @Transactional
    public ResponseEntity<?> resetPassword(ResetPasswordRequest request) {
        // Tìm token
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElse(null);

        if (resetToken == null) {
            return ResponseEntity.badRequest().body(new MessageResponse(
                    "Link đặt lại mật khẩu không hợp lệ."));
        }

        // Kiểm tra token còn hiệu lực không
        if (!resetToken.isValid()) {
            String message = resetToken.isUsed()
                    ? "Link đặt lại mật khẩu đã được sử dụng."
                    : "Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu link mới.";
            return ResponseEntity.badRequest().body(new MessageResponse(message));
        }

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Đánh dấu token đã sử dụng
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Vô hiệu hóa tất cả token khác của user (nếu có)
        passwordResetTokenRepository.invalidateAllTokensForUser(user);

        return ResponseEntity.ok(new MessageResponse(
                "Mật khẩu đã được đặt lại thành công. Vui lòng đăng nhập với mật khẩu mới."));
    }

    @Override
    public boolean validateResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
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
