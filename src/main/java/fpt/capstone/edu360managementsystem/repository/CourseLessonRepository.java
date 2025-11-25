package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findByChapter_IdOrderByOrderIndexAsc(Long chapterId);
}

