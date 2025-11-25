package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.SessionLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionLessonRepository extends JpaRepository<SessionLesson, Long> {
    List<SessionLesson> findBySession_Id(Long sessionId);
    void deleteBySession_Id(Long sessionId);
}

