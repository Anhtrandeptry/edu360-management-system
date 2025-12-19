package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fpt.capstone.edu360managementsystem.dto.request.TeacherCertificateRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherEducationRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherExperienceRequest;
import fpt.capstone.edu360managementsystem.dto.request.TeacherProfileUpdateRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherProfileResponse;
import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.TeacherCertificate;
import fpt.capstone.edu360managementsystem.entity.TeacherEducation;
import fpt.capstone.edu360managementsystem.entity.TeacherExperience;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherCertificateRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherEducationRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherExperienceRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

/**
 * Service for Teacher entity operations. Provides business logic for teacher
 * management.
 */
@Service
public class TeacherService {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClazzRepository clazzRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherCertificateRepository certificateRepository;

    @Autowired
    private TeacherExperienceRepository experienceRepository;

    @Autowired
    private TeacherEducationRepository educationRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    /**
     * Get all active teachers, optionally filtered by subject. Only returns
     * teachers where user.active = true.
     *
     * @param subjectId Optional subject ID to filter teachers
     * @return List of teacher responses
     */
    public List<TeacherResponse> getTeachers(Long subjectId) {
        List<Teacher> teachers;

        if (subjectId != null) {
            teachers = teacherRepository.findActiveByAnySubject(subjectId);
        } else {
            teachers = teacherRepository.findAllActive();
        }

        return teachers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách teachers với phân trang và filter
     *
     * @param search từ khóa tìm kiếm (fullName, email, phone)
     * @param subjectId filter theo môn học
     * @param page số trang (bắt đầu từ 0)
     * @param size số phần tử mỗi trang
     * @param sortBy trường để sắp xếp
     * @param order thứ tự sắp xếp (asc, desc)
     * @return Page<TeacherResponse>
     */
    @Transactional(readOnly = true)
    public Page<TeacherResponse> getTeachersWithPagination(
            String search,
            Long subjectId,
            int page,
            int size,
            String sortBy,
            String order
    ) {
        // Xử lý sort
        Sort sort = Sort.by(sortBy != null ? sortBy : "id");
        if ("desc".equalsIgnoreCase(order)) {
            sort = sort.descending();
        } else {
            sort = sort.ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        // Query với pagination
        Page<Teacher> teacherPage = teacherRepository.findBySearchAndSubject(search, subjectId, pageable);

        // Map to response
        return teacherPage.map(this::mapToResponse);
    }

    /**
     * Map Teacher entity to TeacherResponse DTO.
     */
    private TeacherResponse mapToResponse(Teacher teacher) {
        Long subjectId = null;
        String subjectName = null;
        java.util.List<Long> subjectIds = new java.util.ArrayList<>();
        java.util.List<String> subjectNames = new java.util.ArrayList<>();
        try {
            // Nếu có subject chính populate trước
            if (teacher.getSubject() != null) {
                try {
                    subjectId = teacher.getSubject().getId();
                    subjectName = teacher.getSubject().getName();
                } catch (EntityNotFoundException ex) {
                    subjectId = null;
                    subjectName = null;
                }
            }
            // Thêm tất cả môn từ tập subjects (nếu chưa có)
            if (teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
                for (var s : teacher.getSubjects()) {
                    try {
                        Long sid = s.getId();
                        String sname = s.getName();
                        if (!subjectIds.contains(sid)) {
                            subjectIds.add(sid);
                        }
                        if (!subjectNames.contains(sname)) {
                            subjectNames.add(sname);
                        }
                    } catch (EntityNotFoundException inner) {
                        // skip
                    }
                }
                // Fallback nếu subject chính null thì lấy first từ list
                if (subjectId == null && !subjectIds.isEmpty()) {
                    subjectId = subjectIds.get(0);
                    subjectName = subjectNames.get(0);
                }
            }
        } catch (EntityNotFoundException ex) {
            // ignore, giữ nulls
        }

        long count;
        try {
            count = clazzRepository.countActiveByTeacherUser(teacher.getUser().getId());
        } catch (Exception ex) {
            count = 0L; // Defensive fallback
        }
        return TeacherResponse.builder()
                .id(teacher.getId())
                .userId(teacher.getUser().getId())
                .username(teacher.getUser().getUsername())
                .fullName(teacher.getUser().getFullName())
                .email(teacher.getUser().getEmail())
                .phoneNumber(teacher.getUser().getPhoneNumber())
                .avatarUrl(teacher.getAvatarUrl())
                .subjectId(subjectId)
                .subjectName(subjectName)
                .subjectIds(subjectIds)
                .subjectNames(subjectNames)
                .specialization(teacher.getSpecialization())
                .degree(teacher.getDegree())
                .yearsOfExperience(teacher.getYearsOfExperience())
                .rating(teacher.getRating())
                .bio(teacher.getBio())
                .workplace(teacher.getWorkplace())
                .active(teacher.getUser().getActive())
                .classCount(count)
                .build();
    }

    /**
     * Get a single teacher by the associated user id. Returns null if not found
     * (controller will translate to 404).
     */
    public TeacherResponse getByUserId(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found for userId=" + userId));
        return mapToResponse(teacher);
    }

    /**
     * Get teacher profile information
     *
     * @param userId The user ID of the teacher
     * @return Teacher profile response with all details
     */
    public TeacherProfileResponse getTeacherProfile(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found for userId=" + userId));

        User user = teacher.getUser();
        String primarySubject = teacher.getSubject() != null ? teacher.getSubject().getName() : "";

        // Get all subjects
        List<String> allSubjects = new java.util.ArrayList<>();
        if (teacher.getSubject() != null) {
            allSubjects.add(teacher.getSubject().getName());
        }
        if (teacher.getSubjects() != null && !teacher.getSubjects().isEmpty()) {
            teacher.getSubjects().stream()
                    .map(s -> s.getName())
                    .filter(name -> !allSubjects.contains(name))
                    .forEach(allSubjects::add);
        }

        long classCount = 0;
        try {
            classCount = clazzRepository.countActiveByTeacherUser(userId);
        } catch (Exception ex) {
            // ignore
        }

        // Load from separate tables instead of JSON
        List<TeacherCertificateRequest> certificates = certificateRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::mapCertificateToDto)
                .collect(Collectors.toList());

        List<TeacherExperienceRequest> experiences = experienceRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::mapExperienceToDto)
                .collect(Collectors.toList());

        List<TeacherEducationRequest> educationList = educationRepository.findByTeacherId(teacher.getId())
                .stream()
                .map(this::mapEducationToDto)
                .collect(Collectors.toList());

        return TeacherProfileResponse.builder()
                .id(teacher.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .degree(teacher.getDegree())
                .specialization(teacher.getSpecialization())
                .subject(primarySubject)
                .subjects(allSubjects)
                .workplace(teacher.getWorkplace())
                .avatarUrl(teacher.getAvatarUrl())
                .linkedinUrl(teacher.getLinkedinUrl())
                .facebookUrl(teacher.getFacebookUrl())
                .bio(teacher.getBio())
                .classCount((int) classCount)
                .studentCount(0) // Can be calculated if needed
                .yearsOfExperience(teacher.getYearsOfExperience())
                .rating(teacher.getRating())
                .certificates(certificates)
                .experiences(experiences)
                .educations(educationList)
                .achievements(teacher.getAchievements())
                .isActive(user.getActive())
                .build();
    }

    // Mapper methods
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

    /**
     * Update teacher profile information
     *
     * @param userId The user ID of the teacher
     * @param request The update request with new profile data
     * @return Updated teacher profile response
     */
    @Transactional
    public TeacherProfileResponse updateTeacherProfile(Long userId, TeacherProfileUpdateRequest request) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found for userId=" + userId));

        User user = teacher.getUser();

        // Update user fields
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        userRepository.save(user);

        // Update teacher fields
        if (request.getDegree() != null) {
            teacher.setDegree(request.getDegree().trim());
        }

        if (request.getSpecialization() != null) {
            teacher.setSpecialization(request.getSpecialization().trim());
        }

        if (request.getWorkplace() != null) {
            teacher.setWorkplace(request.getWorkplace().trim());
        }

        if (request.getLinkedinUrl() != null) {
            teacher.setLinkedinUrl(request.getLinkedinUrl().trim());
        }

        if (request.getFacebookUrl() != null) {
            teacher.setFacebookUrl(request.getFacebookUrl().trim());
        }

        if (request.getBio() != null) {
            teacher.setBio(request.getBio().trim());
        }

        if (request.getNote() != null) {
            teacher.setNote(request.getNote().trim());
        }

        if (request.getAvatarUrl() != null) {
            teacher.setAvatarUrl(request.getAvatarUrl());
        }

        // Update new fields
        if (request.getYearsOfExperience() != null) {
            teacher.setYearsOfExperience(request.getYearsOfExperience());
        }

        if (request.getRating() != null) {
            teacher.setRating(request.getRating());
        }

        if (request.getAchievements() != null) {
            teacher.setAchievements(request.getAchievements());
        }

        teacherRepository.save(teacher);

        // Update certificates - delete old and create new
        if (request.getCertificates() != null) {
            certificateRepository.deleteByTeacherId(teacher.getId());
            for (TeacherCertificateRequest certDto : request.getCertificates()) {
                TeacherCertificate cert = TeacherCertificate.builder()
                        .teacher(teacher)
                        .title(certDto.getTitle())
                        .organization(certDto.getOrganization())
                        .year(certDto.getYear())
                        .description(certDto.getDescription())
                        .build();
                certificateRepository.save(cert);
            }
        }

        // Update experiences
        if (request.getExperiences() != null) {
            experienceRepository.deleteByTeacherId(teacher.getId());
            for (TeacherExperienceRequest expDto : request.getExperiences()) {
                TeacherExperience exp = TeacherExperience.builder()
                        .teacher(teacher)
                        .position(expDto.getPosition())
                        .company(expDto.getCompany())
                        .startYear(expDto.getStartYear())
                        .endYear(expDto.getEndYear())
                        .description(expDto.getDescription())
                        .build();
                experienceRepository.save(exp);
            }
        }

        // Update educations
        if (request.getEducations() != null) {
            educationRepository.deleteByTeacherId(teacher.getId());
            for (TeacherEducationRequest eduDto : request.getEducations()) {
                TeacherEducation edu = TeacherEducation.builder()
                        .teacher(teacher)
                        .degree(eduDto.getDegree())
                        .school(eduDto.getSchool())
                        .year(eduDto.getYear())
                        .description(eduDto.getDescription())
                        .build();
                educationRepository.save(edu);
            }
        }

        // Return updated profile
        return getTeacherProfile(userId);
    }

