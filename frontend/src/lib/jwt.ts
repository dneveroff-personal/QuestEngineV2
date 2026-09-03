/**
 * Декодирует payload JWT ТОЛЬКО для отображения в UI (имя пользователя,
 * роль для условного рендера пунктов меню) — НЕ для авторизации.
 * Подпись не проверяется (да и не может быть проверена в браузере без
 * секрета) — авторизация всегда остаётся на backend, который проверит
 * подпись и просрочку сам на каждый запрос. Если кто-то подделает токен
 * в devtools, максимум, что случится — интерфейс покажет не тот пункт
 * меню, а любой реальный запрос всё равно получит 401/403 от backend.
 *
 * Реальный контракт токена — JwtService.generateToken (backend):
 * claims = { role }, subject = username.
 */
export interface JwtPayload {
  sub: string;
  role: string;
  iat: number;
  exp: number;
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(normalized)) as JwtPayload;
  } catch {
    return null;
  }
}
