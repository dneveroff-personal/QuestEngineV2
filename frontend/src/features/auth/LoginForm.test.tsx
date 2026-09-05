import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useLocation } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { LoginForm } from "@/features/auth/LoginForm";
import { renderWithProviders } from "@/test/render";

/** Показывает текущий путь — так тест видит, что navigate("/") реально произошёл, без мока react-router. */
function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
}

function renderLoginForm() {
  return renderWithProviders(
    <>
      <LoginForm />
      <LocationProbe />
    </>,
    { route: "/login" },
  );
}

describe("LoginForm", () => {
  it("успешный вход перенаправляет на главную", async () => {
    const user = userEvent.setup();
    renderLoginForm();

    await user.type(screen.getByLabelText("Имя пользователя"), "odissey");
    await user.type(screen.getByLabelText("Пароль"), "correct-password");
    await user.click(screen.getByRole("button", { name: "Войти" }));

    await waitFor(() => {
      expect(screen.getByTestId("location-probe")).toHaveTextContent("/");
    });
  });

  it("показывает общую ошибку при неверных учётных данных, не привязанную к полю", async () => {
    // handlers.ts: username="wronguser" → 401 без errors[] (не field-level).
    const user = userEvent.setup();
    renderLoginForm();

    await user.type(screen.getByLabelText("Имя пользователя"), "wronguser");
    await user.type(screen.getByLabelText("Пароль"), "whatever");
    await user.click(screen.getByRole("button", { name: "Войти" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Неверное имя пользователя или пароль.",
    );
    // Остаёмся на /login — не перешли на защищённый экран с неверным логином.
    expect(screen.getByTestId("location-probe")).toHaveTextContent("/login");
  });

  it("показывает клиентскую ошибку валидации без обращения к backend", async () => {
    // Слишком короткое имя (< 3 символов, схема в features/auth/schemas.ts)
    // — не должно даже дойти до сети; onUnhandledRequest: "error" в setup.ts
    // провалит тест, если это предположение неверно.
    const user = userEvent.setup();
    renderLoginForm();

    await user.type(screen.getByLabelText("Имя пользователя"), "ab");
    await user.type(screen.getByLabelText("Пароль"), "somepassword");
    await user.click(screen.getByRole("button", { name: "Войти" }));

    expect(
      await screen.findByText("Имя пользователя должно быть от 3 до 64 символов"),
    ).toBeInTheDocument();
  });
});
