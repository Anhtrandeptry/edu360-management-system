package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Clazz;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long> {

    boolean existsByNameAndSubject_IdAndSemester_Id(String name, Long subjectId, Long semesterId);

    // Trùng lịch giáo viên (cùng học kỳ, trùng (dayOfWeek, timeSlot))
    @Query("""
        select distinct c from Clazz c
        where c.teacher.id = :teacherId
          and c.semester.id = :semesterId
          and exists (
            select s from ClassSchedule s
            where s.clazz = c
              and s.dayOfWeek in :dow
              and s.timeSlot.id in :slotIds
          )
    """)
    List<Clazz> findTeacherConflicts(Long teacherId, Long semesterId,
            java.util.Set<Integer> dow,
            java.util.Set<Long> slotIds);

    // Trùng lịch phòng
    @Query("""
        select distinct c from Clazz c
        where c.room.id = :roomId
          and c.semester.id = :semesterId
          and exists (
            select s from ClassSchedule s
            where s.clazz = c
              and s.dayOfWeek in :dow
              and s.timeSlot.id in :slotIds
          )
    """)
    List<Clazz> findRoomConflicts(Long roomId, Long semesterId,
            java.util.Set<Integer> dow,
            java.util.Set<Long> slotIds);

    // Lấy danh sách lớp kèm schedules để filter theo giáo viên (userId) và timeslot
    @Query("""
    select distinct c from Clazz c
    left join fetch c.teacher t
    left join fetch c.subject sj
    left join fetch c.room r
    left join fetch c.semester sem
    left join fetch c.teacher.user tu
    where (:teacherUserId is null or tu.id = :teacherUserId)
  """)
    List<Clazz> findAllWithFilters(Long teacherUserId);
}
