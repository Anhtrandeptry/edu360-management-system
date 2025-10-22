package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsByName(String name);
}