    /**
     * Get all subjects taught by a teacher via teacher_id. The list includes
     * both primary subject and additional subjects.
     */
    public List<SubjectResponse> getSubjectsByTeacherId(Long teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Teacher not found for id=" + teacherId));

        java.util.LinkedHashSet<Subject> all = new java.util.LinkedHashSet<>();
        if (teacher.getSubject() != null) {
            all.add(teacher.getSubject());
        }
        if (teacher.getSubjects() != null) {
            all.addAll(teacher.getSubjects());
        }

        return all.stream()
                .map(s -> SubjectResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .status(s.getStatus())
                .classCount(clazzRepository.countActiveBySubject(s.getId()))
                .build())
                .collect(Collectors.toList());
    }

    /**
     * Update subjects taught by a teacher via teacher_id. Replace the
     * additional subjects set (primary subject remains unchanged).
     */
    @org.springframework.transaction.annotation.Transactional
    public List<SubjectResponse> updateTeacherSubjects(Long teacherId, List<Long> subjectIds) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Teacher not found for id=" + teacherId));

        // Tập môn mới (additional). Primary subject giữ nguyên.
        List<Subject> newAdditional = subjectRepository.findAllById(subjectIds != null ? subjectIds : java.util.List.of());
        java.util.Set<Long> newIds = newAdditional.stream().map(Subject::getId).collect(java.util.stream.Collectors.toSet());

