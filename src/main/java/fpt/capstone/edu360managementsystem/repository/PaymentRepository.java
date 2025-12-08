package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByClazz_IdAndStudent_Id(Long classId, Long studentId);

    boolean existsByClazz_IdAndStudent_IdAndStatus(Long classId, Long studentId, PaymentStatus status);

    Optional<Payment> findByOrderCode(String orderCode);

    // Admin: list all with optional filters
    @Query("""
        SELECT p FROM Payment p
        JOIN p.student s
        JOIN s.user u
        JOIN p.clazz c
        WHERE (:status IS NULL OR p.status = :status)
          AND (:studentName IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :studentName, '%')))
          AND (:classId IS NULL OR c.id = :classId)
          AND (:from IS NULL OR p.createdAt >= :from)
          AND (:to IS NULL OR p.createdAt <= :to)
        ORDER BY p.createdAt DESC
    """)
    Page<Payment> findAllWithFilters(
            @Param("status") PaymentStatus status,
            @Param("studentName") String studentName,
            @Param("classId") Long classId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    Long sumPaidAmount();


    long countByStatus(PaymentStatus status);


    List<Payment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);
}
