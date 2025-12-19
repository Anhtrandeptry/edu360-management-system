package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.ParentEmailNotification;
import fpt.capstone.edu360managementsystem.entity.Student;

public interface ParentEmailNotificationRepository extends JpaRepository<ParentEmailNotification, Long> {


    boolean existsByStudent_IdAndNotificationDate(Long studentId, LocalDate notificationDate);


    Optional<ParentEmailNotification> findByStudent_IdAndNotificationDate(Long studentId, LocalDate notificationDate);

    // Count unread notifications for parent
    long countByParentAndReadFalse(Parent parent);

    // Get all notifications for parent
    List<ParentEmailNotification> findByParentOrderByCreatedAtDesc(Parent parent);
    List<ParentEmailNotification> findByParentAndReadTrueOrderByCreatedAtDesc(Parent parent);
    List<ParentEmailNotification> findByParentAndReadFalseOrderByCreatedAtDesc(Parent parent);

    // Get notifications by parent and student
    List<ParentEmailNotification> findByParentAndStudentOrderByCreatedAtDesc(Parent parent, Student student);
    List<ParentEmailNotification> findByParentAndStudentAndReadTrueOrderByCreatedAtDesc(Parent parent, Student student);
    List<ParentEmailNotification> findByParentAndStudentAndReadFalseOrderByCreatedAtDesc(Parent parent, Student student);
}
