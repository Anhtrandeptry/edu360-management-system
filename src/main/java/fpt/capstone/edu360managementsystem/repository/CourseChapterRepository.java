package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.CourseChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, Long> {
    List<CourseChapter> findByCourse_IdOrderByOrderIndexAsc(Long courseId);
}

