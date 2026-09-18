import { clearSession, getSession, setSession } from "@/lib/auth-token";
import { ApiError, NetworkError, type ProblemDetail } from "@/api/errors";
import type { LoginResponse } from "@/api/auth";

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  /** Skip auth header / refresh loop (used by refresh itself). */
  skipAuth?: boolean;
}

let refreshInFlight: Promise<boolean> | null = null;

async function parseProblemDetail(response: Response): Promise<ApiError> {
  try {
    const problem = (await response.json()) as ProblemDetail;
    return new ApiError(problem);
  } catch {
    return new ApiError({
      type: "about:blank",
      title: response.statusText || "Unknown Error",
      status: response.status,
      detail: `Сервер вернул ответ, который не удалось разобрать (HTTP ${response.status}).`,
    });
  }
}

async function tryRefresh(): Promise<boolean> {
  const current = getSession();
  if (!current?.refreshToken) return false;

  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const response = await fetch("/api/auth/refresh", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: current.refreshToken }),
        });
        if (!response.ok) {
          clearSession();
          return false;
        }
        const data = (await response.json()) as LoginResponse;
        setSession(data.accessToken, data.publicName, data.refreshToken);
        return true;
      } catch {
        clearSession();
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, skipAuth, ...rest } = options;
  const session = skipAuth ? null : getSession();

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

  if (response.status === 401 && !skipAuth && !path.includes("/api/auth/")) {
    const ok = await tryRefresh();
    if (ok) {
      return apiFetch<T>(path, { ...options, skipAuth: false });
    }
    clearSession();
    throw await parseProblemDetail(response);
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
