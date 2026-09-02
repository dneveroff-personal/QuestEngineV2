/**
 * Форма ошибки, которую backend реально возвращает — RFC 7807
 * `ProblemDetail` (см. `GlobalExceptionHandler.java`, `createProblemDetail`).
 *
 * `errors` присутствует только для ошибок валидации
 * (`MethodArgumentNotValidException` / `ConstraintViolationException`) —
 * см. `ValidationError.java` (`{ field, message }`).
 */
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
  errors?: Array<{ field: string; message: string }>;
}

/**
 * Типизированная ошибка API — то, что реально долетает до features.
 * features не должны парсить `Response`/JSON самостоятельно
 * (architecture.md §9.1, "Формат ошибок").
 */
export class ApiError extends Error {
  readonly status: number;
  readonly title: string;
  readonly type: string;
  readonly fieldErrors: Array<{ field: string; message: string }>;

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title);
    this.name = "ApiError";
    this.status = problem.status;
    this.title = problem.title;
    this.type = problem.type;
    this.fieldErrors = problem.errors ?? [];
  }

  /** Ошибка валидации конкретного поля формы (например, для react-hook-form setError). */
  fieldError(field: string): string | undefined {
    return this.fieldErrors.find((e) => e.field === field)?.message;
  }
}

/** Сетевая ошибка (нет соединения, backend недоступен) — отдельно от ApiError, у неё нет ProblemDetail. */
export class NetworkError extends Error {
  constructor(cause: unknown) {
    super("Не удалось связаться с сервером. Проверьте подключение к интернету.");
    this.name = "NetworkError";
    this.cause = cause;
  }
}
