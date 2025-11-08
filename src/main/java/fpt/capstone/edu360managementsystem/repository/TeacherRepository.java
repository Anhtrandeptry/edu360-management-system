package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    boolean existsByUserId(Long userId);
    List<Teacher> findBySubjectId(Long subjectId);

}
