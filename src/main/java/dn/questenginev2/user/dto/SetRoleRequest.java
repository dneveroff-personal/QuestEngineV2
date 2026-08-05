package dn.questenginev2.user.dto;

import dn.questenginev2.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record SetRoleRequest(@NotNull(message = "Роль не может быть пустой") UserRole role) {}
