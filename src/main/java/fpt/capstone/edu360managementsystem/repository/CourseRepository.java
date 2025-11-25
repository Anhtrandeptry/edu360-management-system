package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findBySubject_IdAndStatus(Long subjectId, CourseStatus status);

    List<Course> findByCreatedBy_Id(Long userId);

    List<Course> findByStatus(CourseStatus status);
}
