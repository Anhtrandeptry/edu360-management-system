package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fpt.capstone.edu360managementsystem.entity.SessionChapter;

public interface SessionChapterRepository extends JpaRepository<SessionChapter, Long> {

    List<SessionChapter> findBySession_Id(Long sessionId);

    @Modifying
    @Query("DELETE FROM SessionChapter sc WHERE sc.session.id = :sessionId")
    void deleteBySession_Id(@Param("sessionId") Long sessionId);

    @Modifying
    @Query("DELETE FROM SessionChapter sc WHERE sc.chapter.id = :chapterId")
    void deleteByChapter_Id(@Param("chapterId") Long chapterId);
}
