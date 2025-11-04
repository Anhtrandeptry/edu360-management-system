package fpt.capstone.edu360managementsystem.repository;



import fpt.capstone.edu360managementsystem.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
}

