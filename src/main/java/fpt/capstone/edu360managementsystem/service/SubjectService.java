package fpt.capstone.edu360managementsystem.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fpt.capstone.edu360managementsystem.dto.request.SubjectRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.mapper.SubjectMapper;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SubjectMapper subjectMapper;

    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(s -> {
                    long cnt = clazzRepository.countActiveBySubject(s.getId());
                    SubjectResponse resp = subjectMapper.toResponse(s);
                    resp.setClassCount(cnt);
                    return resp;
                })
                .toList();
    }

    public SubjectResponse getSubjectById(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        SubjectResponse resp = subjectMapper.toResponse(subject);
        resp.setClassCount(clazzRepository.countActiveBySubject(subject.getId()));
        return resp;
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
        if (subjectRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Subject name already exists!");
        }
        return subjectMapper.toResponse(subjectRepository.save(subject));
    }

    public void disableSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        long used = clazzRepository.countActiveBySubject(subject.getId());
        if (used > 0) {
            throw new RuntimeException("Subject is used by active classes, cannot disable");
        }
        subject.setStatus(SubjectStatus.UNAVAILABLE);
        subjectRepository.save(subject);
    }

    public void enableSubject(Long id) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        subject.setStatus(SubjectStatus.AVAILABLE);
        subjectRepository.save(subject);
    }

    public List<Subject> getAvailableSubjects() {
        return subjectRepository.findByStatus(SubjectStatus.AVAILABLE);
    }

    // New: list AVAILABLE subjects as SubjectResponse with classCount
    public List<SubjectResponse> getAvailableSubjectResponses() {
        return subjectRepository.findByStatus(SubjectStatus.AVAILABLE).stream()
                .map(s -> {
                    long cnt = clazzRepository.countActiveBySubject(s.getId());
                    SubjectResponse resp = subjectMapper.toResponse(s);
                    resp.setClassCount(cnt);
                    return resp;
                })
                .toList();
    }

    @Autowired
    private fpt.capstone.edu360managementsystem.repository.ClazzRepository clazzRepository;
}
