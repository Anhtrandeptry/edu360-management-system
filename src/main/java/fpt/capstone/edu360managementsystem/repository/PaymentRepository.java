package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;

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

    // Thống kê tổng tiền đã thanh toán
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID'")
    Long sumPaidAmount();

    // Đếm payment theo status
    long countByStatus(PaymentStatus status);

    // Payments của 1 student
    List<Payment> findByStudent_IdOrderByCreatedAtDesc(Long studentId);

    // ==================== REPORT QUERIES ====================
    // Tổng doanh thu chờ thanh toán
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PENDING'")
    Long sumPendingAmount();

    // Doanh thu đã thanh toán trong khoảng thời gian
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID' AND p.paidAt >= :from AND p.paidAt <= :to")
    Long sumPaidAmountBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Đếm payment trong khoảng thời gian
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.paidAt >= :from AND p.paidAt <= :to")
    Long countByStatusBetween(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Doanh thu theo giáo viên (đã thanh toán)
    @Query("""
        SELECT p.clazz.teacher.id, 
               p.clazz.teacher.user.id,
               p.clazz.teacher.user.fullName, 
               p.clazz.teacher.user.email,
               COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.amount ELSE 0 END), 0)
        FROM Payment p
        GROUP BY p.clazz.teacher.id, p.clazz.teacher.user.id, p.clazz.teacher.user.fullName, 
                 p.clazz.teacher.user.email
        ORDER BY SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END) DESC
    """)
    List<Object[]> getRevenueByTeacher();

    // Doanh thu theo môn học
    @Query("""
        SELECT p.clazz.subject.id,
               p.clazz.subject.name,
               COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0)
        FROM Payment p
        GROUP BY p.clazz.subject.id, p.clazz.subject.name
        ORDER BY SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END) DESC
    """)
    List<Object[]> getRevenueBySubject();

    // Doanh thu theo ngày (trong khoảng thời gian)
    // Sử dụng COALESCE để fallback từ paidAt sang createdAt nếu paidAt null
    @Query("""
        SELECT CAST(COALESCE(p.paidAt, p.createdAt) AS LocalDate), COALESCE(SUM(p.amount), 0), COUNT(p)
        FROM Payment p
        WHERE p.status = 'PAID' AND COALESCE(p.paidAt, p.createdAt) >= :from AND COALESCE(p.paidAt, p.createdAt) <= :to
        GROUP BY CAST(COALESCE(p.paidAt, p.createdAt) AS LocalDate)
        ORDER BY CAST(COALESCE(p.paidAt, p.createdAt) AS LocalDate)
    """)
    List<Object[]> getRevenueByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // Doanh thu theo lớp
    @Query("""
        SELECT p.clazz.id,
               p.clazz.name,
               p.clazz.teacher.user.fullName,
               p.clazz.subject.name,
               p.clazz.maxStudents,
               COALESCE(SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END), 0),
               COALESCE(SUM(CASE WHEN p.status = 'PENDING' THEN p.amount ELSE 0 END), 0),
               COUNT(p),
               SUM(CASE WHEN p.status = 'PAID' THEN 1 ELSE 0 END),
               p.clazz.meetingLink
        FROM Payment p
        WHERE p.clazz.status = 'PUBLIC'
        GROUP BY p.clazz.id, p.clazz.name, p.clazz.teacher.user.fullName, 
                 p.clazz.subject.name, p.clazz.maxStudents, p.clazz.meetingLink
        ORDER BY SUM(CASE WHEN p.status = 'PAID' THEN p.amount ELSE 0 END) DESC
    """)
    List<Object[]> getRevenueByClass();

    // Đếm số học sinh distinct có payment tạo sau thời điểm (đại diện cho học sinh mới đăng ký)
    @Query("SELECT COUNT(DISTINCT p.student.id) FROM Payment p WHERE p.createdAt >= :after")
    Long countDistinctStudentsCreatedAfter(@Param("after") LocalDateTime after);
}
