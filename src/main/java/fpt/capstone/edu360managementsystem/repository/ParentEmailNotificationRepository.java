package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fpt.capstone.edu360managementsystem.entity.ParentEmailNotification;

public interface ParentEmailNotificationRepository extends JpaRepository<ParentEmailNotification, Long> {

    /**
     * Kiểm tra đã gửi email cho student vào ngày cụ thể chưa
     */
    boolean existsByStudent_IdAndNotificationDate(Long studentId, LocalDate notificationDate);

    /**
     * Tìm notification theo student và ngày
     */
    Optional<ParentEmailNotification> findByStudent_IdAndNotificationDate(Long studentId, LocalDate notificationDate);
}
