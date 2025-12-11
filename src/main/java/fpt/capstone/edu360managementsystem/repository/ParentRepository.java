package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByPhone(String phone);
    
    // Tìm parent theo số điện thoại trong bảng users
    @Query("SELECT p FROM Parent p WHERE p.user.phoneNumber = :phone")
    Optional<Parent> findByUserPhoneNumber(@Param("phone") String phone);
    
    // Tìm parent theo phone trong parents hoặc phone_number trong users
    @Query("SELECT p FROM Parent p WHERE p.phone = :phone OR p.user.phoneNumber = :phone")
    Optional<Parent> findByPhoneOrUserPhoneNumber(@Param("phone") String phone);

    /**
     * Tìm parent theo user id
     */
    Optional<Parent> findByUser_Id(Long userId);
}
