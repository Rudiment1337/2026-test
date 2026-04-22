package com.busmonitor.controller;

import com.busmonitor.model.Permission;
import com.busmonitor.model.Role;
import com.busmonitor.model.User;
import com.busmonitor.repository.PermissionRepository;
import com.busmonitor.repository.RoleRepository;
import com.busmonitor.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

	// User CRUD

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("GET /api/admin/users - fetching all users");
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        log.info("GET /api/admin/users/{} - fetching user", id);
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<User> createUser(@RequestBody User user,
                                           @RequestParam(required = false) Set<Long> roleIds) {
        log.info("POST /api/admin/users - creating user: {}", user.getUsername());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (roleIds != null && !roleIds.isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<User> updateUser(@PathVariable Long id,
                                           @RequestBody User userData,
                                           @RequestParam(required = false) Set<Long> roleIds) {
        log.info("PUT /api/admin/users/{} - updating user", id);

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (userData.getUsername() != null) {
            user.setUsername(userData.getUsername());
        }
        if (userData.getPassword() != null && !userData.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userData.getPassword()));
        }
        if (userData.isEnabled() != user.isEnabled()) {
            user.setEnabled(userData.isEnabled());
        }
        if (roleIds != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("DELETE /api/admin/users/{} - deleting user", id);

        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

	// Role CURD

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<List<Role>> getAllRoles() {
        log.info("GET /api/admin/roles - fetching all roles");
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        log.info("GET /api/admin/roles/{} - fetching role", id);
        return roleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<Role> createRole(@RequestBody Role role,
                                           @RequestParam(required = false) Set<Long> permissionIds) {
        log.info("POST /api/admin/roles - creating role: {}", role.getTitle());

        if (permissionIds != null && !permissionIds.isEmpty()) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
            role.setPermissions(permissions);
        }

        Role savedRole = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRole);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<Role> updateRole(@PathVariable Long id,
                                           @RequestBody Role roleData,
                                           @RequestParam(required = false) Set<Long> permissionIds) {
        log.info("PUT /api/admin/roles/{} - updating role", id);

        Role role = roleRepository.findById(id).orElse(null);
        if (role == null) {
            return ResponseEntity.notFound().build();
        }

        if (roleData.getTitle() != null) {
            role.setTitle(roleData.getTitle());
        }
        if (permissionIds != null) {
            Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));
            role.setPermissions(permissions);
        }

        Role updatedRole = roleRepository.save(role);
        return ResponseEntity.ok(updatedRole);
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        log.info("DELETE /api/admin/roles/{} - deleting role", id);

        if (!roleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

	// Permis CURD

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        log.info("GET /api/admin/permissions - fetching all permissions");
        return ResponseEntity.ok(permissionRepository.findAll());
    }

    @GetMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long id) {
        log.info("GET /api/admin/permissions/{} - fetching permission", id);
        return permissionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:create')")
    public ResponseEntity<Permission> createPermission(@RequestBody Permission permission) {
        log.info("POST /api/admin/permissions - creating permission: {}", permission.getPermission());

        Permission savedPermission = permissionRepository.save(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedPermission);
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('permission:delete')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        log.info("DELETE /api/admin/permissions/{} - deleting permission", id);

        if (!permissionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        permissionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
