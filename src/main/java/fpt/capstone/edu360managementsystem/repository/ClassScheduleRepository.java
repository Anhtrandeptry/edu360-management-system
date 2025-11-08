package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> { }
