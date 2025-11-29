package fpt.capstone.edu360managementsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fpt.capstone.edu360managementsystem.entity.SessionContentConfig;

public interface SessionContentConfigRepository extends JpaRepository<SessionContentConfig, Long> {

    Optional<SessionContentConfig> findBySession_Id(Long sessionId);

    boolean existsBySession_Id(Long sessionId);
}
