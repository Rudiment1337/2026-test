package com.busmonitor.dto;

import lombok.Data;
import java.util.Set;
import java.util.stream.Collectors;
import com.busmonitor.model.User;
import com.busmonitor.model.Role;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private boolean enabled;
    private Set<String> roles;
    private Set<String> permissions;
    public static UserResponseDTO fromUser(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEnabled(user.isEnabled());

        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                .map(Role::getTitle)
                .collect(Collectors.toSet()));
            dto.setPermissions(user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(perm -> perm.getPermission())
                .collect(Collectors.toSet()));
        }
        return dto;
    }
}
