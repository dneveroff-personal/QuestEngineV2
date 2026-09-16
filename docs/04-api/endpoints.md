# API Endpoints — обзор ресурсов

Статус: 🟡 Draft.

Это не полный контракт — для полей запросов/ответов используйте сгенерированную OpenAPI-спеку. Здесь — карта ресурсов по группам, их статус и связь с доменной документацией.

Соглашения (формат ошибок, аутентификация, пагинация, версионирование) — в `04-api/conventions.md`.

---

## Auth — `/api/auth`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/auth/login` | 🟡 *(будет возвращать пару access+refresh вместо одного токена — ADR-0015)* |
| POST | `/auth/register` | 🔵 |
| POST | `/auth/reset-admin-password` | 🔵 |
| POST | `/auth/refresh` | ⚪ **Отсутствует.** Обмен refresh-токена на новый access-токен, с ротацией (ADR-0015). |
| POST | `/auth/logout` | ⚪ **Отсутствует.** Отзыв refresh-токена текущей сессии (ADR-0015). |

---

## Users — `/api/users`

| Метод | Путь | Статус |
|---|---|---|
| GET | `/users/me` | ⚪ **Отсутствует.** Нужен frontend для профиля и role-based UI. |
| PUT | `/users/{userId}/role` | 🔵 |
| POST | `/users/{userId}/reset-password` | 🔵 |
| GET | `/users/search` | 🟡 *(пагинация реализована; остаются вопросы по доступности поиска и составу `UserResponse`)* |

---

## Teams — `/api/teams`

См. `01-domain/team.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/teams` | 🔵 |
| GET | `/teams/{teamId}` | 🔵 |
| GET | `/teams/my` | 🔵 |
| GET | `/teams/{teamId}/members` | 🔵 |
| GET | `/teams/{teamId}/quests` | ⚪ **Отсутствует.** Нужен frontend для списка квестов/регистраций команды без N+1. |
| GET | `/teams/search` | 🔵 *(пагинация реализована)* |
| POST | `/teams/{teamId}/request` | 🔵 |
| GET | `/teams/requests` | 🔵 |
| POST | `/teams/requests/{requestId}/approve` | 🔵 |
| POST | `/teams/requests/{requestId}/reject` | 🔵 |
| DELETE | `/teams/leave` | 🔵 |
| POST | `/teams/transfer-captain/{userId}` | 🔵 |

---

## Quests — `/api/quests`

См. `01-domain/quest.md`, `02-processes/quest-lifecycle.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests` | 🔵 | создание, всегда в `DRAFT` |
| GET | `/quests/{questId}` | 🔵 |
| GET | `/quests/authors/{authorId}` | 🔵 |
| GET | `/quests/upcoming` | 🔵 |
| PUT | `/quests/{questId}` | 🔵 |
| DELETE | `/quests/{questId}` | 🟡 *(технически работает во всех статусах; бизнес-правило удаления `RUNNING/FINISHED` требует решения)* |
| POST | `/quests/{questId}/publish` | 🔵 *(DRAFT → REGISTRATION, с валидацией уровней по ADR-0005)* |
| POST | `/quests/{questId}/finish` | 🔵 *(RUNNING → FINISHED, автор; незавершённые QuestProgress получают DNF)* |

---

## Quest Registration — `/api/quests/register`

См. `01-domain/registration.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/register/{questId}/{teamId}` | 🟡 *(реализовано; backend пока разрешает регистрацию в любом статусе кроме `FINISHED` — требуется решение о допустимых статусах)* |
| GET | `/quests/register/{questId}` | 🔵 |
| DELETE | `/quests/register/{questId}` | 🔵 |
| PUT | `/quests/register/{questId}/approve/{teamId}` | 🔵 *(конкурентная гонка лимита команд закрыта)* |
| PUT | `/quests/register/{questId}/teams/{teamId}/reject` | 🔵 |

---

## Quest Progress — `/api/quests/progress`

См. `01-domain/progress.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/progress/{questId}/enter` | 🔵 |
| GET | `/quests/progress/{questId}/{teamId}` | 🔵 |
| GET | `/quests/progress/{questId}` | 🔵 |
| PUT | `/quests/progress/{questId}/{teamId}/finish` | 🟡 *(ручной override для форс-мажорных случаев — основной путь завершения автоматический)* |
| POST | `/quests/progress/{questId}/{teamId}/codes` | 🔵 *(CodeSubmission, см. `code-submission.md`)* |
| GET | `/quests/progress/{questId}/{teamId}/hints` | 🔵 *(три состояния видимости)* |
| POST | `/quests/progress/{questId}/{teamId}/hints/{hintId}/take` | 🔵 *(явное взятие BONUS/PENALTY-подсказки)* |
| PUT | `/quests/progress/{questId}/{teamId}/dnf` | 🟡 *(реализовано; момент допустимого вызова ещё требует бизнес-решения)* |

---

## Levels — `/api/quests/{questId}/levels`, `/api/levels`

См. `01-domain/level.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/{questId}/levels` | 🔵 |
| GET | `/quests/{questId}/levels` | 🔵 |
| GET | `/levels/{levelId}` | 🔵 |
| PUT | `/levels/{levelId}` | 🔵 |
| DELETE | `/levels/{levelId}` | 🔵 |

---

## Hints — `/api/quests/{questId}/levels/{levelId}/hints`, `/api/hints`

CRUD автором и игровая механика показа — см. `01-domain/hint-progress.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST / GET / PUT / DELETE | (CRUD) | 🔵 *(редактирование автором)* |
| **Показ подсказки командой** | — | 🔵 REGULAR — автоматически через Job 3; BONUS/PENALTY — явно через `POST .../hints/{hintId}/take` |

---

## Codes — `/api/quests/{questId}/levels/{levelId}/codes`, `/api/codes`

CRUD автором и игровой ввод командой — см. `01-domain/code-submission.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST / GET / PUT / DELETE | (CRUD) | 🔵 *(уникальность `code_value` проверяется в пределах Level)* |
| **Ввод кода командой** | — | 🔵 `POST /api/quests/progress/{questId}/{teamId}/codes` |

---

## Bonus / Penalty — `/api/quest-progress`, `/api/adjustments`

Ручная корректировка автора — один из трёх источников, см. `01-domain/bonus-penalty.md`. Эффект кода и подсказки виден в `QuestProgressResponse.bonusPenaltySeconds`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quest-progress/{questProgressId}/adjustments` | 🟡 *(реализовано; unit-покрытие агрегатора есть, API/integration coverage ещё требуется)* |
| GET | `/quest-progress/{questProgressId}/adjustments` | 🟡 *(реализовано; API/integration coverage ещё требуется)* |
| POST | `/adjustments/{adjustmentId}/revoke` | 🟡 *(реализовано; API/integration coverage ещё требуется; запрещено после `Quest.status = FINISHED`)* |

---

## Statistics — не существует

Пакет `statistic/` в проекте создан пустым. `01-domain/statistics-ranking.md` описывает механику, для которой ещё нет модели, сервиса и эндпоинтов.

---

## Сводка по крупным пробелам

1. Statistics / Ranking (`statistic/`) не реализованы.
2. Auth всё ещё использует один JWT вместо access+refresh (ADR-0015).
3. Отсутствует `GET /api/users/me`.
4. Отсутствует получение квестов текущей команды одним API-запросом.
5. `QuestResponse` не содержит автора.
6. `Quest.maximumTeams` не прокинут в DTO.
7. Требуется решение по статусам регистрации, DNF и удалению Quest.
