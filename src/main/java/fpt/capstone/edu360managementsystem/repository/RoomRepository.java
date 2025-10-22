package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.Room;
import fpt.capstone.edu360managementsystem.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    boolean existsByName(String name);
    List<Room> findByStatus(RoomStatus status);
}
