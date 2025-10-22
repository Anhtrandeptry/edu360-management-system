package fpt.capstone.edu360managementsystem.repository;


import fpt.capstone.edu360managementsystem.entity.Role;
import fpt.capstone.edu360managementsystem.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
