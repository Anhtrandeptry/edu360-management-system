package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {

    boolean existsByUserId(Long userId);

    /**
     * Đếm số giáo viên đang hoạt động (user.active = true)
     */
    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.user.active = true")
    long countActive();

    /**
     * Lấy tất cả giáo viên đang hoạt động (user.active = true)
     */
    @Query("SELECT t FROM Teacher t WHERE t.user.active = true")
    List<Teacher> findAllActive();

    /**
     * Lấy giáo viên đang hoạt động theo môn học
     */
    @Query("SELECT DISTINCT t FROM Teacher t LEFT JOIN t.subjects s WHERE t.user.active = true AND (t.subject.id = :subjectId OR s.id = :subjectId)")
    List<Teacher> findActiveByAnySubject(@Param("subjectId") Long subjectId);

    @Query("select distinct t from Teacher t left join t.subjects s where t.subject.id = :subjectId or s.id = :subjectId")
    List<Teacher> findByAnySubject(@Param("subjectId") Long subjectId);

    java.util.Optional<Teacher> findByUserId(Long userId);

    @Query("""
        SELECT DISTINCT t FROM Teacher t
        LEFT JOIN t.user u
        LEFT JOIN t.subjects s
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
               u.phoneNumber LIKE CONCAT('%', :search, '%'))
        AND (:subjectId IS NULL OR t.subject.id = :subjectId OR s.id = :subjectId)
        """)
    Page<Teacher> findBySearchAndSubject(
            @Param("search") String search,
            @Param("subjectId") Long subjectId,
            Pageable pageable
    );

}
