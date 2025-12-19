package fpt.capstone.edu360managementsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Parent;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

    Optional<Parent> findByPhone(String phone);

    // Tìm parent theo phone trong User (backup cho dữ liệu cũ)
    @Query("SELECT p FROM Parent p WHERE p.user.phoneNumber = :phone")
    Optional<Parent> findByUserPhoneNumber(@Param("phone") String phone);

    // Tìm parent theo user_id
    @Query("SELECT p FROM Parent p WHERE p.user.id = :userId")
    Optional<Parent> findByUserId(@Param("userId") Long userId);
}
