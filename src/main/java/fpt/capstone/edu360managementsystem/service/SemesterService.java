package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.response.SemesterResponse;
import fpt.capstone.edu360managementsystem.entity.Semester;
import fpt.capstone.edu360managementsystem.enums.SemesterStatus;
import fpt.capstone.edu360managementsystem.repository.SemesterRepository;

@Service
public class SemesterService {

    @Autowired
    private SemesterRepository semesterRepository;

    public List<SemesterResponse> getAll(String status) {
        List<Semester> semesters = semesterRepository.findAll();

        if (status != null) {
            SemesterStatus statusEnum = SemesterStatus.valueOf(status.toUpperCase());
            semesters = semesters.stream()
                    .filter(s -> s.getStatus() == statusEnum)
                    .collect(Collectors.toList());
        }

        return semesters.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public SemesterResponse getById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Semester not found"));
        return mapToResponse(semester);
    }

    private SemesterResponse mapToResponse(Semester s) {
        return SemesterResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .build();
    }
}
