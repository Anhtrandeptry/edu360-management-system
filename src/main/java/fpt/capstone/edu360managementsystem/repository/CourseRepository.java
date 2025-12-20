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

    // Đếm số khóa học theo subjectId (trạng thái APPROVED)
    @Query("SELECT COUNT(c) FROM Course c WHERE c.subject.id = :subjectId AND c.status = 'APPROVED'")
    long countApprovedBySubjectId(@Param("subjectId") Long subjectId);

    List<Course> findByCreatedBy_Id(Long userId);

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByOwnerTeacher_Id(Long teacherId);

    List<Course> findByOwnerTeacher_IdAndSubject_IdAndStatus(Long teacherId, Long subjectId, CourseStatus status);

    List<Course> findByOwnerTeacher_IdAndTitle(Long teacherId, String title);

    @Query("""
        SELECT DISTINCT c FROM Course c
        LEFT JOIN c.createdBy cb
        LEFT JOIN c.subject s
        LEFT JOIN c.ownerTeacher ot
        WHERE c.ownerTeacher IS NOT NULL
        AND (:search IS NULL OR :search = '' OR 
               LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(cb.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR c.status = :status)
        AND (:subjectId IS NULL OR s.id = :subjectId)
        AND (:teacherUserId IS NULL OR ot.user.id = :teacherUserId)
        """)
    Page<Course> findBySearchAndFilters(
            @Param("search") String search,
            @Param("status") CourseStatus status,
            @Param("subjectId") Long subjectId,
            @Param("teacherUserId") Long teacherUserId,
            Pageable pageable
    );
}
