package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fpt.capstone.edu360managementsystem.entity.Course;
import fpt.capstone.edu360managementsystem.enums.CourseStatus;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findBySubject_IdAndStatus(Long subjectId, CourseStatus status);

    List<Course> findByCreatedBy_Id(Long userId);

    List<Course> findByStatus(CourseStatus status);

    // Các khóa học cá nhân thuộc một giáo viên (teacher ownership)
    List<Course> findByOwnerTeacher_Id(Long teacherId);

    // Các khóa học cá nhân thuộc giáo viên theo cùng môn học và trạng thái
    List<Course> findByOwnerTeacher_IdAndSubject_IdAndStatus(Long teacherId, Long subjectId, CourseStatus status);
}
