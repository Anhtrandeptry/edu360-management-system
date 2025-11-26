package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.TeacherEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherEducationRepository extends JpaRepository<TeacherEducation, Long> {
    List<TeacherEducation> findByTeacherId(Long teacherId);
    void deleteByTeacherId(Long teacherId);
}
