package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByUserId(Long userId);

    // Lọc theo subject: chấp nhận cả subject chính và các môn trong danh sách many-to-many
    @Query("select distinct t from Teacher t left join t.subjects s where t.subject.id = :subjectId or s.id = :subjectId")
    List<Teacher> findByAnySubject(@Param("subjectId") Long subjectId);

    java.util.Optional<Teacher> findByUserId(Long userId);

}
