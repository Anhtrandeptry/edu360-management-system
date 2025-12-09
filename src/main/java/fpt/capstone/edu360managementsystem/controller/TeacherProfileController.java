package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.request.ChangePasswordRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherCertificateRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherEducationRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherExperienceRequest;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.TeacherCertificate;
import fpt.capstone.edu360managementsystem.entity.TeacherEducation;
import fpt.capstone.edu360managementsystem.entity.TeacherExperience;
import fpt.capstone.edu360managementsystem.repository.TeacherCertificateRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherEducationRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherExperienceRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import fpt.capstone.edu360managementsystem.service.CloudinaryService;
import fpt.capstone.edu360managementsystem.service.UserDetailsImpl;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teachers/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Slf4j
public class TeacherProfileController {

    private final TeacherRepository teacherRepository;
    private final TeacherCertificateRepository certificateRepository;
    private final TeacherExperienceRepository experienceRepository;
    private final TeacherEducationRepository educationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    private Teacher getAuthenticatedTeacher(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new SecurityException("User not authenticated");
        }

        Long userId = ((UserDetailsImpl) auth.getPrincipal()).getId();
        return teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found for user: " + userId));
    }

    // ===================== AVATAR UPLOAD =====================
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB
    private static final String ERROR_KEY = "error";

    @PostMapping("/upload-avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        log.info("Upload avatar request received. File: {}, Size: {}",
                file.getOriginalFilename(), file.getSize());
        try {
            // Validate file
            if (file.isEmpty()) {
                log.warn("File is empty");
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File is empty"));
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("File size exceeds 5MB: {}", file.getSize());
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File size exceeds 5MB"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                log.warn("Invalid content type: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "File must be an image"));
            }

            // Upload to Cloudinary
            log.info("Uploading to Cloudinary...");
            String fileUrl = cloudinaryService.uploadImage(file, "avatars");
            log.info("Upload successful. URL: {}", fileUrl);

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(ERROR_KEY, "Failed to upload file: " + e.getMessage()));
        }
    }

    // ===================== SECURITY (CHANGE PASSWORD) =====================
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            Teacher teacher = getAuthenticatedTeacher(auth);
            var user = teacher.getUser();

            // Validate current password
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu hiện tại không đúng"));
            }

            // Validate confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mật khẩu xác nhận không khớp"));
            }

            // Persist new password
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
        }
    }

    // ===================== CERTIFICATES =====================
    @GetMapping("/certificates")
    public ResponseEntity<List<TeacherCertificateRequest>> getMyCertificates(Authentication auth) {
        Teacher teacher = getAuthenticatedTeacher(auth);
        List<TeacherCertificate> certificates = certificateRepository.findByTeacherId(teacher.getId());
        List<TeacherCertificateRequest> response = certificates.stream()
                .map(this::mapCertificateToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/certificates")
    public ResponseEntity<TeacherCertificateRequest> addCertificate(
            Authentication auth,
            @RequestBody TeacherCertificateRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherCertificate certificate = TeacherCertificate.builder()
                .teacher(teacher)
                .title(request.getTitle())
                .organization(request.getOrganization())
                .year(request.getYear())
                .description(request.getDescription())
                .build();

        certificate = certificateRepository.save(certificate);
        return ResponseEntity.ok(mapCertificateToDto(certificate));
    }

    @PutMapping("/certificates/{certId}")
    public ResponseEntity<TeacherCertificateRequest> updateCertificate(
            Authentication auth,
            @PathVariable Long certId,
            @RequestBody TeacherCertificateRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherCertificate certificate = certificateRepository.findById(certId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate not found with id: " + certId));

        if (!certificate.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        certificate.setTitle(request.getTitle());
        certificate.setOrganization(request.getOrganization());
        certificate.setYear(request.getYear());
        certificate.setDescription(request.getDescription());

        certificate = certificateRepository.save(certificate);
        return ResponseEntity.ok(mapCertificateToDto(certificate));
    }

    @DeleteMapping("/certificates/{certId}")
    public ResponseEntity<Void> deleteCertificate(
            Authentication auth,
            @PathVariable Long certId) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherCertificate certificate = certificateRepository.findById(certId)
                .orElseThrow(() -> new EntityNotFoundException("Certificate not found with id: " + certId));

        if (!certificate.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        certificateRepository.deleteById(certId);
        return ResponseEntity.noContent().build();
    }

    // ===================== EXPERIENCES =====================
    @GetMapping("/experiences")
    public ResponseEntity<List<TeacherExperienceRequest>> getMyExperiences(Authentication auth) {
        Teacher teacher = getAuthenticatedTeacher(auth);
        List<TeacherExperience> experiences = experienceRepository.findByTeacherId(teacher.getId());
        List<TeacherExperienceRequest> response = experiences.stream()
                .map(this::mapExperienceToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/experiences")
    public ResponseEntity<TeacherExperienceRequest> addExperience(
            Authentication auth,
            @RequestBody TeacherExperienceRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherExperience experience = TeacherExperience.builder()
                .teacher(teacher)
                .position(request.getPosition())
                .company(request.getCompany())
                .startYear(request.getStartYear())
                .endYear(request.getEndYear())
                .description(request.getDescription())
                .build();

        experience = experienceRepository.save(experience);
        return ResponseEntity.ok(mapExperienceToDto(experience));
    }

    @PutMapping("/experiences/{expId}")
    public ResponseEntity<TeacherExperienceRequest> updateExperience(
            Authentication auth,
            @PathVariable Long expId,
            @RequestBody TeacherExperienceRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherExperience experience = experienceRepository.findById(expId)
                .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + expId));

        if (!experience.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        experience.setPosition(request.getPosition());
        experience.setCompany(request.getCompany());
        experience.setStartYear(request.getStartYear());
        experience.setEndYear(request.getEndYear());
        experience.setDescription(request.getDescription());

        experience = experienceRepository.save(experience);
        return ResponseEntity.ok(mapExperienceToDto(experience));
    }

    @DeleteMapping("/experiences/{expId}")
    public ResponseEntity<Void> deleteExperience(
            Authentication auth,
            @PathVariable Long expId) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherExperience experience = experienceRepository.findById(expId)
                .orElseThrow(() -> new EntityNotFoundException("Experience not found with id: " + expId));

        if (!experience.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        experienceRepository.deleteById(expId);
        return ResponseEntity.noContent().build();
    }

    // ===================== EDUCATION =====================
    @GetMapping("/educations")
    public ResponseEntity<List<TeacherEducationRequest>> getMyEducations(Authentication auth) {
        Teacher teacher = getAuthenticatedTeacher(auth);
        List<TeacherEducation> educations = educationRepository.findByTeacherId(teacher.getId());
        List<TeacherEducationRequest> response = educations.stream()
                .map(this::mapEducationToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/educations")
    public ResponseEntity<TeacherEducationRequest> addEducation(
            Authentication auth,
            @RequestBody TeacherEducationRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherEducation education = TeacherEducation.builder()
                .teacher(teacher)
                .degree(request.getDegree())
                .school(request.getSchool())
                .year(request.getYear())
                .description(request.getDescription())
                .build();

        education = educationRepository.save(education);
        return ResponseEntity.ok(mapEducationToDto(education));
    }

    @PutMapping("/educations/{eduId}")
    public ResponseEntity<TeacherEducationRequest> updateEducation(
            Authentication auth,
            @PathVariable Long eduId,
            @RequestBody TeacherEducationRequest request) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherEducation education = educationRepository.findById(eduId)
                .orElseThrow(() -> new EntityNotFoundException("Education not found with id: " + eduId));

        if (!education.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        education.setDegree(request.getDegree());
        education.setSchool(request.getSchool());
        education.setYear(request.getYear());
        education.setDescription(request.getDescription());

        education = educationRepository.save(education);
        return ResponseEntity.ok(mapEducationToDto(education));
    }

    @DeleteMapping("/educations/{eduId}")
    public ResponseEntity<Void> deleteEducation(
            Authentication auth,
            @PathVariable Long eduId) {

        Teacher teacher = getAuthenticatedTeacher(auth);

        TeacherEducation education = educationRepository.findById(eduId)
                .orElseThrow(() -> new EntityNotFoundException("Education not found with id: " + eduId));

        if (!education.getTeacher().getId().equals(teacher.getId())) {
            return ResponseEntity.status(403).build();
        }

        educationRepository.deleteById(eduId);
        return ResponseEntity.noContent().build();
    }

    // ===================== MAPPER METHODS =====================
    private TeacherCertificateRequest mapCertificateToDto(TeacherCertificate entity) {
        return TeacherCertificateRequest.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .organization(entity.getOrganization())
                .year(entity.getYear())
                .description(entity.getDescription())
                .build();
    }

    private TeacherExperienceRequest mapExperienceToDto(TeacherExperience entity) {
        return TeacherExperienceRequest.builder()
                .id(entity.getId())
                .position(entity.getPosition())
                .company(entity.getCompany())
                .startYear(entity.getStartYear())
                .endYear(entity.getEndYear())
                .description(entity.getDescription())
                .build();
    }

    private TeacherEducationRequest mapEducationToDto(TeacherEducation entity) {
        return TeacherEducationRequest.builder()
                .id(entity.getId())
                .degree(entity.getDegree())
                .school(entity.getSchool())
                .year(entity.getYear())
                .description(entity.getDescription())
                .build();
    }
}
