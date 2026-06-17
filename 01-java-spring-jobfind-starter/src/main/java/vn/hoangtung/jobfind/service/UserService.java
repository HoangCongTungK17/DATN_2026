package vn.hoangtung.jobfind.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import vn.hoangtung.jobfind.domain.Company;
import vn.hoangtung.jobfind.domain.Role;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.domain.request.ReqRegisterDTO;
import vn.hoangtung.jobfind.domain.response.ResCreateUserDTO;
import vn.hoangtung.jobfind.domain.response.ResUpdateUserDTO;
import vn.hoangtung.jobfind.domain.response.ResUserDTO;
import vn.hoangtung.jobfind.domain.response.ResultPaginationDTO;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.service.GoogleTokenService.GoogleUserInfo;

@Service
public class UserService {
    private static final String PROVIDER_GOOGLE = "GOOGLE";

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final RoleService roleService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            CompanyService companyService,
            RoleService roleService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    public User handleCreateUser(User user) {
        // check company
        if (user.getCompany() != null) {
            Optional<Company> companyOptional = this.companyService.findById(user.getCompany().getId());
            user.setCompany(companyOptional.isPresent() ? companyOptional.get() : null);
        }

        // check role
        if (user.getRole() != null) {
            Role r = this.roleService.fetchById(user.getRole().getId());
            user.setRole(r != null ? r : null);
        } else {
            // Nếu KHÔNG gửi kèm Role, mặc định gán Role USER (id = 2)
            // Lưu ý: Đảm bảo trong Database bảng roles đã có record với id = 2
            Role r = this.roleService.fetchById(2);
            user.setRole(r != null ? r : null);
        }

        return this.userRepository.save(user);
    }

    public User registerUser(ReqRegisterDTO dto) {
        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setAge(dto.getAge());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());
        user.setRole(resolveDefaultUserRole());
        return this.userRepository.save(user);
    }

    @Transactional
    public User findOrCreateGoogleUser(GoogleUserInfo googleUserInfo) {
        User user = this.userRepository.findByProviderAndProviderId(
                PROVIDER_GOOGLE,
                googleUserInfo.subject());
        if (user != null) {
            return user;
        }

        String email = googleUserInfo.email().trim().toLowerCase();
        user = this.userRepository.findByEmail(email);
        if (user != null) {
            user.setProvider(PROVIDER_GOOGLE);
            user.setProviderId(googleUserInfo.subject());
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName(googleUserInfo.name());
            }
            return this.userRepository.save(user);
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(googleUserInfo.name());
        newUser.setPassword(this.passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setProvider(PROVIDER_GOOGLE);
        newUser.setProviderId(googleUserInfo.subject());
        newUser.setRole(resolveDefaultUserRole());
        return this.userRepository.save(newUser);
    }

    private Role resolveDefaultUserRole() {
        Role role = this.roleService.fetchByName("USER");
        if (role == null) {
            role = this.roleService.fetchByName("CANDIDATE");
        }
        if (role == null) {
            role = this.roleService.fetchById(2);
        }
        if (role == null) {
            throw new IllegalStateException("Default USER role is not configured");
        }
        return role;
    }

    public void handleDeleteUser(long id) {
        this.userRepository.deleteById(id);
    }

    public User fetchUserById(long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            return userOptional.get();
        }
        return null;
    }

    public ResultPaginationDTO fetchAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);
        List<ResUserDTO> listUser = pageUser.getContent()
                .stream().map(item -> this.convertToResUserDTO(
                        item))
                .collect(Collectors.toList());

        rs.setResult(listUser);

        return rs;
    }

    public User handleUpdateUser(User reqUser) {
        User currentUser = this.fetchUserById(reqUser.getId());
        if (currentUser != null) {

            currentUser.setAddress(reqUser.getAddress());
            currentUser.setGender(reqUser.getGender());
            currentUser.setAge(reqUser.getAge());
            currentUser.setName(reqUser.getName());

            // check company
            if (reqUser.getCompany() != null) {
                Optional<Company> companyOptional = this.companyService.findById(reqUser.getCompany().getId());
                currentUser.setCompany(companyOptional.isPresent() ? companyOptional.get() : null);
            }

            // check role
            if (reqUser.getRole() != null) {
                Role r = this.roleService.fetchById(reqUser.getRole().getId());
                currentUser.setRole(r != null ? r : null);
            }

            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    public boolean isEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO res = new ResCreateUserDTO();
        ResCreateUserDTO.CompanyUser com = new ResCreateUserDTO.CompanyUser();

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());

        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        return res;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO res = new ResUpdateUserDTO();
        ResUpdateUserDTO.CompanyUser com = new ResUpdateUserDTO.CompanyUser();
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        return res;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO res = new ResUserDTO();
        ResUserDTO.CompanyUser com = new ResUserDTO.CompanyUser();

        ResUserDTO.RoleUser roleUser = new ResUserDTO.RoleUser();
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }

        if (user.getRole() != null) {
            roleUser.setId(user.getRole().getId());
            roleUser.setName(user.getRole().getName());
            res.setRole(roleUser);
        }

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        return res;
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    public void changePassword(String email, String currentPassword, String newPassword) throws Exception {
        User user = this.handleGetUserByUsername(email);
        if (user == null) {
            throw new Exception("User không tồn tại");
        }

        // Verify current password bằng passwordEncoder.matches()
        if (!this.passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new Exception("Mật khẩu hiện tại không đúng");
        }

        // Encode password mới và lưu
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
    }

}
