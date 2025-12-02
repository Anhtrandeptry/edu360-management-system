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

    // Trùng lịch giáo viên (theo khoảng thời gian startDate-endDate)
    @Query("""
        select distinct c from Clazz c
        where c.teacher.id = :teacherId
          and ((c.startDate <= :endDate and c.endDate >= :startDate))
          and exists (
            select s from ClassSchedule s
            where s.clazz = c
              and s.dayOfWeek in :dow
              and s.timeSlot.id in :slotIds
          )
    """)
    List<Clazz> findTeacherConflictsByDateRange(Long teacherId,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
            java.util.Set<Integer> dow,
            java.util.Set<Long> slotIds);

    // Trùng lịch phòng (theo khoảng thời gian startDate-endDate)
    @Query("""
        select distinct c from Clazz c
        where c.room.id = :roomId
          and ((c.startDate <= :endDate and c.endDate >= :startDate))
          and exists (
            select s from ClassSchedule s
            where s.clazz = c
              and s.dayOfWeek in :dow
              and s.timeSlot.id in :slotIds
          )
    """)
    List<Clazz> findRoomConflictsByDateRange(Long roomId,
            java.time.LocalDate startDate, java.time.LocalDate endDate,
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

    // Get all classes with schedules eagerly loaded for schedule management
    @Query("""
    select distinct c from Clazz c
    left join fetch c.teacher t
    left join fetch c.subject sj
    left join fetch c.room r
    left join fetch c.semester sem
    left join fetch c.teacher.user tu
  """)
    List<Clazz> findAllWithSchedules();

    // Đếm số lớp (chưa ARCHIVED) đang dùng subject
    @Query("""
        select count(c) from Clazz c
        where c.subject.id = :subjectId
          and c.status <> fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveBySubject(Long subjectId);

    // Đếm số lớp (chưa ARCHIVED) đang dùng room
    @Query("""
        select count(c) from Clazz c
        where c.room.id = :roomId
          and c.status <> fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByRoom(Long roomId);

    // Đếm số lớp (chưa ARCHIVED) đang dạy bởi teacher (userId)
    @Query("""
        select count(c) from Clazz c
        join c.teacher t
        where t.user.id = :teacherUserId
          and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByTeacherUser(Long teacherUserId);

    // ✅ THÊM: Query theo teacher.id để so sánh
    @Query("""
        select count(c) from Clazz c
        where c.teacher.id = :teacherId
          and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByTeacherId(Long teacherId);

    // Liệt kê tên lớp đang active theo giáo viên và môn
    @Query("""
      select c from Clazz c
      where c.teacher.id = :teacherId
        and c.subject.id = :subjectId
        and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    java.util.List<Clazz> findActiveByTeacherAndSubject(Long teacherId, Long subjectId);
}
