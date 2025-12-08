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

    // Count completed sessions for a class
    // A session is completed if: status = DONE OR has at least one attendance not UNMARKED
    @Query("""
      SELECT COUNT(DISTINCT s) FROM ClassSession s 
      WHERE s.clazz.id = :classId 
      AND (s.status = 'DONE' 
           OR EXISTS (SELECT 1 FROM Attendance a WHERE a.session.id = s.id AND a.status <> 'UNMARKED'))
    """)
    long countCompletedByClazzId(Long classId);

    // Batch count completed sessions for multiple classes (avoid N+1)
    // A session is completed if: status = DONE OR has at least one attendance not UNMARKED
    @Query("""
      SELECT s.clazz.id, COUNT(DISTINCT s)
      FROM ClassSession s
      WHERE s.clazz.id IN :classIds 
      AND (s.status = 'DONE' 
           OR EXISTS (SELECT 1 FROM Attendance a WHERE a.session.id = s.id AND a.status <> 'UNMARKED'))
      GROUP BY s.clazz.id
    """)
    List<Object[]> countCompletedByClazzIdIn(List<Long> classIds);

    // Batch count sessions for multiple classes (avoid N+1)
    @Query("""
      SELECT s.clazz.id, COUNT(s)
      FROM ClassSession s
      WHERE s.clazz.id IN :classIds
      GROUP BY s.clazz.id
    """)
    List<Object[]> countByClazzIdIn(List<Long> classIds);

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
