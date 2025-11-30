package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    // Tìm kiếm theo title hoặc content
    @Query("SELECT n FROM News n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<News> searchNews(@Param("search") String search, Pageable pageable);

    // Lấy tin tức theo status
    Page<News> findByStatus(String status, Pageable pageable);

    // Lấy tin tức PUBLISHED (cho guest)
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.publishedAt DESC")
    Page<News> findPublishedNews(Pageable pageable);

    // Tìm kiếm tin tức PUBLISHED
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> searchPublishedNews(@Param("search") String search, Pageable pageable);
}
