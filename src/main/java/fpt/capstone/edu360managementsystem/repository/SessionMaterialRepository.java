package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.SessionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionMaterialRepository extends JpaRepository<SessionMaterial, Long> {
    
    /**
     * Lấy tất cả tài liệu của một session
     */
    List<SessionMaterial> findBySession_IdOrderByUploadedAtDesc(Long sessionId);
    
    /**
     * Lấy tài liệu của nhiều sessions (batch query)
     */
    List<SessionMaterial> findBySession_IdIn(List<Long> sessionIds);
    
    /**
     * Đếm số tài liệu của session
     */
    long countBySession_Id(Long sessionId);
    
    /**
     * Xóa tất cả tài liệu của session
     */
    void deleteBySession_Id(Long sessionId);
}
