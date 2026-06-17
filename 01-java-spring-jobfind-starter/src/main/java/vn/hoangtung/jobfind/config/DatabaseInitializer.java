package vn.hoangtung.jobfind.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoangtung.jobfind.domain.Permission;
import vn.hoangtung.jobfind.domain.Role;
import vn.hoangtung.jobfind.domain.User;
import vn.hoangtung.jobfind.repository.PermissionRepository;
import vn.hoangtung.jobfind.repository.RoleRepository;
import vn.hoangtung.jobfind.repository.UserRepository;
import vn.hoangtung.jobfind.util.constant.GenderEnum;

@Service
public class DatabaseInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hoangtung.bootstrap.admin.email:}")
    private String bootstrapAdminEmail;

    @Value("${hoangtung.bootstrap.admin.password:}")
    private String bootstrapAdminPassword;

    public DatabaseInitializer(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");

        if (this.permissionRepository.count() == 0) {
            ArrayList<Permission> permissions = new ArrayList<>();

            permissions.add(new Permission("Create a company", "/api/v1/companies", "POST", "COMPANIES"));
            permissions.add(new Permission("Update a company", "/api/v1/companies", "PUT", "COMPANIES"));
            permissions.add(new Permission("Delete a company", "/api/v1/companies/{id}", "DELETE", "COMPANIES"));
            permissions.add(new Permission("Get a company by id", "/api/v1/companies/{id}", "GET", "COMPANIES"));
            permissions.add(new Permission("Get companies with pagination", "/api/v1/companies", "GET", "COMPANIES"));

            permissions.add(new Permission("Create a job", "/api/v1/jobs", "POST", "JOBS"));
            permissions.add(new Permission("Update a job", "/api/v1/jobs", "PUT", "JOBS"));
            permissions.add(new Permission("Delete a job", "/api/v1/jobs/{id}", "DELETE", "JOBS"));
            permissions.add(new Permission("Get a job by id", "/api/v1/jobs/{id}", "GET", "JOBS"));
            permissions.add(new Permission("Get jobs with pagination", "/api/v1/jobs", "GET", "JOBS"));

            permissions.add(new Permission("Create a permission", "/api/v1/permissions", "POST", "PERMISSIONS"));
            permissions.add(new Permission("Update a permission", "/api/v1/permissions", "PUT", "PERMISSIONS"));
            permissions.add(new Permission("Delete a permission", "/api/v1/permissions/{id}", "DELETE",
                    "PERMISSIONS"));
            permissions.add(new Permission("Get a permission by id", "/api/v1/permissions/{id}", "GET",
                    "PERMISSIONS"));
            permissions.add(new Permission("Get permissions with pagination", "/api/v1/permissions", "GET",
                    "PERMISSIONS"));

            permissions.add(new Permission("Create a resume", "/api/v1/resumes", "POST", "RESUMES"));
            permissions.add(new Permission("Update a resume", "/api/v1/resumes", "PUT", "RESUMES"));
            permissions.add(new Permission("Delete a resume", "/api/v1/resumes/{id}", "DELETE", "RESUMES"));
            permissions.add(new Permission("Get a resume by id", "/api/v1/resumes/{id}", "GET", "RESUMES"));
            permissions.add(new Permission("Get resumes with pagination", "/api/v1/resumes", "GET", "RESUMES"));
            permissions.add(new Permission("Get list resumes by user", "/api/v1/resumes/by-user", "POST",
                    "RESUMES"));

            permissions.add(new Permission("Create a role", "/api/v1/roles", "POST", "ROLES"));
            permissions.add(new Permission("Update a role", "/api/v1/roles", "PUT", "ROLES"));
            permissions.add(new Permission("Delete a role", "/api/v1/roles/{id}", "DELETE", "ROLES"));
            permissions.add(new Permission("Get a role by id", "/api/v1/roles/{id}", "GET", "ROLES"));
            permissions.add(new Permission("Get roles with pagination", "/api/v1/roles", "GET", "ROLES"));

            permissions.add(new Permission("Create a user", "/api/v1/users", "POST", "USERS"));
            permissions.add(new Permission("Update a user", "/api/v1/users", "PUT", "USERS"));
            permissions.add(new Permission("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS"));
            permissions.add(new Permission("Get a user by id", "/api/v1/users/{id}", "GET", "USERS"));
            permissions.add(new Permission("Get users with pagination", "/api/v1/users", "GET", "USERS"));

            permissions.add(new Permission("Create a subscriber", "/api/v1/subscribers", "POST", "SUBSCRIBERS"));
            permissions.add(new Permission("Update a subscriber", "/api/v1/subscribers", "PUT", "SUBSCRIBERS"));
            permissions.add(new Permission("Delete a subscriber", "/api/v1/subscribers/{id}", "DELETE",
                    "SUBSCRIBERS"));
            permissions.add(new Permission("Get a subscriber by id", "/api/v1/subscribers/{id}", "GET",
                    "SUBSCRIBERS"));
            permissions.add(new Permission("Get subscribers with pagination", "/api/v1/subscribers", "GET",
                    "SUBSCRIBERS"));
            permissions.add(new Permission("Get subscriber's skill pagination", "/api/v1/subscribers/skills",
                    "POST", "SUBSCRIBERS"));

            permissions.add(new Permission("Upload a file", "/api/v1/files", "POST", "FILES"));
            permissions.add(new Permission("Download a file", "/api/v1/files", "GET", "FILES"));

            this.permissionRepository.saveAll(permissions);
        }

        ensurePermission("Sync AI job vectors", "/api/v1/ai/sync", "POST", "AI");
        ensurePermission("Trigger subscriber email", "/api/v1/email", "GET", "EMAIL");
        ensurePermission("Create a skill", "/api/v1/skills", "POST", "SKILLS");
        ensurePermission("Update a skill", "/api/v1/skills", "PUT", "SKILLS");
        ensurePermission("Delete a skill", "/api/v1/skills/{id}", "DELETE", "SKILLS");
        ensurePermission("Get skills with pagination", "/api/v1/skills", "GET", "SKILLS");

        ensureRole("SUPER_ADMIN", "Admin with all permissions", true);
        ensureRole("HR", "Company recruiter with scoped company, job and resume permissions", true);
        ensureRole("USER", "Default candidate user", true);

        if (this.userRepository.count() == 0 && !isBlank(bootstrapAdminEmail) && !isBlank(bootstrapAdminPassword)) {
            User adminUser = new User();
            adminUser.setEmail(bootstrapAdminEmail.trim().toLowerCase());
            adminUser.setAddress("hn");
            adminUser.setAge(25);
            adminUser.setGender(GenderEnum.MALE);
            adminUser.setName("Bootstrap super admin");
            adminUser.setPassword(this.passwordEncoder.encode(bootstrapAdminPassword));
            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminUser.setRole(adminRole);
            }
            this.userRepository.save(adminUser);
        } else if (this.userRepository.count() == 0) {
            System.out.println(">>> SKIP ADMIN BOOTSTRAP: configure hoangtung.bootstrap.admin.email/password");
        }

        Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
        List<Permission> allPermissions = this.permissionRepository.findAll();
        if (adminRole != null) {
            adminRole.setPermissions(allPermissions);
            this.roleRepository.save(adminRole);
        }

        syncDefaultHrPermissions(allPermissions);

        User adminUser = !isBlank(bootstrapAdminEmail)
                ? this.userRepository.findByEmail(bootstrapAdminEmail.trim().toLowerCase())
                : null;
        if (adminRole != null && adminUser != null) {
            adminUser.setRole(adminRole);
            this.userRepository.save(adminUser);
        }

        System.out.println(">>> END INIT DATABASE");
    }

    private void ensurePermission(String name, String apiPath, String method, String module) {
        boolean exists = this.permissionRepository.existsByModuleAndApiPathAndMethod(module, apiPath, method);
        if (!exists) {
            this.permissionRepository.save(new Permission(name, apiPath, method, module));
        }
    }

    private void ensureRole(String name, String description, boolean active) {
        Role existingRole = this.roleRepository.findByName(name);
        if (existingRole == null) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setActive(active);
            this.roleRepository.save(role);
        }
    }

    private void syncDefaultHrPermissions(List<Permission> allPermissions) {
        Role role = this.roleRepository.findByName("HR");
        if (role == null) {
            return;
        }

        List<Permission> currentPermissions = role.getPermissions() == null
                ? new ArrayList<>()
                : new ArrayList<>(role.getPermissions());
        currentPermissions.removeIf(this::isForbiddenHrPermission);

        for (Permission permission : allPermissions) {
            if (!isDefaultHrPermission(permission)) {
                continue;
            }
            boolean alreadyAssigned = currentPermissions.stream()
                    .anyMatch(item -> samePermission(item, permission));
            if (!alreadyAssigned) {
                currentPermissions.add(permission);
            }
        }

        role.setPermissions(currentPermissions);
        this.roleRepository.save(role);
    }

    private boolean isDefaultHrPermission(Permission permission) {
        if (permission == null || permission.getModule() == null) {
            return false;
        }
        if ("JOBS".equals(permission.getModule()) || "RESUMES".equals(permission.getModule())) {
            return true;
        }
        if (!"COMPANIES".equals(permission.getModule())) {
            return false;
        }
        return ("GET".equals(permission.getMethod()) && "/api/v1/companies".equals(permission.getApiPath()))
                || ("GET".equals(permission.getMethod()) && "/api/v1/companies/{id}".equals(permission.getApiPath()))
                || ("PUT".equals(permission.getMethod()) && "/api/v1/companies".equals(permission.getApiPath()));
    }

    private boolean isForbiddenHrPermission(Permission permission) {
        if (permission == null || permission.getModule() == null || permission.getMethod() == null) {
            return false;
        }
        if ("COMPANIES".equals(permission.getModule())) {
            return "POST".equals(permission.getMethod()) || "DELETE".equals(permission.getMethod());
        }
        if ("SKILLS".equals(permission.getModule())) {
            return !"GET".equals(permission.getMethod());
        }
        return false;
    }

    private boolean samePermission(Permission first, Permission second) {
        return first != null && second != null
                && Objects.equals(first.getApiPath(), second.getApiPath())
                && Objects.equals(first.getMethod(), second.getMethod())
                && Objects.equals(first.getModule(), second.getModule());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
