package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>, JpaSpecificationExecutor<Subject> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Subject> findByStatus(SubjectStatus status);

    /**
     * Phân trang và tìm kiếm subjects với filter theo status
     *
     * @param search tìm theo name
     * @param status filter theo status (AVAILABLE, UNAVAILABLE) - null để lấy
     * tất cả
     * @param pageable thông tin phân trang
     */
    @Query("""
        SELECT s FROM Subject s
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR s.status = :status)
        """)
    Page<Subject> findBySearchAndStatus(
            @Param("search") String search,
            @Param("status") SubjectStatus status,
            Pageable pageable
    );

}
