import { http, HttpResponse } from "msw";

/**
 * Валидный по форме (header.payload.signature), но не подписанный
 * настоящим секретом JWT — для тестов достаточно, decodeJwtPayload
 * (lib/jwt.ts) не проверяет подпись, только читает payload.
 */
function fakeJwt(payload: Record<string, unknown>): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${base64url({ alg: "HS256" })}.${base64url(payload)}.fake-signature`;
}

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
