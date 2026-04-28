package com.busmonitor.dto;

import lombok.Data;
import com.busmonitor.model.Permission;

@Data
public class PermissionResponseDTO {
    private Long id;
    private String permission;
    private String operation;

    public static PermissionResponseDTO fromPermission(Permission permission) {
        PermissionResponseDTO dto = new PermissionResponseDTO();
        dto.setId(permission.getId());
        dto.setPermission(permission.getPermission());
        dto.setOperation(permission.getOperation());
        return dto;
    }
}
