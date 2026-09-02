import { z } from "zod";

/**
 * Зеркалит backend AuthRequestBase.java дословно — не "разумные
 * дефолты", а именно те границы, что реально проверяет @NotBlank/@Size
 * на сервере. Если сервер это изменит, схема должна разойтись явно
 * (architecture.md §12.1) — а не совпасть случайно.
 */
const usernameSchema = z
  .string()
  .min(3, "Имя пользователя должно быть от 3 до 64 символов")
  .max(64, "Имя пользователя должно быть от 3 до 64 символов");

const passwordSchema = z.string().min(5, "Пароль должен быть не менее 5 символов");

export const loginSchema = z.object({
  username: usernameSchema,
  password: passwordSchema,
});

export type LoginFormValues = z.infer<typeof loginSchema>;

export const registerSchema = z.object({
  username: usernameSchema,
  password: passwordSchema,
  email: z.string().email("Email должен быть валидным").max(254),
  publicName: z.string().max(128, "Публичное имя должно быть не более 128 символов").optional(),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
