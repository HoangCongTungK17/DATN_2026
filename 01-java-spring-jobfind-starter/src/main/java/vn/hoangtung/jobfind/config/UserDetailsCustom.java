package vn.hoangtung.jobfind.config;

import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import vn.hoangtung.jobfind.service.UserService;

// Đánh dấu đây là một Bean và thuộc tầng Service
@Service
public class UserDetailsCustom implements UserDetailsService {

    private final UserService userService;

    // Sử dụng Constructor Injection để tiêm UserService vào
    public UserDetailsCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Lấy thông tin người dùng từ database thông qua username (ở đây là email)
        vn.hoangtung.jobfind.domain.User user = this.userService.handleGetUserByUsername(username);

        if (user == null) {
            // Nếu không tìm thấy người dùng, ném ra exception để Spring Security xử lý
            throw new UsernameNotFoundException("Username/Password không hợp lệ");
        }

        // 2. Chuyển đổi đối tượng User của bạn thành đối tượng UserDetails mà Spring
        // Security hiểu
        // Constructor của User (org.springframework.security.core.userdetails.User) yêu
        // cầu:
        // - username: định danh duy nhất (dùng email)
        // - password: mật khẩu đã được mã hóa
        // - authorities: danh sách các quyền của người dùng
        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")) // Hard-code quyền là ROLE_USER
        );
    }
}