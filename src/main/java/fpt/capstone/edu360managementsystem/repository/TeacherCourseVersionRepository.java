package fpt.capstone.edu360managementsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fpt.capstone.edu360managementsystem.entity.TeacherCourseVersion;

public interface TeacherCourseVersionRepository extends JpaRepository<TeacherCourseVersion, Long> {

    Optional<TeacherCourseVersion> findByBaseCourse_IdAndTeacherCourse_IdAndTeacher_Id(Long baseCourseId, Long teacherCourseId, Long teacherId);

    boolean existsByBaseCourse_IdAndTeacherCourse_IdAndTeacher_Id(Long baseCourseId, Long teacherCourseId, Long teacherId);

    List<TeacherCourseVersion> findByBaseCourse_IdAndTeacher_Id(Long baseCourseId, Long teacherId);

    // Find TeacherCourseVersion by base course and teacher (for student enrolled class)
    Optional<TeacherCourseVersion> findFirstByBaseCourse_IdAndTeacher_Id(Long baseCourseId, Long teacherId);
}
