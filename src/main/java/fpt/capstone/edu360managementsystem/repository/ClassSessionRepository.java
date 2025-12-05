package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.ClassSession;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    @Query("""
      select s from ClassSession s
      where s.date = :date
        and s.clazz.teacher.id = :teacherId
      order by s.timeSlot.startTime asc
    """)
    List<ClassSession> findTodaySessionsForTeacher(Long teacherId, LocalDate date);

    List<ClassSession> findByClazz_IdInAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            List<Long> classIds,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<ClassSession> findByClazz_IdAndDate(Long classId, LocalDate date);

    // When a class has multiple sessions in the same day (different time slots),
    // use slot-aware lookups to avoid IncorrectResultSizeDataAccessException
    Optional<ClassSession> findByClazz_IdAndDateAndTimeSlot_Id(Long classId, LocalDate date, Long timeSlotId);

    List<ClassSession> findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(Long classId, LocalDate date);

    // Count all sessions for a class (used for tuition/payment calculation)
    long countByClazz_Id(Long classId);

    boolean existsByClazz_IdAndDateBefore(Long classId, LocalDate date);

    // Thêm tiện ích lấy toàn bộ session theo class để xoá/regenerate khi chỉnh sửa draft
    List<ClassSession> findByClazz_Id(Long classId);
    
    // Lấy toàn bộ sessions của một class, sắp xếp theo ngày và slot
    List<ClassSession> findByClazz_IdOrderByDateAscTimeSlot_StartTimeAsc(Long classId);
    
    // Lấy sessions trong khoảng thời gian cho một lớp (dùng cho teacher attendance)
    List<ClassSession> findByClazz_IdAndDateBetweenOrderByDateAscTimeSlot_StartTimeAsc(
            Long classId,
            LocalDate startDate,
            LocalDate endDate
    );
}
