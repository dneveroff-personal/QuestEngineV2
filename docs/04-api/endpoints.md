# API Endpoints — обзор ресурсов

Статус: 🟡 Draft.

Это не полный контракт (поля запросов/ответов, коды ошибок по каждому полю) — для этого используйте сгенерированную OpenAPI-спеку (`/api-docs`, UI на `/swagger-ui`), которая всегда актуальна относительно кода. Здесь — карта ресурсов по группам, их статус и связь с доменной документацией, чтобы ориентироваться, не читая контроллеры целиком.

Соглашения (формат ошибок, аутентификация, пагинация, версионирование) — в `04-api/conventions.md`.

---

## Auth — `/api/auth`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/auth/login` | 🟡 *(будет возвращать пару access+refresh вместо одного токена — ADR-0015)* |
| POST | `/auth/register` | 🔵 |
| POST | `/auth/reset-admin-password` | 🔵 |
| **POST `/auth/refresh`** | — | ⚪ **Отсутствует.** Обмен refresh-токена на новый access-токен, с ротацией (ADR-0015). |
| **POST `/auth/logout`** | — | ⚪ **Отсутствует.** Отзыв refresh-токена текущей сессии (ADR-0015). |

---

## Users — `/api/users`

| Метод | Путь | Статус |
|---|---|---|
| PUT | `/users/{userId}/role` | 🔵 |
| POST | `/users/{userId}/reset-password` | 🔵 |
| GET | `/users/search` | 🔵 *(пагинация принимается, но не возвращается — см. `conventions.md`)* |

---

## Teams — `/api/teams`

См. `01-domain/team.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/teams` | 🔵 |
| GET | `/teams/{teamId}` | 🔵 |
| GET | `/teams/my` | 🔵 |
| GET | `/teams/{teamId}/members` | 🔵 |
| GET | `/teams/search` | 🔵 *(та же проблема пагинации)* |
| POST | `/teams/{teamId}/request` | 🔵 | заявка на вступление
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
| DELETE | `/quests/{questId}` | 🔵 |
| POST | `/quests/{questId}/publish` | 🔵 *(`DRAFT → REGISTRATION`, с валидацией "аномальных" уровней по ADR-0005)* |
| POST | `/quests/{questId}/finish` | 🔵 *(`RUNNING → FINISHED`, автор; незавершённые QuestProgress получают DNF)* |

---

## Quest Registration — `/api/quests/register`

См. `01-domain/registration.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/register/{questId}/{teamId}` | 🔵 |
| GET | `/quests/register/{questId}` | 🔵 |
| DELETE | `/quests/register/{questId}` | 🔵 | отмена своей заявки |
| PUT | `/quests/register/{questId}/approve/{teamId}` | 🟡 *(есть подтверждённая гонка на лимите команд — см. `concurrency-scenarios.md` Сценарий 1)* |
| PUT | `/quests/register/{questId}/teams/{teamId}/reject` | 🔵 |

---

## Quest Progress — `/api/quests/progress`

См. `01-domain/progress.md`.

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/progress/{questId}/enter` | 🟡 *(есть незащищённый повторный вызов — см. `concurrency-scenarios.md` Сценарий 2)* |
| GET | `/quests/progress/{questId}/{teamId}` | 🔵 |
| GET | `/quests/progress/{questId}` | 🔵 |
| PUT | `/quests/progress/{questId}/{teamId}/finish` | 🟡 *(ручной override для форс-мажорных случаев — основной путь завершения теперь автоматический, ADR-0009)* |
| POST | `/quests/progress/{questId}/{teamId}/codes` | 🔵 *(CodeSubmission, см. `code-submission.md`)* |
| GET | `/quests/progress/{questId}/{teamId}/hints` | 🔵 *(показанные подсказки команды, auto-reveal, ADR-0020)* |
| **DNF endpoint** | — | ⚪ Метод `setDnf()` существует в сервисе, но **не выведен ни в один контроллер** — вызвать через API невозможно. |

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

CRUD автором — см. `01-domain/hint-progress.md`. Игровая механика открытия командой не реализована (см. тот же документ).

| Метод | Путь | Статус |
|---|---|---|
| POST / GET / PUT / DELETE | (CRUD) | 🔵 *(только редактирование автором)* |
| **Показ подсказки командой (auto-reveal)** | — | 🔵 Реализовано через Job 3 (`HintRevealScheduler`), не отдельный HTTP-эндпоинт для триггера — команда узнаёт о показанных подсказках через `GET /api/quests/progress/{questId}/{teamId}/hints` |

---

## Codes — `/api/quests/{questId}/levels/{levelId}/codes`, `/api/codes`

CRUD автором — см. `01-domain/code-submission.md`. Игровой ввод кода командой не реализован.

| Метод | Путь | Статус |
|---|---|---|
| POST / GET / PUT / DELETE | (CRUD) | 🟡 *(CRUD работает, но с проблемой глобальной уникальности значения — см. `code-submission.md`)* |
| **Ввод кода командой** | — | ⚪ Не реализовано, см. `code-submission.md` |

---

## Statistics — не существует

Пакет `statistic/` в проекте создан пустым (нет ни одного файла). Весь `01-domain/statistics-ranking.md` описывает механику, для которой нет вообще никакого кода — ни модели, ни сервиса, ни эндпоинта.

---

## Служебный/тестовый эндпоинт, требующий внимания

`GET /api/test/secure` (`TestController`) — судя по названию и расположению (`auth/controller/TestController.java`), это диагностический эндпоинт для ручной проверки JWT-аутентификации на этапе разработки. Нужно решить: удалить перед первым релизом или явно задокументировать как оставленный намеренно (например, health-check для аутентификации) — иначе он останется в контракте API как "лишняя" незадокументированная поверхность.

---

## Сводка по крупным пробелам, не отражённым построчно выше

1. Нет эндпоинтов публикации (`DRAFT → REGISTRATION`) и финального завершения Quest (`RUNNING → FINISHED`) автором.
2. `setDnf()` реализован в сервисе, но не достижим через API.
3. Вся статистика (`statistic/`) не реализована.
4. Игровые механики `CodeSubmission` и `HintProgress` (ввод кода/открытие подсказки командой во время игры) не реализованы — есть только редактирование автором.
