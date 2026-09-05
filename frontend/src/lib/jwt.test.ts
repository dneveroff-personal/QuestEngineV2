import { describe, expect, it } from "vitest";

import { decodeJwtPayload } from "@/lib/jwt";

function fakeJwt(payload: Record<string, unknown>): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${base64url({ alg: "HS256" })}.${base64url(payload)}.fake-signature`;
}

describe("decodeJwtPayload", () => {
  it("читает sub и role из валидного по форме токена", () => {
    const token = fakeJwt({ sub: "odissey", role: "AUTHOR", iat: 1, exp: 2 });

    const payload = decodeJwtPayload(token);

    expect(payload).not.toBeNull();
    expect(payload?.sub).toBe("odissey");
    expect(payload?.role).toBe("AUTHOR");
  });

  it("возвращает null для мусорной строки вместо падения", () => {
    // Критично: api/client.ts и useAuth не должны крашиться, если токен
    // повреждён или отсутствует — только для UI, не для авторизации.
    expect(decodeJwtPayload("not-a-jwt")).toBeNull();
  });

  it("возвращает null для пустой строки", () => {
    expect(decodeJwtPayload("")).toBeNull();
  });

  it("корректно обрабатывает base64url-символы (- и _), которых нет в обычном base64", () => {
    // payload специально содержит байты, кодирующиеся в base64 как + или /,
    // чтобы проверить именно url-safe декодирование, а не просто atob.
    const token = fakeJwt({ sub: "user_with-symbols???", role: "PLAYER", iat: 0, exp: 0 });

    const payload = decodeJwtPayload(token);

    expect(payload?.sub).toBe("user_with-symbols???");
  });
});
