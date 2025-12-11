package fpt.capstone.edu360managementsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    // Case-insensitive check for duplicate room name
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE LOWER(r.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    // Case-insensitive check for duplicate room name excluding current room (for update)
    @Query("SELECT COUNT(r) > 0 FROM Room r WHERE LOWER(r.name) = LOWER(:name) AND r.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    List<Room> findByStatus(RoomStatus status);

    @Query("""
        SELECT r FROM Room r
        WHERE (:search IS NULL OR :search = '' OR 
               LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:status IS NULL OR r.status = :status)
        """)
    Page<Room> findBySearchAndStatus(
            @Param("search") String search,
            @Param("status") RoomStatus status,
            Pageable pageable
    );

}
