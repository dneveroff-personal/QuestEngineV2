import { clearSession, getSession } from "@/lib/auth-token";
import { ApiError, NetworkError, type ProblemDetail } from "@/api/errors";

/**
 * Единая точка выполнения HTTP-запросов (architecture.md §9). Все файлы
 * в api/ (auth.ts, ...) используют только её, а не сырой fetch напрямую.
 *
 * ВАЖНО про 401 (architecture.md §9.1): полный механизм там описан для
 * целевой access+refresh модели (ADR-0015). Backend её ещё не
 * реализовал (см. roadmap/backlog.md — POST /api/auth/refresh
 * отсутствует), поэтому сейчас 401 просто завершает сессию — повторный
 * запрос через /api/auth/refresh НЕ выполняется, потому что этого
 * эндпоинта физически нет.
 *
 * TODO(ADR-0015): когда backend добавит POST /api/auth/refresh, заменить
 * блок "если 401" ниже на полный механизм из architecture.md §9.1:
 * вызвать /api/auth/refresh, при успехе — повторить исходный запрос один
 * раз, при неудаче — как сейчас (clearSession). Не забыть про дедупликацию
 * конкурентных refresh-вызовов (см. §9.1, "если несколько запросов...").
 */

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

async function parseProblemDetail(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail;
    return new ApiError(problem);
  } catch {
    // Backend всегда отвечает ProblemDetail на ошибку (GlobalExceptionHandler
    // ловит Exception.class как fallback) — сюда попадаем только если
    // ответ вообще не JSON (например, ошибка самого nginx/прокси).
    return new ApiError({
      type: "about:blank",
      title: response.statusText || "Unknown Error",
      status: response.status,
      detail: `Сервер вернул ответ, который не удалось разобрать (HTTP ${response.status}).`,
    });
  }
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...rest } = options;
  const session = getSession();

  let response: Response;
  try {
    response = await fetch(path, {
      ...rest,
      headers: {
        "Content-Type": "application/json",
        ...(session ? { Authorization: `Bearer ${session.token}` } : {}),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (cause) {
    throw new NetworkError(cause);
  }

  if (response.status === 401) {
    clearSession();
    throw await parseProblemDetail(response);
  }

  if (!response.ok) {
    throw await parseProblemDetail(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
