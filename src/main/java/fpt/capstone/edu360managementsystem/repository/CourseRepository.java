package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    List<Course> findBySubject_IdAndStatus(Long subjectId, CourseStatus status);

    List<Course> findByCreatedBy_Id(Long userId);

    List<Course> findByStatus(CourseStatus status);

    // Các khóa học cá nhân thuộc một giáo viên (teacher ownership)
    List<Course> findByOwnerTeacher_Id(Long teacherId);

    // Các khóa học cá nhân thuộc giáo viên theo cùng môn học và trạng thái
    List<Course> findByOwnerTeacher_IdAndSubject_IdAndStatus(Long teacherId, Long subjectId, CourseStatus status);

    // Tìm theo tiêu đề để nhận diện course của lớp đã clone (deterministic naming)
    List<Course> findByOwnerTeacher_IdAndTitle(Long teacherId, String title);

    /**
     * Phân trang và tìm kiếm courses với filter theo status, subjectId,
     * teacherId
     *
     * @param search tìm theo title, description, teacherName
     * @param status filter theo CourseStatus (DRAFT, PENDING, APPROVED,
     * ARCHIVED) - null để lấy tất cả
     * @param subjectId filter theo môn học - null để lấy tất cả
     * @param teacherUserId filter theo giáo viên tạo (user.id) - null để lấy
     * tất cả
     * @param pageable thông tin phân trang
     */
    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN c.createdBy cb
        LEFT JOIN c.subject s
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(cb.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR c.status = :status)
        AND (:subjectId IS NULL OR s.id = :subjectId)
        AND (:teacherUserId IS NULL OR cb.id = :teacherUserId)
        """)
    Page<Course> findBySearchAndFilters(
            @Param("search") String search,
            @Param("status") CourseStatus status,
            @Param("subjectId") Long subjectId,
            @Param("teacherUserId") Long teacherUserId,
            Pageable pageable
    );
}
