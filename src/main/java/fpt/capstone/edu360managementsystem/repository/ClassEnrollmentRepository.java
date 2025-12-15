package fpt.capstone.edu360managementsystem.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByStudent_Id(Long studentId);

    // ==================== REPORT QUERIES ====================
    // Đếm số enrollment đang active (lớp PUBLIC)
    @Query("SELECT COUNT(ce) FROM ClassEnrollment ce WHERE ce.clazz.status = 'PUBLIC'")
    Long countActiveEnrollments();

    // Đếm số học sinh theo giáo viên
    @Query("SELECT COUNT(DISTINCT ce.student.id) FROM ClassEnrollment ce WHERE ce.clazz.teacher.id = :teacherId AND ce.clazz.status = 'PUBLIC'")
    Integer countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    // Đếm số học sinh theo môn học
    @Query("SELECT COUNT(DISTINCT ce.student.id) FROM ClassEnrollment ce WHERE ce.clazz.subject.id = :subjectId AND ce.clazz.status = 'PUBLIC'")
    Integer countStudentsBySubjectId(@Param("subjectId") Long subjectId);

}
