package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.SessionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionMaterialRepository extends JpaRepository<SessionMaterial, Long> {
    

    List<SessionMaterial> findBySession_IdOrderByUploadedAtDesc(Long sessionId);
    

    List<SessionMaterial> findBySession_IdIn(List<Long> sessionIds);
    

    long countBySession_Id(Long sessionId);
    

    void deleteBySession_Id(Long sessionId);
}
