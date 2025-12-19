package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.TeacherExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherExperienceRepository extends JpaRepository<TeacherExperience, Long> {
    List<TeacherExperience> findByTeacherId(Long teacherId);
    void deleteByTeacherId(Long teacherId);
}
