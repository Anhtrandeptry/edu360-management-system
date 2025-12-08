package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Notification;
import fpt.capstone.edu360managementsystem.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);


    List<Notification> findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(Long userId);


    long countByUser_IdAndIsReadFalse(Long userId);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("now") LocalDateTime now);


    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);


    Page<Notification> findByUser_IdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type, Pageable pageable);
}