        // Tập môn hiện tại (primary + additional)
        java.util.LinkedHashSet<Subject> currentAll = new java.util.LinkedHashSet<>();
        if (teacher.getSubject() != null) {
            currentAll.add(teacher.getSubject());
        }
        if (teacher.getSubjects() != null) {
            currentAll.addAll(teacher.getSubjects());
        }

        // Tập môn sau cập nhật (primary + new additional)
        java.util.LinkedHashSet<Subject> afterAll = new java.util.LinkedHashSet<>();
        if (teacher.getSubject() != null) {
            afterAll.add(teacher.getSubject());
        }
        afterAll.addAll(newAdditional);

        // Các môn bị loại bỏ (có trong currentAll nhưng không có trong afterAll)
        java.util.List<Subject> removed = currentAll.stream()
                .filter(s -> afterAll.stream().noneMatch(ns -> java.util.Objects.equals(ns.getId(), s.getId())))
                .collect(java.util.stream.Collectors.toList());

        // Ràng buộc: không thể loại bỏ môn nếu giáo viên đang có lớp active của môn đó
        java.util.List<String> blockingMessages = new java.util.ArrayList<>();
        for (Subject s : removed) {
            java.util.List<fpt.capstone.edu360managementsystem.entity.Clazz> activeClasses
                    = clazzRepository.findActiveByTeacherAndSubject(teacherId, s.getId());
            if (activeClasses != null && !activeClasses.isEmpty()) {
                // Gom tên lớp để hiển thị rõ ràng
                String classNames = activeClasses.stream()
                        .map(fpt.capstone.edu360managementsystem.entity.Clazz::getName)
                        .collect(java.util.stream.Collectors.joining(", "));
                blockingMessages.add(String.format(
                        "Giáo viên đang dạy lớp %s (%s), không thể chuyển môn",
                        classNames, s.getName()
                ));
            }
        }

