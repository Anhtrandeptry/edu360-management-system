package fpt.capstone.edu360managementsystem.service;

import fpt.capstone.edu360managementsystem.dto.response.SemesterResponse;
import fpt.capstone.edu360managementsystem.entity.Semester;
import fpt.capstone.edu360managementsystem.enums.SemesterStatus;
import fpt.capstone.edu360managementsystem.repository.SemesterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SemesterServiceTest {
    @Mock private SemesterRepository semesterRepository;
    @InjectMocks private SemesterService semesterService;

    private Semester semester;

    @BeforeEach
    void setUp() {
        semester = new Semester();
        semester.setId(1L);
        semester.setName("Fall 2024");
        semester.setStartDate(LocalDate.of(2024, 9, 1));
        semester.setEndDate(LocalDate.of(2024, 12, 31));
        semester.setStatus(SemesterStatus.OPEN);
    }

    // getAll - 8 cases
    @Test void test01_getAll_noFilter() {
        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        List<SemesterResponse> result = semesterService.getAll(null);
        assertThat(result).hasSize(1);
    }

    @Test void test02_getAll_filterByStatus() {
        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        List<SemesterResponse> result = semesterService.getAll("OPEN");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("OPEN");
    }

    @Test void test03_getAll_emptyDatabase() {
        when(semesterRepository.findAll()).thenReturn(List.of());
        List<SemesterResponse> result = semesterService.getAll(null);
        assertThat(result).isEmpty();
    }

    @Test void test04_getAll_multipleSemesters() {
        Semester semester2 = new Semester();
        semester2.setId(2L);
        semester2.setName("Spring 2025");
        semester2.setStatus(SemesterStatus.CLOSED);
        when(semesterRepository.findAll()).thenReturn(List.of(semester, semester2));
        List<SemesterResponse> result = semesterService.getAll(null);
        assertThat(result).hasSize(2);
    }

    @Test void test05_getAll_statusFilter_exactMatch() {
        Semester semester2 = new Semester();
        semester2.setId(2L);
        semester2.setStatus(SemesterStatus.CLOSED);
        when(semesterRepository.findAll()).thenReturn(List.of(semester, semester2));
        List<SemesterResponse> result = semesterService.getAll("OPEN");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test void test06_getAll_invalidStatus_error() {
        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        assertThatThrownBy(() -> semesterService.getAll("INVALID"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void test07_getAll_mapping_allFields() {
        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        List<SemesterResponse> result = semesterService.getAll(null);
        SemesterResponse resp = result.get(0);
        assertThat(resp.getName()).isEqualTo("Fall 2024");
        assertThat(resp.getStartDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(resp.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test void test08_getAll_statusMapping() {
        when(semesterRepository.findAll()).thenReturn(List.of(semester));
        List<SemesterResponse> result = semesterService.getAll(null);
        assertThat(result.get(0).getStatus()).isEqualTo("OPEN");
    }

    // getById - 7 cases
    @Test void test09_getById_notFound() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> semesterService.getById(1L))
            .hasMessageContaining("Semester not found");
    }

    @Test void test10_getById_found() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result).isNotNull();
    }

    @Test void test11_getById_mapping_allFields() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Fall 2024");
    }

    @Test void test12_getById_status_correct() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
    }

    @Test void test13_getById_dates_correct() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2024, 12, 31));
    }

    @Test void test14_getById_name_correct() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result.getName()).isEqualTo("Fall 2024");
    }

    @Test void test15_getById_returnResponse_correct() {
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        SemesterResponse result = semesterService.getById(1L);
        assertThat(result).isInstanceOf(SemesterResponse.class);
    }
}
