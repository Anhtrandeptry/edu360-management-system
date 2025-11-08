package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> { }