        if (!blockingMessages.isEmpty()) {
            throw new IllegalStateException(String.join(". ", blockingMessages));
        }

        // Áp dụng cập nhật tập môn phụ
        teacher.setSubjects(new java.util.HashSet<>(newAdditional));
        teacherRepository.save(teacher);

        return getSubjectsByTeacherId(teacherId);
    }

    /**
     * Update primary subject for a teacher. Enforce business rule: cannot
     * change (remove current primary) if teacher has active classes of current
     * primary subject.
     */
    @org.springframework.transaction.annotation.Transactional
    public SubjectResponse updatePrimarySubject(Long teacherId, Long newSubjectId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Teacher not found for id=" + teacherId));

        Subject currentPrimary = teacher.getSubject();
        if (currentPrimary != null) {
            java.util.List<fpt.capstone.edu360managementsystem.entity.Clazz> activeClasses
                    = clazzRepository.findActiveByTeacherAndSubject(teacherId, currentPrimary.getId());
            if (activeClasses != null && !activeClasses.isEmpty()) {
                String classNames = activeClasses.stream()
                        .map(fpt.capstone.edu360managementsystem.entity.Clazz::getName)
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new IllegalStateException(String.format(
                        "Giáo viên đang dạy lớp %s (%s), không thể chuyển môn",
                        classNames, currentPrimary.getName()
                ));
            }
        }

        Subject newSubject = subjectRepository.findById(newSubjectId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Subject not found for id=" + newSubjectId));

        teacher.setSubject(newSubject);
        teacherRepository.save(teacher);

        return SubjectResponse.builder()
                .id(newSubject.getId())
                .name(newSubject.getName())
                .status(newSubject.getStatus())
                .classCount(clazzRepository.countActiveBySubject(newSubject.getId()))
                .build();
    }
}
