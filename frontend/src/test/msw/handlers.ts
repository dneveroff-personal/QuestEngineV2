import { http, HttpResponse } from "msw";

import { fakeJwt } from "@/test/fixtures";

export const handlers = [
  http.post("/api/auth/login", async ({ request }) => {
    const body = (await request.json()) as { username: string; password: string };

    if (body.username === "wronguser") {
      return HttpResponse.json(
        {
          type: "about:blank",
          title: "Unauthorized",
          status: 401,
          detail: "Неверное имя пользователя или пароль.",
        },
        { status: 401 },
      );
    }

    return HttpResponse.json({
      publicName: "Test Player",
      token: fakeJwt({ sub: body.username, role: "PLAYER", iat: 0, exp: 9999999999 }),
    });
  }),
];
