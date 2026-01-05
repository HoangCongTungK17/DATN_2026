package vn.hoangtung.jobfind.repository;

import org.springframework.stereotype.Repository;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    // Spring Data JPA tự động tạo các phương thức CRUD cơ bản.
    // Chúng ta có thể thêm các phương thức truy vấn tùy chỉnh nếu cần.
    User findByEmail(String email);

    boolean existsByEmail(String email);

    User findByRefreshTokenAndEmail(String token, String email);

    List<User> findByCompany(Company company);

}
