package fpt.capstone.edu360managementsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser_Id(Long userId);

    
    long countByParent(Parent parent);

}
