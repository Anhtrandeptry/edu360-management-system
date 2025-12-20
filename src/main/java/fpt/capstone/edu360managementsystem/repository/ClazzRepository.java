package fpt.capstone.edu360managementsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Clazz;
import fpt.capstone.edu360managementsystem.enums.ClassStatus;
import jakarta.persistence.LockModeType;

@Repository
public interface ClazzRepository extends JpaRepository<Clazz, Long>, JpaSpecificationExecutor<Clazz> {

    boolean existsByNameAndSubject_IdAndSemester_Id(String name, Long subjectId, Long semesterId);


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


    @Query("""
    select distinct c from Clazz c
    left join fetch c.teacher t
    left join fetch c.subject sj
    left join fetch c.room r
    left join fetch c.semester sem
    left join fetch c.teacher.user tu
  """)
    List<Clazz> findAllWithSchedules();


    @Query("""
        select count(c) from Clazz c
        where c.subject.id = :subjectId
          and c.status <> fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveBySubject(Long subjectId);


    @Query("""
        select count(c) from Clazz c
        where c.room.id = :roomId
          and c.status <> fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByRoom(Long roomId);


    @Query("""
        select count(c) from Clazz c
        join c.teacher t
        where t.user.id = :teacherUserId
          and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByTeacherUser(Long teacherUserId);


    @Query("""
        select count(c) from Clazz c
        where c.teacher.id = :teacherId
          and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    long countActiveByTeacherId(Long teacherId);


    @Query("""
      select c from Clazz c
      where c.teacher.id = :teacherId
        and c.subject.id = :subjectId
        and c.status != fpt.capstone.edu360managementsystem.enums.ClassStatus.ARCHIVED
    """)
    java.util.List<Clazz> findActiveByTeacherAndSubject(Long teacherId, Long subjectId);


    List<Clazz> findByCourse_Id(Long courseId);


    List<Clazz> findByTeacher_Id(Long teacherId);


    @Query("""
        SELECT DISTINCT c FROM Clazz c
        LEFT JOIN c.teacher t
        LEFT JOIN t.user tu
        LEFT JOIN c.subject s
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(tu.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR c.status = :status)
        AND (:isOnline IS NULL OR 
             (:isOnline = true AND c.meetingLink IS NOT NULL AND c.meetingLink <> '') OR
             (:isOnline = false AND (c.meetingLink IS NULL OR c.meetingLink = '')))
        AND (:teacherUserId IS NULL OR tu.id = :teacherUserId)
        AND (:subjectId IS NULL OR s.id = :subjectId)
        AND (:minPrice IS NULL OR c.pricePerSession IS NULL OR c.pricePerSession >= :minPrice)
        AND (:maxPrice IS NULL OR c.pricePerSession IS NULL OR c.pricePerSession <= :maxPrice)
        """)
    Page<Clazz> findBySearchAndFilters(
            @Param("search") String search,
            @Param("status") ClassStatus status,
            @Param("isOnline") Boolean isOnline,
            @Param("teacherUserId") Long teacherUserId,
            @Param("subjectId") Long subjectId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    /**
     * Lấy danh sách lớp DRAFT có startDate trong khoảng từ ngày A đến ngày B
     * Dùng để nhắc nhở admin về các lớp DRAFT sắp đến ngày bắt đầu
     */
    @Query("""
      SELECT c FROM Clazz c
      WHERE c.status = fpt.capstone.edu360managementsystem.enums.ClassStatus.DRAFT
      AND c.startDate BETWEEN :fromDate AND :toDate
      """)
    java.util.List<Clazz> findDraftClassesWithStartDateBetween(
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate
    );

    // ==================== REPORT QUERIES ====================
    // Đếm lớp theo status
    Long countByStatus(ClassStatus status);

    // Đếm lớp theo giáo viên và status
    @Query("SELECT COUNT(c) FROM Clazz c WHERE c.teacher.id = :teacherId AND c.status = :status")
    Integer countByTeacherIdAndStatus(@Param("teacherId") Long teacherId, @Param("status") ClassStatus status);

    // Đếm lớp theo môn học và status
    @Query("SELECT COUNT(c) FROM Clazz c WHERE c.subject.id = :subjectId AND c.status = :status")
    Integer countBySubjectIdAndStatus(@Param("subjectId") Long subjectId, @Param("status") ClassStatus status);

    // Đếm số giáo viên có lớp PUBLIC
    @Query("SELECT COUNT(DISTINCT c.teacher.id) FROM Clazz c WHERE c.status = 'PUBLIC'")
    Long countDistinctTeachersWithPublicClasses();

    // ==================== PESSIMISTIC LOCK FOR ENROLLMENT ====================
    /**
     * Lấy Clazz với Pessimistic Write Lock để tránh race condition khi enroll.
     * Khi một transaction đang giữ lock, các transaction khác sẽ phải chờ.
     * Điều này đảm bảo capacity check và enrollment insert là atomic.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clazz c WHERE c.id = :id")
    Optional<Clazz> findByIdWithPessimisticLock(@Param("id") Long id);
}
