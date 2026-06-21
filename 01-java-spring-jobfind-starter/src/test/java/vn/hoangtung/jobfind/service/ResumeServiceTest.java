package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.Role;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.response.resume.ResCreateResumeDTO;
import vn.hoangtung.jobfind.repository.JobRepository;
import vn.hoangtung.jobfind.repository.ResumeRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.SecurityUtil;

/**
 * Kiểm thử đơn vị cho ResumeService.create() - luồng ứng tuyển việc làm.
 *
 * Kỹ thuật: kiểm thử hộp trắng (bao phủ nhánh) - kiểm các quy tắc nghiệp vụ:
 * ứng tuyển thành công, chặn ứng tuyển trùng, chặn job đã đóng, chặn job hết hạn,
 * chặn job không hợp lệ. Dùng Mockito để giả lập Repository và SecurityUtil (static).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResumeServiceTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private CvVectorService cvVectorService;

    @InjectMocks
    private ResumeService resumeService;

    private MockedStatic<SecurityUtil> securityUtilMock;
    private User candidate;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName("USER");
        candidate = new User();
        candidate.setId(1L);
        candidate.setEmail("candidate@test.com");
        candidate.setRole(role);

        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserLogin)
                .thenReturn(Optional.of("candidate@test.com"));
        lenient().when(userRepository.findByEmail("candidate@test.com")).thenReturn(candidate);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private Job openJob() {
        Job job = new Job();
        job.setId(10L);
        job.setActive(true);
        job.setEndDate(null);
        return job;
    }

    private Resume resumeForJob(long jobId) {
        Job ref = new Job();
        ref.setId(jobId);
        Resume resume = new Resume();
        resume.setJob(ref);
        resume.setUrl("cv.pdf");
        return resume;
    }

    @Test
    void create_normalUserAppliesToOpenJob_savesResume() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(openJob()));
        when(resumeRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(false);
        Resume saved = new Resume();
        saved.setId(100L);
        when(resumeRepository.save(any(Resume.class))).thenReturn(saved);

        ResCreateResumeDTO result = resumeService.create(resumeForJob(10L));

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(resumeRepository).save(any(Resume.class));
    }

    @Test
    void create_duplicateApplication_throws() {
        when(jobRepository.findById(10L)).thenReturn(Optional.of(openJob()));
        when(resumeRepository.existsByUserIdAndJobId(1L, 10L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resumeService.create(resumeForJob(10L)));
        assertTrue(ex.getMessage().contains("đã ứng tuyển"));
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void create_closedJob_throws() {
        Job closed = openJob();
        closed.setActive(false);
        when(jobRepository.findById(10L)).thenReturn(Optional.of(closed));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resumeService.create(resumeForJob(10L)));
        assertTrue(ex.getMessage().contains("dong"));
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void create_expiredJob_throws() {
        Job expired = openJob();
        expired.setEndDate(Instant.now().minus(1, ChronoUnit.DAYS));
        when(jobRepository.findById(10L)).thenReturn(Optional.of(expired));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> resumeService.create(resumeForJob(10L)));
        assertTrue(ex.getMessage().contains("het han"));
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    void create_invalidJobId_throws() {
        Resume resume = new Resume();
        resume.setJob(null);

        assertThrows(IllegalArgumentException.class, () -> resumeService.create(resume));
        verify(resumeRepository, never()).save(any(Resume.class));
    }
}
