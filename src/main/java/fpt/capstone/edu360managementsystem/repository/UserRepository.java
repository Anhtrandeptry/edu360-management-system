package fpt.capstone.edu360managementsystem.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    Boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Phân trang và tìm kiếm users với filter theo role
     *
     * @param search tìm theo username, fullName, email, phone
     * @param roleName filter theo role (ADMIN, TEACHER, STUDENT, PARENT) - null
     * để lấy tất cả
     * @param pageable thông tin phân trang
     */
    @Query("""
      SELECT DISTINCT u FROM User u 
      LEFT JOIN u.roles r
      WHERE (:search IS NULL OR :search = '' OR 
             LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
             u.phoneNumber LIKE CONCAT('%', :search, '%'))
      AND (:roleName IS NULL OR r.name = :roleName)
      """)
    Page<User> findBySearchAndRole(
            @Param("search") String search,
            @Param("roleName") fpt.capstone.edu360managementsystem.enums.ERole roleName,
            Pageable pageable
    );

}
