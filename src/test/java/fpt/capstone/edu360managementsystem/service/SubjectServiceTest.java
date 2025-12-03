package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.request.SubjectRequest;
import fpt.capstone.edu360managementsystem.dto.response.SubjectResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.enums.SubjectStatus;
import fpt.capstone.edu360managementsystem.mapper.SubjectMapper;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubjectServiceTest {
    @Mock private SubjectRepository subjectRepository;
    @Mock private SubjectMapper subjectMapper;
    @Mock private ClazzRepository clazzRepository;
    @InjectMocks private SubjectService subjectService;

    private Subject subject;
    private SubjectRequest subjectRequest;
    private SubjectResponse subjectResponse;

    @BeforeEach
    void setUp() {
        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");
        subject.setStatus(SubjectStatus.AVAILABLE);

        subjectRequest = new SubjectRequest();
        subjectRequest.setName("Math");

        subjectResponse = new SubjectResponse(1L, "Math", SubjectStatus.AVAILABLE, 0L);
    }

    // getAllSubjects - 5 cases
    @Test void test01_getAllSubjects_empty() {
        when(subjectRepository.findAll()).thenReturn(List.of());
        assertThat(subjectService.getAllSubjects()).isEmpty();
    }

    @Test void test02_getAllSubjects_hasSubjects() {
        when(subjectRepository.findAll()).thenReturn(List.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(5L);
        List<SubjectResponse> result = subjectService.getAllSubjects();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getClassCount()).isEqualTo(5L);
    }

    @Test void test03_getAllSubjects_classCount() {
        when(subjectRepository.findAll()).thenReturn(List.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(3L);
        subjectService.getAllSubjects();
        verify(clazzRepository).countActiveBySubject(1L);
    }

    @Test void test04_getAllSubjects_mapping() {
        when(subjectRepository.findAll()).thenReturn(List.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(anyLong())).thenReturn(0L);
        subjectService.getAllSubjects();
        verify(subjectMapper).toResponse(subject);
    }

    @Test void test05_getAllSubjects_multiple() {
        Subject subject2 = new Subject();
        subject2.setId(2L);
        SubjectResponse resp2 = new SubjectResponse(2L, "Physics", SubjectStatus.AVAILABLE, 0L);
        when(subjectRepository.findAll()).thenReturn(List.of(subject, subject2));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(subjectMapper.toResponse(subject2)).thenReturn(resp2);
        when(clazzRepository.countActiveBySubject(anyLong())).thenReturn(0L);
        assertThat(subjectService.getAllSubjects()).hasSize(2);
    }

    // getSubjectById - 5 cases
    @Test void test06_getSubjectById_notFound() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subjectService.getSubjectById(1L))
            .hasMessageContaining("Subject not found");
    }

    @Test void test07_getSubjectById_found() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(0L);
        SubjectResponse result = subjectService.getSubjectById(1L);
        assertThat(result).isNotNull();
    }

    @Test void test08_getSubjectById_classCount() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(7L);
        SubjectResponse result = subjectService.getSubjectById(1L);
        assertThat(result.getClassCount()).isEqualTo(7L);
    }

    @Test void test09_getSubjectById_mapping() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(anyLong())).thenReturn(0L);
        subjectService.getSubjectById(1L);
        verify(subjectMapper).toResponse(subject);
    }

    @Test void test10_getSubjectById_allFields() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        when(clazzRepository.countActiveBySubject(anyLong())).thenReturn(0L);
        SubjectResponse result = subjectService.getSubjectById(1L);
        assertThat(result.getName()).isEqualTo("Math");
    }

    // createSubject - 5 cases
    @Test void test11_createSubject_nameExists() {
        when(subjectRepository.existsByName("Math")).thenReturn(true);
        assertThatThrownBy(() -> subjectService.createSubject(subjectRequest))
            .hasMessageContaining("already exists");
    }

    @Test void test12_createSubject_valid() {
        when(subjectRepository.existsByName(anyString())).thenReturn(false);
        when(subjectMapper.toEntity(subjectRequest)).thenReturn(subject);
        when(subjectRepository.save(subject)).thenReturn(subject);
        when(subjectMapper.toResponse(subject)).thenReturn(subjectResponse);
        SubjectResponse result = subjectService.createSubject(subjectRequest);
        assertThat(result).isNotNull();
    }

    @Test void test13_createSubject_saved() {
        when(subjectRepository.existsByName(anyString())).thenReturn(false);
        when(subjectMapper.toEntity(subjectRequest)).thenReturn(subject);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        subjectService.createSubject(subjectRequest);
        verify(subjectRepository).save(any());
    }

    @Test void test14_createSubject_mapping() {
        when(subjectRepository.existsByName(anyString())).thenReturn(false);
        when(subjectMapper.toEntity(subjectRequest)).thenReturn(subject);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        subjectService.createSubject(subjectRequest);
        verify(subjectMapper).toEntity(subjectRequest);
    }

    @Test void test15_createSubject_response() {
        when(subjectRepository.existsByName(anyString())).thenReturn(false);
        when(subjectMapper.toEntity(subjectRequest)).thenReturn(subject);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        SubjectResponse result = subjectService.createSubject(subjectRequest);
        assertThat(result.getName()).isEqualTo("Math");
    }

    // updateSubject - 5 cases
    @Test void test16_updateSubject_notFound() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subjectService.updateSubject(1L, subjectRequest))
            .hasMessageContaining("Subject not found");
    }

    @Test void test17_updateSubject_nameConflict() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        doNothing().when(subjectMapper).updateEntityFromDto(subjectRequest, subject);
        when(subjectRepository.existsByNameAndIdNot("Math", 1L)).thenReturn(true);
        assertThatThrownBy(() -> subjectService.updateSubject(1L, subjectRequest))
            .hasMessageContaining("already exists");
    }

    @Test void test18_updateSubject_valid() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        doNothing().when(subjectMapper).updateEntityFromDto(subjectRequest, subject);
        when(subjectRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        SubjectResponse result = subjectService.updateSubject(1L, subjectRequest);
        assertThat(result).isNotNull();
    }

    @Test void test19_updateSubject_saved() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        doNothing().when(subjectMapper).updateEntityFromDto(any(), any());
        when(subjectRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        subjectService.updateSubject(1L, subjectRequest);
        verify(subjectRepository).save(any());
    }

    @Test void test20_updateSubject_response() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        doNothing().when(subjectMapper).updateEntityFromDto(any(), any());
        when(subjectRepository.existsByNameAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(subjectRepository.save(any())).thenReturn(subject);
        when(subjectMapper.toResponse(any())).thenReturn(subjectResponse);
        SubjectResponse result = subjectService.updateSubject(1L, subjectRequest);
        assertThat(result).isNotNull();
    }

    // disableSubject - 3 cases
    @Test void test21_disableSubject_notFound() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subjectService.disableSubject(1L))
            .hasMessageContaining("Subject not found");
    }

    @Test void test22_disableSubject_inUse() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(5L);
        assertThatThrownBy(() -> subjectService.disableSubject(1L))
            .hasMessageContaining("used by active classes");
    }

    @Test void test23_disableSubject_notInUse() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(clazzRepository.countActiveBySubject(1L)).thenReturn(0L);
        when(subjectRepository.save(any())).thenReturn(subject);
        subjectService.disableSubject(1L);
        verify(subjectRepository).save(argThat(s -> s.getStatus() == SubjectStatus.UNAVAILABLE));
    }

    // enableSubject - 2 cases
    @Test void test24_enableSubject_notFound() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> subjectService.enableSubject(1L))
            .hasMessageContaining("Subject not found");
    }

    @Test void test25_enableSubject_success() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(subjectRepository.save(any())).thenReturn(subject);
        subjectService.enableSubject(1L);
        verify(subjectRepository).save(argThat(s -> s.getStatus() == SubjectStatus.AVAILABLE));
    }
}
