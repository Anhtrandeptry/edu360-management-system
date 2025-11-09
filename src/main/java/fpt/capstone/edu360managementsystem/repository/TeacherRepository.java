package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Teacher;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByUserId(Long userId);

    List<Teacher> findBySubjectId(Long subjectId);

    java.util.Optional<Teacher> findByUserId(Long userId);

}
