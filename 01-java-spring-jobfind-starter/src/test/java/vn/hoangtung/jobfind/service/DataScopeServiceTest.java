package vn.hoangtung.jobfind.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.Job;
import vn.hoangtung.jobfind.domain.Resume;
import vn.hoangtung.jobfind.domain.Role;
import vn.hoangtung.jobfind.domain.User;

/**
 * Kiểm thử đơn vị cho DataScopeService - cơ chế phân quyền dữ liệu theo công ty
 * (SUPER_ADMIN thấy tất cả; HR chỉ thấy dữ liệu công ty mình).
 *
 * Kỹ thuật: kiểm thử hộp đen + phân vùng tương đương (vai trò ADMIN/HR/USER)
 * + phân tích giá trị biên (role null, company null, công ty khác nhau).
 */
class DataScopeServiceTest {

    // Các phương thức kiểm thử không dùng tới userRepository nên truyền null là đủ.
    private final DataScopeService dataScopeService = new DataScopeService(null);

    private User userWithRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setRole(role);
        return user;
    }

    private Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    // ---------- isAdmin ----------
    @Test
    void isAdmin_superAdminRole_returnsTrue() {
        assertTrue(dataScopeService.isAdmin(userWithRole("SUPER_ADMIN")));
    }

    @Test
    void isAdmin_hrRole_returnsFalse() {
        assertFalse(dataScopeService.isAdmin(userWithRole("HR")));
    }

    @Test
    void isAdmin_nullRole_returnsFalse() {
        assertFalse(dataScopeService.isAdmin(new User()));
    }

    // ---------- isHr ----------
    @Test
    void isHr_hrRole_returnsTrue() {
        assertTrue(dataScopeService.isHr(userWithRole("HR")));
    }

    @Test
    void isHr_recruiterRole_returnsTrue() {
        assertTrue(dataScopeService.isHr(userWithRole("RECRUITER")));
    }

    @Test
    void isHr_normalUser_returnsFalse() {
        assertFalse(dataScopeService.isHr(userWithRole("USER")));
    }

    // ---------- isHrOnly ----------
    @Test
    void isHrOnly_hr_returnsTrue() {
        assertTrue(dataScopeService.isHrOnly(userWithRole("HR")));
    }

    @Test
    void isHrOnly_superAdmin_returnsFalse() {
        assertFalse(dataScopeService.isHrOnly(userWithRole("SUPER_ADMIN")));
    }

    // ---------- isSameCompany ----------
    @Test
    void isSameCompany_sameId_returnsTrue() {
        assertTrue(dataScopeService.isSameCompany(company(1), company(1)));
    }

    @Test
    void isSameCompany_differentId_returnsFalse() {
        assertFalse(dataScopeService.isSameCompany(company(1), company(2)));
    }

    @Test
    void isSameCompany_nullArgument_returnsFalse() {
        assertFalse(dataScopeService.isSameCompany(null, company(1)));
    }

    // ---------- isJobInCurrentCompany ----------
    @Test
    void isJobInCurrentCompany_sameCompany_returnsTrue() {
        User hr = userWithRole("HR");
        hr.setCompany(company(1));
        Job job = new Job();
        job.setCompany(company(1));
        assertTrue(dataScopeService.isJobInCurrentCompany(job, hr));
    }

    @Test
    void isJobInCurrentCompany_differentCompany_returnsFalse() {
        User hr = userWithRole("HR");
        hr.setCompany(company(1));
        Job job = new Job();
        job.setCompany(company(2));
        assertFalse(dataScopeService.isJobInCurrentCompany(job, hr));
    }

    // ---------- isResumeInCurrentCompany ----------
    @Test
    void isResumeInCurrentCompany_sameCompany_returnsTrue() {
        User hr = userWithRole("HR");
        hr.setCompany(company(1));
        Job job = new Job();
        job.setCompany(company(1));
        Resume resume = new Resume();
        resume.setJob(job);
        assertTrue(dataScopeService.isResumeInCurrentCompany(resume, hr));
    }

    // ---------- getCurrentCompanyOrThrow ----------
    @Test
    void getCurrentCompanyOrThrow_hrWithoutCompany_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> dataScopeService.getCurrentCompanyOrThrow(userWithRole("HR")));
    }

    @Test
    void getCurrentCompanyOrThrow_withCompany_returnsCompany() {
        User hr = userWithRole("HR");
        hr.setCompany(company(5));
        assertEquals(5L, dataScopeService.getCurrentCompanyOrThrow(hr).getId());
    }
}
