package fpt.capstone.edu360managementsystem.repository;

import fpt.capstone.edu360managementsystem.entity.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
    List<ClassSchedule> findByClazz_Id(Long classId);

}
