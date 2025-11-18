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
}
