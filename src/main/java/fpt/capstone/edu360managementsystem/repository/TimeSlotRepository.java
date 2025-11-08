package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> { }
