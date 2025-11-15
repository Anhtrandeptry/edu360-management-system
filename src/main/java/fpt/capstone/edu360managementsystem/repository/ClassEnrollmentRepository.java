package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, Long> {
    List<ClassEnrollment> findByClazz_Id(Long classId);
}
