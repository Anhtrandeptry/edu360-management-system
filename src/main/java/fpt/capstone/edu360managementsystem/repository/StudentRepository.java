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

    /**
     * Đếm số học sinh đang hoạt động (user.active = true)
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.user.active = true")
    long countActive();

    long countByParent(Parent parent);

    /**
     * Tìm tất cả students có parent_id cụ thể
     */
    @Query("SELECT s FROM Student s WHERE s.parent.id = :parentId")
    java.util.List<Student> findByParentId(@Param("parentId") Long parentId);

    // Đếm tất cả student có parent với số điện thoại cụ thể (trong parents.phone hoặc users.phone_number)
    @Query("SELECT COUNT(s) FROM Student s WHERE s.parent.phone = :phone OR s.parent.user.phoneNumber = :phone")
    long countByParentPhone(@Param("phone") String phone);

}
