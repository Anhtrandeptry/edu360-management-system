package fpt.capstone.edu360managementsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Parent;
import fpt.capstone.edu360managementsystem.entity.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser_Id(Long userId);

    
    long countByParent(Parent parent);
    
    // Đếm tất cả student có parent với số điện thoại cụ thể (trong parents.phone hoặc users.phone_number)
    @Query("SELECT COUNT(s) FROM Student s WHERE s.parent.phone = :phone OR s.parent.user.phoneNumber = :phone")
    long countByParentPhone(@Param("phone") String phone);

}
