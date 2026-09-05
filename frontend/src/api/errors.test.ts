import { describe, expect, it } from "vitest";

import { ApiError } from "@/api/errors";

describe("ApiError", () => {
  it("использует detail как message, если он есть", () => {
    const error = new ApiError({
      type: "about:blank",
      title: "Bad Request",
      status: 400,
      detail: "Имя пользователя занято",
    });

    expect(error.message).toBe("Имя пользователя занято");
    expect(error.status).toBe(400);
  });

  it("падает обратно на title, если detail пуст", () => {
    const error = new ApiError({
      type: "about:blank",
      title: "Internal Server Error",
      status: 500,
      detail: "",
    });

    expect(error.message).toBe("Internal Server Error");
  });

  it("fieldError находит сообщение по имени поля из errors[]", () => {
    const error = new ApiError({
      type: "about:blank",
      title: "Validation failed",
      status: 400,
      detail: "Ошибка валидации",
      errors: [
        { field: "username", message: "Слишком короткое имя" },
        { field: "password", message: "Слишком короткий пароль" },
      ],
    });

    expect(error.fieldError("username")).toBe("Слишком короткое имя");
    expect(error.fieldError("password")).toBe("Слишком короткий пароль");
  });

  it("fieldError возвращает undefined для поля без ошибки", () => {
    const error = new ApiError({
      type: "about:blank",
      title: "Validation failed",
      status: 400,
      detail: "Ошибка валидации",
      errors: [{ field: "username", message: "Слишком короткое имя" }],
    });

    expect(error.fieldError("email")).toBeUndefined();
  });

  it("fieldErrors — пустой массив, если errors отсутствует в ответе", () => {
    // Backend не всегда присылает errors[] — только для ошибок валидации
    // (см. комментарий в api/errors.ts). Остальные коды ошибок (401, 404,
    // 409...) его не содержат вообще.
    const error = new ApiError({
      type: "about:blank",
      title: "Not Found",
      status: 404,
      detail: "Команда не найдена",
    });

    expect(error.fieldErrors).toEqual([]);
  });
});
