# API Endpoints — обзор ресурсов

Статус: 🟡 Draft.

Это не полный контракт — для полей запросов/ответов используйте сгенерированную OpenAPI-спеку. Здесь — карта ресурсов по группам, их статус и связь с доменной документацией.

Соглашения (формат ошибок, аутентификация, пагинация, версионирование) — в `04-api/conventions.md`.

---

## Auth — `/api/auth`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/auth/login` | 🔵 *(access + refresh, ADR-0015)* |
| POST | `/auth/register` | 🔵 |
| POST | `/auth/reset-admin-password` | 🔵 |
| POST | `/auth/refresh` | 🔵 |
| POST | `/auth/logout` | 🔵 |

---

## Users — `/api/users`

| Метод | Путь | Статус |
|---|---|---|
| GET | `/users/me` | 🔵 |
| PUT | `/users/{userId}/role` | 🔵 |
| POST | `/users/{userId}/reset-password` | 🔵 |
| GET | `/users/search` | 🔵 |

---

## Teams — `/api/teams`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/teams` | 🔵 |
| GET | `/teams/{teamId}` | 🔵 |
| GET | `/teams/my` | 🔵 |
| GET | `/teams/{teamId}/members` | 🔵 |
| GET | `/teams/{teamId}/quests` | 🔵 |
| GET | `/teams/my/quests` | 🔵 |
| GET | `/teams/search` | 🔵 |
| POST | `/teams/{teamId}/request` | 🔵 |
| GET | `/teams/requests` | 🔵 |
| POST | `/teams/requests/{requestId}/approve` | 🔵 |
| POST | `/teams/requests/{requestId}/reject` | 🔵 |
| DELETE | `/teams/leave` | 🔵 |
| POST | `/teams/transfer-captain/{userId}` | 🔵 |

---

## Quests — `/api/quests`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests` | 🔵 |
| GET | `/quests/{questId}` | 🔵 |
| GET | `/quests/authors/{authorId}` | 🔵 |
| GET | `/quests/upcoming` | 🔵 |
| PUT | `/quests/{questId}` | 🔵 |
| DELETE | `/quests/{questId}` | 🟡 |
| POST | `/quests/{questId}/publish` | 🔵 |
| POST | `/quests/{questId}/finish` | 🔵 |

---

## Quest Registration — `/api/quests/register`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/register/{questId}/{teamId}` | 🟡 |
| GET | `/quests/register/{questId}` | 🔵 |
| DELETE | `/quests/register/{questId}` | 🔵 |
| PUT | `/quests/register/{questId}/approve/{teamId}` | 🔵 |
| PUT | `/quests/register/{questId}/teams/{teamId}/reject` | 🔵 |

---

## Quest Progress — `/api/quests/progress`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/progress/{questId}/enter` | 🔵 |
| GET | `/quests/progress/{questId}/{teamId}` | 🔵 |
| GET | `/quests/progress/{questId}/{teamId}/current-level` | 🔵 *(Game Mode: ACTIVE level + content + autoTransitionAt + hints + mainCodesSolved)* |
| GET | `/quests/progress/{questId}` | 🔵 |
| PUT | `/quests/progress/{questId}/{teamId}/finish` | 🟡 |
| POST | `/quests/progress/{questId}/{teamId}/codes` | 🔵 |
| GET | `/quests/progress/{questId}/{teamId}/hints` | 🔵 |
| POST | `/quests/progress/{questId}/{teamId}/hints/{hintId}/take` | 🔵 |
| PUT | `/quests/progress/{questId}/{teamId}/dnf` | 🟡 |

---

## Levels / Hints / Codes / Bonus-Penalty

CRUD уровней, подсказок, кодов и ручные корректировки — 🔵 / 🟡 как прежде (см. предыдущие ревизии).

---

## Statistics — не существует

Пакет `statistic/` пуст. См. backlog §5.

---

## Сводка по крупным пробелам

1. Statistics / Ranking.
2. `Quest.maximumTeams` в DTO.
3. Бизнес-решения: DNF, статусы регистрации, удаление Quest.
