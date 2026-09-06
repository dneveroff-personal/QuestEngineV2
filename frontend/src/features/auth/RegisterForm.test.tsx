import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { useLocation } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { RegisterForm } from "@/features/auth/RegisterForm";
import { server } from "@/test/msw/server";
import { fakeJwt } from "@/test/fixtures";
import { renderWithProviders } from "@/test/render";

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location-probe">{location.pathname}</div>;
}

function renderRegisterForm() {
  return renderWithProviders(
    <>
      <RegisterForm />
      <LocationProbe />
    </>,
    { route: "/register" },
  );
}

function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  return Promise.all([
    user.type(screen.getByLabelText("Имя пользователя"), "newplayer"),
    user.type(screen.getByLabelText("Email"), "newplayer@example.com"),
    user.type(screen.getByLabelText("Пароль"), "validpass"),
  ]);
}

describe("RegisterForm", () => {
  it("успешная регистрация сразу авторизует и перенаправляет на главную", async () => {
    server.use(
      http.post("/api/auth/register", () => {
        return HttpResponse.json(
          {
            publicName: "New Player",
            token: fakeJwt({ sub: "newplayer", role: "PLAYER", iat: 0, exp: 9999999999 }),
          },
          { status: 201 },
        );
      }),
    );
    const user = userEvent.setup();
    renderRegisterForm();

    await fillValidForm(user);
    await user.click(screen.getByRole("button", { name: "Зарегистрироваться" }));

    await waitFor(() => {
      expect(screen.getByTestId("location-probe")).toHaveTextContent("/");
    });
  });

  it("привязывает ошибку валидации username от backend к полю, не к общему сообщению", async () => {
    // Пример: клиентская схема не поймала, а backend поймал (architecture.md §12.1 —
    // backend всегда источник истины, даже если клиентская схема с ним разошлась).
    server.use(
      http.post("/api/auth/register", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Validation failed",
            status: 400,
            detail: "Ошибка валидации",
            errors: [{ field: "username", message: "Такое имя пользователя уже занято" }],
          },
          { status: 400 },
        );
      }),
    );
    const user = userEvent.setup();
    renderRegisterForm();

    await fillValidForm(user);
    await user.click(screen.getByRole("button", { name: "Зарегистрироваться" }));

    expect(await screen.findByText("Такое имя пользователя уже занято")).toBeInTheDocument();
    // Это field-level ошибка — не должно появляться общее сообщение с role="alert".
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("показывает общую ошибку, если errors[] от backend пуст", async () => {
    server.use(
      http.post("/api/auth/register", () => {
        return HttpResponse.json(
          {
            type: "about:blank",
            title: "Internal Server Error",
            status: 500,
            detail: "Что-то пошло не так на сервере.",
          },
          { status: 500 },
        );
      }),
    );
    const user = userEvent.setup();
    renderRegisterForm();

    await fillValidForm(user);
    await user.click(screen.getByRole("button", { name: "Зарегистрироваться" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Что-то пошло не так на сервере.");
  });

  it("клиентская валидация email не пускает пустой запрос на backend", async () => {
    const user = userEvent.setup();
    renderRegisterForm();

    await user.type(screen.getByLabelText("Имя пользователя"), "newplayer");
    await user.type(screen.getByLabelText("Пароль"), "validpass");
    // email специально не заполнен
    await user.click(screen.getByRole("button", { name: "Зарегистрироваться" }));

    expect(await screen.findByText("Email должен быть валидным")).toBeInTheDocument();
  });
});
