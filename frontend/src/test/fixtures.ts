import { setSession } from "@/lib/auth-token";

/**
 * Валидный по форме (header.payload.signature), но не подписанный
 * настоящим секретом JWT — для тестов достаточно, decodeJwtPayload
 * (lib/jwt.ts) не проверяет подпись, только читает payload.
 */
export function fakeJwt(payload: Record<string, unknown>): string {
  const base64url = (obj: object) =>
    btoa(JSON.stringify(obj)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  return `${base64url({ alg: "HS256" })}.${base64url(payload)}.fake-signature`;
}

/**
 * Устанавливает сессию напрямую через module-singleton (lib/auth-token.ts),
 * без реального похода в /api/auth/login — для тестов компонентов,
 * которым нужен залогиненный useAuth() (username/role), но которые сами
 * не тестируют форму входа. Не забыть clearSession() в afterEach теста,
 * иначе сессия «утечёт» в соседние it() в том же файле (module-singleton
 * общий на весь файл, Vitest не сбрасывает его между отдельными it()).
 */
export function loginAs(username: string, role = "PLAYER", publicName = username): void {
  setSession(fakeJwt({ sub: username, role, iat: 0, exp: 9999999999 }), publicName);
}
