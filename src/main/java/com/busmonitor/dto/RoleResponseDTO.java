package com.busmonitor.dto;

import lombok.Data;
import com.busmonitor.model.Role;
import com.busmonitor.model.Permission;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class RoleResponseDTO {
    private Long id;
    private String title;
    private Set<String> permissions;
    public static RoleResponseDTO fromRole(Role role) {
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(role.getId());
        dto.setTitle(role.getTitle());
        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                .map(Permission::getPermission)
                .collect(Collectors.toSet()));
        }
        return dto;
    }
}
