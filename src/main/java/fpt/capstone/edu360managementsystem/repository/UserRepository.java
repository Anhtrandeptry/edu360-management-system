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

    /**
     * Lấy danh sách users theo role
     */
    @Query("""
      SELECT u FROM User u 
      JOIN u.roles r
      WHERE r.name = :roleName
      """)
    java.util.List<User> findAllByRole(@Param("roleName") fpt.capstone.edu360managementsystem.enums.ERole roleName);

    /**
     * Tìm tất cả Users có role PARENT theo số điện thoại
     */
    @Query("""
      SELECT u FROM User u 
      JOIN u.roles r
      WHERE r.name = :roleName
        AND u.phoneNumber = :phone
      """)
    java.util.List<User> findParentsByPhone(@Param("phone") String phone, @Param("roleName") fpt.capstone.edu360managementsystem.enums.ERole roleName);

    /**
     * Kiểm tra trùng email trong nhóm giáo viên (role TEACHER), loại trừ một userId nếu cung cấp.
     */
    @Query("""
      SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
      FROM User u JOIN u.roles r
      WHERE r.name = :roleName
        AND LOWER(u.email) = LOWER(:email)
        AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)
      """)
    boolean existsTeacherEmail(@Param("email") String email, @Param("excludeUserId") Long excludeUserId, @Param("roleName") fpt.capstone.edu360managementsystem.enums.ERole roleName);

    /**
     * Kiểm tra trùng số điện thoại trong nhóm giáo viên (role TEACHER), loại trừ một userId nếu cung cấp.
     */
    @Query("""
      SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
      FROM User u JOIN u.roles r
      WHERE r.name = :roleName
        AND u.phoneNumber = :phone
        AND (:excludeUserId IS NULL OR u.id <> :excludeUserId)
      """)
    boolean existsTeacherPhone(@Param("phone") String phone, @Param("excludeUserId") Long excludeUserId, @Param("roleName") fpt.capstone.edu360managementsystem.enums.ERole roleName);

}
