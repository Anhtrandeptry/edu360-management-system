package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findBySession_Id(Long sessionId);
    Optional<Attendance> findBySessionAndStudent(ClassSession session, Student student);
    // Batch load all attendance records for given session ids & a student
    List<Attendance> findBySession_IdInAndStudent_Id(List<Long> sessionIds, Long studentId);
}
