package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    @Query("""
      select s from ClassSession s
      where s.date = :date
        and s.clazz.teacher.id = :teacherId
      order by s.timeSlot.startTime asc
    """)
    List<ClassSession> findTodaySessionsForTeacher(Long teacherId, LocalDate date);
}
