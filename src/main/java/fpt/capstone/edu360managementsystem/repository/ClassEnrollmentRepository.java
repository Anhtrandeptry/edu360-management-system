package fpt.capstone.edu360managementsystem.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.entity.Student;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {

    List<ClassEnrollment> findByClazz_Id(Long classId);

    int countByClazz_Id(Long classId);

    void deleteByClazz_IdAndStudent_Id(Long classId, Long studentId);

    boolean existsByClazzAndStudent(Clazz clazz, Student student);

    List<ClassEnrollment> findByStudent_IdAndClazz_Semester_Id(Long studentId, Long semesterId);

    @Query("""
      select ce from ClassEnrollment ce
      where ce.student.id = :studentId
        and ce.clazz.semester.id = :semesterId
        and exists (
          select s from ClassSchedule s
          where s.clazz = ce.clazz
            and s.dayOfWeek in :dow
            and s.timeSlot.id in :slotIds
        )
    """)
    List<ClassEnrollment> findScheduleConflicts(Long studentId, Long semesterId,
            Set<Integer> dow, Set<Long> slotIds);

    List<ClassEnrollment> findByStudent_Id(Long studentId);

}
