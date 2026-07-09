package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetRoleRequest {

    @NotNull(message = "Роль не может быть пустой")
    private UserRole role;
}
