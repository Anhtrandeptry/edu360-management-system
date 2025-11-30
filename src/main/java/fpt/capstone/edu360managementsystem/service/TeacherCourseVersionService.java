package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import fpt.capstone.edu360managementsystem.dto.request.CreateTeacherCourseVersionRequest;
import fpt.capstone.edu360managementsystem.dto.response.TeacherCourseVersionResponse;
import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.entity.TeacherCourseVersion;
import fpt.capstone.edu360managementsystem.repository.CourseRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherCourseVersionRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;

@Service
public class TeacherCourseVersionService {

    private static final Logger log = LoggerFactory.getLogger(TeacherCourseVersionService.class);

    @Autowired
    private TeacherCourseVersionRepository teacherCourseVersionRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private TeacherRepository teacherRepository;

    @Transactional
    public TeacherCourseVersionResponse createMapping(Long userId, CreateTeacherCourseVersionRequest req) {
        Long teacherId = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy giáo viên từ tài khoản này"))
                .getId();
        log.info("🔵 createMapping teacherId={}, baseCourseId={}, teacherCourseId={}", teacherId, req.getBaseCourseId(), req.getTeacherCourseId());

        if (teacherCourseVersionRepository.existsByBaseCourse_IdAndTeacherCourse_IdAndTeacher_Id(req.getBaseCourseId(), req.getTeacherCourseId(), teacherId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mapping đã tồn tại");
        }

        Course baseCourse = courseRepository.findById(req.getBaseCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học gốc"));
        Course teacherCourse = courseRepository.findById(req.getTeacherCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học cá nhân"));

        // Kiểm tra ownership: khóa học cá nhân phải do giáo viên này sở hữu (ownerTeacher != null)
        if (teacherCourse.getOwnerTeacher() == null || !teacherCourse.getOwnerTeacher().getId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Khóa học cá nhân không thuộc giáo viên này");
        }

        TeacherCourseVersion saved = teacherCourseVersionRepository.save(TeacherCourseVersion.builder()
                .baseCourse(baseCourse)
                .teacherCourse(teacherCourse)
                .teacher(teacherCourse.getOwnerTeacher())
                .build());

        log.info("✅ createMapping thành công id={}", saved.getId());
        return TeacherCourseVersionResponse.builder()
                .id(saved.getId())
                .baseCourseId(baseCourse.getId())
                .teacherCourseId(teacherCourse.getId())
                .teacherId(teacherId)
                .teacherCourseTitle(teacherCourse.getTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherCourseVersionResponse> listMappings(Long userId, Long baseCourseId) {
        // Log current user and base course input
        log.info("🔍 Fetch personal course versions: baseCourseId={}, teacherUserId={}", baseCourseId, userId);

        Long teacherId = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy giáo viên từ tài khoản này"))
                .getId();
        log.info("🔍 Resolved teacherId used for query: {}", teacherId);
        log.info("📌 Fetch personal course versions: baseCourseId={}, teacherId={}", baseCourseId, teacherId);
        log.info("➡ SQL Query executed => SELECT * FROM teacher_course_versions WHERE base_course_id={} AND teacher_id={} ", baseCourseId, teacherId);

        // Chỉ trả về các phiên bản CÓ MAPPING đúng theo teacher_id + base_course_id.
        // Không lọc theo status phê duyệt ở luồng khóa học cá nhân (theo nghiệp vụ mô tả),
        // và KHÔNG dùng fallback, vì dropdown chỉ phải hiển thị đúng các phiên bản đã chỉnh từ khóa học gốc A-1.
        List<TeacherCourseVersion> mappings = teacherCourseVersionRepository
                .findByBaseCourse_IdAndTeacher_Id(baseCourseId, teacherId);

        if (mappings == null || mappings.isEmpty()) {
            log.warn("❌ Không tìm thấy phiên bản cá nhân đã ghép cho baseCourseId={} và teacherId={}", baseCourseId, teacherId);
            return List.of();
        }

        // Log summary and per-item mapping
        log.info("🔍 Found teacher course versions: {} versions available", mappings.size());
        for (TeacherCourseVersion m : mappings) {
            Long tCourseId = m.getTeacherCourse() != null ? m.getTeacherCourse().getId() : null;
            String tCourseTitle = m.getTeacherCourse() != null ? m.getTeacherCourse().getTitle() : null;
            log.info("✔ Version {} linked with baseCourseId {} for teacherId {} (title='{}')", tCourseId, baseCourseId, teacherId, tCourseTitle);
            // Optional: additional checks can be added here (e.g., ownership, visibility)
        }

        return mappings.stream()
                .map(m -> TeacherCourseVersionResponse.builder()
                .id(m.getId())
                .baseCourseId(m.getBaseCourse().getId())
                .teacherCourseId(m.getTeacherCourse().getId())
                .teacherId(teacherId)
                .teacherCourseTitle(m.getTeacherCourse().getTitle())
                .build())
                .toList();
    }
}
