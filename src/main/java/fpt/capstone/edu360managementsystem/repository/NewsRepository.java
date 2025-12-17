package fpt.capstone.edu360managementsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.News;
import fpt.capstone.edu360managementsystem.enums.NewsStatus;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

<<<<<<< HEAD
    @Query("SELECT n FROM News n WHERE "
            + "LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))")
=======
    /**
     * Counts news by status.
     */
    long countByStatus(String status);

    @Query("SELECT n FROM News n WHERE " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))")
>>>>>>> origin/hung-fixbug
    Page<News> searchNews(@Param("search") String search, Pageable pageable);

    Page<News> findByStatus(NewsStatus status, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = fpt.capstone.edu360managementsystem.enums.NewsStatus.PUBLISHED ORDER BY n.publishedAt DESC")
    Page<News> findPublishedNews(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = fpt.capstone.edu360managementsystem.enums.NewsStatus.PUBLISHED AND "
            + "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) "
            + "ORDER BY n.publishedAt DESC")
    Page<News> searchPublishedNews(@Param("search") String search, Pageable pageable);
}
