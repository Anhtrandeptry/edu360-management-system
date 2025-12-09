package fpt.capstone.edu360managementsystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.capstone.edu360managementsystem.dto.request.GoogleAuthRequest;
import fpt.capstone.edu360managementsystem.dto.request.GoogleRegisterRequest;
import fpt.capstone.edu360managementsystem.dto.response.GoogleAuthResponse;
import fpt.capstone.edu360managementsystem.entity.*;
import fpt.capstone.edu360managementsystem.enums.ERole;
import fpt.capstone.edu360managementsystem.repository.ParentRepository;
import fpt.capstone.edu360managementsystem.repository.RoleRepository;
import fpt.capstone.edu360managementsystem.repository.StudentRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    /**
     * Xử lý Google OAuth callback
     * 1. Đổi authorization code lấy access token
     * 2. Lấy thông tin user từ Google
     * 3. Check email có trong DB không:
     *    - Có → Login thành công
     *    - Chưa có → Yêu cầu đăng ký (trả về thông tin Google)
     */
    public GoogleAuthResponse handleGoogleCallback(GoogleAuthRequest request) {
        try {
            // 1. Exchange code for access token
            String accessToken = exchangeCodeForToken(request.getCode(), request.getRedirectUri());
            if (accessToken == null) {
                return GoogleAuthResponse.builder()
                        .needsRegistration(false)
                        .message("Không thể lấy access token từ Google")
                        .build();
            }

            // 2. Get user info from Google
            Map<String, String> googleUserInfo = getGoogleUserInfo(accessToken);
            if (googleUserInfo == null) {
                return GoogleAuthResponse.builder()
                        .needsRegistration(false)
                        .message("Không thể lấy thông tin từ Google")
                        .build();
            }

            String googleEmail = googleUserInfo.get("email");
            String googleName = googleUserInfo.get("name");
            String googlePicture = googleUserInfo.get("picture");
            String googleId = googleUserInfo.get("id");

            // 3. Check if email exists in DB
            Optional<User> existingUser = userRepository.findAll().stream()
                    .filter(u -> googleEmail.equalsIgnoreCase(u.getEmail()))
                    .findFirst();

            if (existingUser.isPresent()) {
                // User exists → Login success
                User user = existingUser.get();
                List<String> roles = user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toList());

                // Get avatar from Student entity if available
                String avatarUrl = studentRepository.findByUser_Id(user.getId())
                        .map(Student::getAvatarUrl)
                        .orElse(null);

                return GoogleAuthResponse.builder()
                        .needsRegistration(false)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .roles(roles)
                        .avatarUrl(avatarUrl)
                        .message("Đăng nhập thành công")
                        .build();
            } else {
                // User doesn't exist → Need registration
                return GoogleAuthResponse.builder()
                        .needsRegistration(true)
                        .googleEmail(googleEmail)
                        .googleName(googleName)
                        .googlePicture(googlePicture)
                        .googleId(googleId)
                        .message("Email chưa được đăng ký. Vui lòng hoàn tất thông tin đăng ký.")
                        .build();
            }

        } catch (Exception e) {
            log.error("Google OAuth error: ", e);
            return GoogleAuthResponse.builder()
                    .needsRegistration(false)
                    .message("Lỗi xác thực Google: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Đăng ký tài khoản mới từ Google OAuth
     */
    @Transactional
    public GoogleAuthResponse registerWithGoogle(GoogleRegisterRequest request) {
        try {
            // Check if email already exists
            boolean emailExists = userRepository.findAll().stream()
                    .anyMatch(u -> request.getGoogleEmail().equalsIgnoreCase(u.getEmail()));
            
            if (emailExists) {
                return GoogleAuthResponse.builder()
                        .needsRegistration(false)
                        .message("Email đã được sử dụng trong hệ thống")
                        .build();
            }

            // 1. Check if parent already exists by phone
            Parent parent;
            Optional<Parent> existingParent = parentRepository.findByPhone(request.getParentPhone());
            
            if (existingParent.isPresent()) {
                // Use existing parent
                parent = existingParent.get();
                log.info("Using existing parent with phone={}, parentId={}", request.getParentPhone(), parent.getId());
            } else {
                // Create new parent user
                String parentUsername = generateUsernameFromEmail(request.getParentEmail() != null ? 
                        request.getParentEmail() : "parent_" + request.getGoogleEmail());
                String parentPassword = UUID.randomUUID().toString().substring(0, 12);
                
                User parentUser = new User();
                parentUser.setUsername(parentUsername);
                parentUser.setEmail(request.getParentEmail() != null ? request.getParentEmail() : "noemail_" + parentUsername + "@placeholder.com");
                parentUser.setPassword(passwordEncoder.encode(parentPassword));
                parentUser.setFullName(request.getParentFullName());
                parentUser.setPhoneNumber(request.getParentPhone());
                
                Role parentRole = roleRepository.findByName(ERole.ROLE_PARENT)
                        .orElseThrow(() -> new RuntimeException("Role PARENT not found"));
                parentUser.getRoles().add(parentRole);
                userRepository.save(parentUser);

                // Create Parent entity
                parent = new Parent();
                parent.setUser(parentUser);
                parent.setPhone(request.getParentPhone()); // Set phone for future reference
                parentRepository.save(parent);
                log.info("Created new parent with phone={}, parentId={}", request.getParentPhone(), parent.getId());
            }

            // 3. Create student user - Use username from request
            String studentUsername = request.getUsername();
            
            // Check if username already exists
            if (userRepository.existsByUsername(studentUsername)) {
                return GoogleAuthResponse.builder()
                        .needsRegistration(true)
                        .message("Tên đăng nhập đã tồn tại. Vui lòng chọn tên khác.")
                        .build();
            }

            User studentUser = new User();
            studentUser.setUsername(studentUsername);
            studentUser.setEmail(request.getGoogleEmail());
            studentUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 12)));
            studentUser.setFullName(request.getStudentFullName());
            studentUser.setPhoneNumber(request.getStudentPhone());
            
            Role studentRole = roleRepository.findByName(ERole.ROLE_STUDENT)
                    .orElseThrow(() -> new RuntimeException("Role STUDENT not found"));
            studentUser.getRoles().add(studentRole);
            userRepository.save(studentUser);

            // 4. Create Student entity and link to parent
            Student student = new Student();
            student.setUser(studentUser);
            student.setParent(parent);
            student.setAvatarUrl(request.getGooglePicture()); // Save Google avatar
            student = studentRepository.save(student);
            log.info("Created Student record with ID={}, userId={}, parentId={}", 
                    student.getId(), studentUser.getId(), parent.getId());

            // 5. Return success response
            List<String> roles = studentUser.getRoles().stream()
                    .map(r -> r.getName().name())
                    .toList();

            return GoogleAuthResponse.builder()
                    .needsRegistration(false)
                    .userId(studentUser.getId())
                    .username(studentUser.getUsername())
                    .fullName(studentUser.getFullName())
                    .email(studentUser.getEmail())
                    .roles(roles)
                    .avatarUrl(request.getGooglePicture())
                    .message("Đăng ký thành công!")
                    .build();

        } catch (Exception e) {
            log.error("Google register error: ", e);
            return GoogleAuthResponse.builder()
                    .needsRegistration(true)
                    .message("Lỗi đăng ký: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Exchange authorization code for access token
     */
    private String exchangeCodeForToken(String code, String redirectUri) {
        try {
            // URL decode the code in case it contains encoded characters
            String decodedCode = java.net.URLDecoder.decode(code, java.nio.charset.StandardCharsets.UTF_8);
            
            log.info("Exchanging code for token...");
            log.info("Client ID: {}", clientId);
            log.info("Client Secret: {}", clientSecret); // Log full secret for debugging (remove in production!)
            log.info("Redirect URI: {}", redirectUri);
            log.info("Original Code (first 30 chars): {}", code != null && code.length() > 30 ? code.substring(0, 30) + "..." : code);
            log.info("Decoded Code (first 30 chars): {}", decodedCode != null && decodedCode.length() > 30 ? decodedCode.substring(0, 30) + "..." : decodedCode);
            
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", decodedCode);
            params.add("client_id", clientId.trim());
            params.add("client_secret", clientSecret.trim());
            params.add("redirect_uri", redirectUri.trim());
            params.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, String.class);

            log.info("Google token response status: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("access_token").asText();
            } else {
                log.error("Google token response body: {}", response.getBody());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP Error from Google: Status={}, Response Body={}", e.getStatusCode(), e.getResponseBodyAsString());
            log.error("Headers: {}", e.getResponseHeaders());
            throw new RuntimeException("Google OAuth error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error exchanging code for token: {}", e.getMessage());
            log.error("Full error: ", e);
            throw new RuntimeException("Failed to exchange code for token: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get user info from Google using access token
     */
    private Map<String, String> getGoogleUserInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                Map<String, String> userInfo = new HashMap<>();
                userInfo.put("id", jsonNode.has("id") ? jsonNode.get("id").asText() : "");
                userInfo.put("email", jsonNode.has("email") ? jsonNode.get("email").asText() : "");
                userInfo.put("name", jsonNode.has("name") ? jsonNode.get("name").asText() : "");
                userInfo.put("picture", jsonNode.has("picture") ? jsonNode.get("picture").asText() : "");
                return userInfo;
            }
        } catch (Exception e) {
            log.error("Error getting Google user info: ", e);
        }
        return null;
    }

    /**
     * Generate username from email
     */
    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase();
        
        // Check if username exists, add random suffix if needed
        String username = baseUsername;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        return username;
    }

    /**
     * Check if parent phone exists and return parent info
     */
    public Map<String, Object> checkParentPhone(String phone) {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Parent> parentOpt = parentRepository.findByPhone(phone);
        
        if (parentOpt.isPresent()) {
            Parent parent = parentOpt.get();
            response.put("exists", true);
            
            Map<String, Object> parentInfo = new HashMap<>();
            parentInfo.put("fullName", parent.getFullName());
            parentInfo.put("email", parent.getEmail());
            
            // Count children (students) linked to this parent
            long childCount = studentRepository.countByParent(parent);
            parentInfo.put("childCount", childCount);
            
            response.put("parentInfo", parentInfo);
        } else {
            response.put("exists", false);
        }
        
        return response;
    }
}
