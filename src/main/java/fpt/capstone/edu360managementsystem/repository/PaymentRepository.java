package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Payment;
import fpt.capstone.edu360managementsystem.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByClazz_IdAndStudent_Id(Long classId, Long studentId);

    boolean existsByClazz_IdAndStudent_IdAndStatus(Long classId, Long studentId, PaymentStatus status);

    Optional<Payment> findByOrderCode(String orderCode);
}
