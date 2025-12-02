package fpt.capstone.edu360managementsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Attendance;
import fpt.capstone.edu360managementsystem.entity.ClassSession;
import fpt.capstone.edu360managementsystem.entity.Student;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findBySession_Id(Long sessionId);

    Optional<Attendance> findBySessionAndStudent(ClassSession session, Student student);

    // Batch load all attendance records for given session ids & a student
    List<Attendance> findBySession_IdInAndStudent_Id(List<Long> sessionIds, Long studentId);

    // Quick guard: check whether any attendance exists for sessions of a class
    boolean existsBySession_Clazz_Id(Long clazzId);
}
