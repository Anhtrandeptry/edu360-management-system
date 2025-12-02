package fpt.capstone.edu360managementsystem.repository;

import java.time.LocalDate;
import java.util.List;

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

    // Avoid unique-result expectation; classes may have multiple sessions per day
    List<ClassSession> findAllByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(Long classId, LocalDate date);

    // When a class has multiple sessions, duplicates may exist due to legacy generation
    // Prefer list-returning methods and let service pick a stable one
    List<ClassSession> findAllByClazz_IdAndDateAndTimeSlot_IdOrderByIdAsc(Long classId, LocalDate date, Long timeSlotId);

    List<ClassSession> findByClazz_IdAndDateOrderByTimeSlot_StartTimeAsc(Long classId, LocalDate date);

    // Count all sessions for a class (used for tuition/payment calculation)
    long countByClazz_Id(Long classId);

    boolean existsByClazz_IdAndDateBefore(Long classId, LocalDate date);

    // Thêm tiện ích lấy toàn bộ session theo class để xoá/regenerate khi chỉnh sửa draft
    List<ClassSession> findByClazz_Id(Long classId);
}
