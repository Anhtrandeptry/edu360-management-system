package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fpt.capstone.edu360managementsystem.entity.SessionLesson;

public interface SessionLessonRepository extends JpaRepository<SessionLesson, Long> {

    List<SessionLesson> findBySession_Id(Long sessionId);

    @Modifying
    @Query("DELETE FROM SessionLesson sl WHERE sl.session.id = :sessionId")
    void deleteBySession_Id(@Param("sessionId") Long sessionId);

    @Modifying
    @Query("DELETE FROM SessionLesson sl WHERE sl.lesson.id = :lessonId")
    void deleteByLesson_Id(@Param("lessonId") Long lessonId);
}
