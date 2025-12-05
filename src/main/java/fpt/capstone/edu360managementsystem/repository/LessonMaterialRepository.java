package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, Long> {
    
    List<LessonMaterial> findByLesson_IdOrderByUploadedAtDesc(Long lessonId);
    
    List<LessonMaterial> findByLesson_Chapter_IdOrderByUploadedAtDesc(Long chapterId);
    
    void deleteByLesson_Id(Long lessonId);
}
