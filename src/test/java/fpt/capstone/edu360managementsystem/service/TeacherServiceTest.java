package fpt.capstone.edu360managementsystem.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import fpt.capstone.edu360managementsystem.dto.response.TeacherResponse;
import fpt.capstone.edu360managementsystem.entity.Subject;
import fpt.capstone.edu360managementsystem.entity.Teacher;
import fpt.capstone.edu360managementsystem.entity.User;
import fpt.capstone.edu360managementsystem.repository.ClazzRepository;
import fpt.capstone.edu360managementsystem.repository.SubjectRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherCertificateRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherEducationRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherExperienceRepository;
import fpt.capstone.edu360managementsystem.repository.TeacherRepository;
import fpt.capstone.edu360managementsystem.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherServiceTest {
    @Mock private TeacherRepository teacherRepository;
    @Mock private ClazzRepository clazzRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeacherCertificateRepository certificateRepository;
    @Mock private TeacherExperienceRepository experienceRepository;
    @Mock private TeacherEducationRepository educationRepository;
    @Mock private SubjectRepository subjectRepository;
    @InjectMocks private TeacherService teacherService;

    private Teacher teacher;
    private User user;
    private Subject subject;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("teacher1");
        user.setFullName("Teacher One");
        user.setEmail("teacher@test.com");
        user.setPhoneNumber("0123456789");
        user.setActive(true);

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");

        teacher = new Teacher();
        teacher.setId(1L);
        teacher.setUser(user);
        teacher.setSubject(subject);
        teacher.setSpecialization("Mathematics");
        teacher.setDegree("Master");
    }

    // getTeachers - 10 cases
    @Test void test01_getTeachers_noFilter() {
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(5L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result).hasSize(1);
    }

    @Test void test02_getTeachers_filterBySubject() {
        when(teacherRepository.findActiveByAnySubject(1L)).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(3L);
        List<TeacherResponse> result = teacherService.getTeachers(1L);
        assertThat(result).hasSize(1);
        verify(teacherRepository).findActiveByAnySubject(1L);
    }

    @Test void test03_getTeachers_noTeachers() {
        when(teacherRepository.findAllActive()).thenReturn(List.of());
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result).isEmpty();
    }

    @Test void test04_getTeachers_multipleTeachers() {
        Teacher teacher2 = new Teacher();
        teacher2.setId(2L);
        User user2 = new User();
        user2.setId(2L);
        user2.setActive(true);
        teacher2.setUser(user2);
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher, teacher2));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result).hasSize(2);
    }

    @Test void test05_getTeachers_subjectFilter_exactMatch() {
        when(teacherRepository.findActiveByAnySubject(1L)).thenReturn(List.of(teacher));
        when(teacherRepository.findActiveByAnySubject(2L)).thenReturn(List.of());
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        List<TeacherResponse> result = teacherService.getTeachers(1L);
        assertThat(result).hasSize(1);
    }

    @Test void test06_getTeachers_mapping_allFields() {
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(1L)).thenReturn(7L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        TeacherResponse resp = result.get(0);
        assertThat(resp.getFullName()).isEqualTo("Teacher One");
        assertThat(resp.getClassCount()).isEqualTo(7L);
    }

    @Test void test07_getTeachers_classCount_calculated() {
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(1L)).thenReturn(10L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result.get(0).getClassCount()).isEqualTo(10L);
    }

    @Test void test08_getTeachers_primarySubject_mapped() {
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result.get(0).getSubjectName()).isEqualTo("Math");
    }

    @Test void test09_getTeachers_multipleSubjects_allMapped() {
        Subject subject2 = new Subject();
        subject2.setId(2L);
        subject2.setName("Physics");
        teacher.getSubjects().add(subject);
        teacher.getSubjects().add(subject2);
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result.get(0).getSubjectIds()).hasSize(2);
    }

    @Test void test10_getTeachers_activeStatus_included() {
        when(teacherRepository.findAllActive()).thenReturn(List.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        List<TeacherResponse> result = teacherService.getTeachers(null);
        assertThat(result.get(0).getActive()).isTrue();
    }

    // getByUserId - 10 cases
    @Test void test11_getByUserId_notFound() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> teacherService.getByUserId(1L))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Teacher not found");
    }

    @Test void test12_getByUserId_found() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result).isNotNull();
    }

    @Test void test13_getByUserId_mapping_allFields() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(1L)).thenReturn(5L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getFullName()).isEqualTo("Teacher One");
        assertThat(result.getEmail()).isEqualTo("teacher@test.com");
    }

    @Test void test14_getByUserId_classCount_correct() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(1L)).thenReturn(8L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getClassCount()).isEqualTo(8L);
    }

    @Test void test15_getByUserId_primarySubject_correct() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getSubjectId()).isEqualTo(1L);
        assertThat(result.getSubjectName()).isEqualTo("Math");
    }

    @Test void test16_getByUserId_multipleSubjects_allIncluded() {
        Subject subject2 = new Subject();
        subject2.setId(2L);
        subject2.setName("Physics");
        teacher.getSubjects().add(subject);
        teacher.getSubjects().add(subject2);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getSubjectIds()).contains(1L, 2L);
        assertThat(result.getSubjectNames()).contains("Math", "Physics");
    }

    @Test void test17_getByUserId_subjectNames_allIncluded() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getSubjectName()).isNotNull();
    }

    @Test void test18_getByUserId_entityNotFoundException_handled() {
        when(teacherRepository.findByUserId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> teacherService.getByUserId(999L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test void test19_getByUserId_nullSubject_handled() {
        teacher.setSubject(null);
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result).isNotNull();
    }

    @Test void test20_getByUserId_returnResponse_correct() {
        when(teacherRepository.findByUserId(1L)).thenReturn(Optional.of(teacher));
        when(clazzRepository.countActiveByTeacherUser(anyLong())).thenReturn(0L);
        TeacherResponse result = teacherService.getByUserId(1L);
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
    }
}
