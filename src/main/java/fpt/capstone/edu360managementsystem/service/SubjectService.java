package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.SubjectRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.mapper.SubjectMapper;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectMapper subjectMapper;

    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(subjectMapper::toResponse)
                .toList();
    }

    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        return subjectMapper.toResponse(subject);
    }

    public SubjectResponse createSubject(SubjectRequest request) {
        if (subjectRepository.existsByName(request.getName())) {
            throw new RuntimeException("Subject name already exists!");
        }
        Subject subject = subjectMapper.toEntity(request);
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        subjectMapper.updateEntityFromDto(request, subject);
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    public void disableSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        subject.setStatus(SubjectStatus.UNAVAILABLE);
        subjectRepository.save(subject);
    }

    public void enableSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        subject.setStatus(SubjectStatus.AVAILABLE);
        subjectRepository.save(subject);
    }
}
