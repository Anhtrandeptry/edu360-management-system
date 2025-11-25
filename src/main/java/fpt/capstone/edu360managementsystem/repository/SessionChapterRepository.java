package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.SessionChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionChapterRepository extends JpaRepository<SessionChapter, Long> {
    List<SessionChapter> findBySession_Id(Long sessionId);
    void deleteBySession_Id(Long sessionId);
}

