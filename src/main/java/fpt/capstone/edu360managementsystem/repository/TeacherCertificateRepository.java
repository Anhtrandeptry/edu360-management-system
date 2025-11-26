package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.TeacherCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherCertificateRepository extends JpaRepository<TeacherCertificate, Long> {
    List<TeacherCertificate> findByTeacherId(Long teacherId);
    void deleteByTeacherId(Long teacherId);
}
