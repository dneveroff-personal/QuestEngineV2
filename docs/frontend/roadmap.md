# Frontend — План реализации

Статус: 🟡 Draft — актуализировать по мере продвижения (см. чек-лист внизу).

## Назначение

Единая точка правды по тому, что во frontend уже сделано, что можно
делать прямо сейчас (backend готов), и что упирается в backend. Основано
на прямой сверке с backend-кодом (`*ControllerIT`, DTO, `Routes.java`), а
не только с `04-api/endpoints.md` — там, где документация и код
разошлись, здесь зафиксирован код как источник истины, с пометкой.

Легенда статусов — та же, что и в остальной документации:
🔵 Implemented · 🟡 Частично / с оговоркой · ⚪ Не начато · 🔴 Заблокировано backend'ом

---

## 1. Реализовано

| Область | Что сделано |
|---|---|
| **Инфраструктура проекта** | Vite + React 19 + TS + Tailwind v4 + shadcn/ui (`base-nova`, `@base-ui/react`). `vite.config.ts` с dev-прокси на backend. Структура `pages/features/api/components/lib` по `architecture.md`. |
| **Деплой** | `frontend/Dockerfile` (Node build → nginx serve), `nginx.conf` (SPA fallback + reverse proxy `/api/*`, SSE-безопасный для будущей статистики), сервис в `docker-compose.local.yml`/`docker-compose.prod.yml`. Задокументировано в `08-ops/deployment.md`. |
| **CI** | `.github/workflows/build.yml` — отдельный job для frontend (lint, test, build, Docker-образ в GHCR), запускается только при изменениях в `frontend/**`. |
| **HTTP-слой** | `api/client.ts` — единая точка запросов, разбор `ProblemDetail` в типизированный `ApiError` (форма сверена с `GlobalExceptionHandler.java`), `NetworkError` отдельно. Access token — в памяти (`lib/auth-token.ts`), не в `localStorage`. |
| **Auth** | `LoginForm`, `RegisterForm` (react-hook-form + zod, схемы дословно зеркалят backend-валидацию `AuthRequestBase`), `useAuth`, `ProtectedRoute`, редирект на `/login`. Работает против реального `POST /api/auth/login` и `/api/auth/register`. |
| **UI-примитивы** | `Button` (был), `Input`, `Label`, `Form*` (RHF-интеграция) — написаны вручную в стиле проекта, т.к. `npx shadcn add` недоступен в этой среде (нет сети). |
| **Каркас страниц** | `RootLayout` (навигация, имя пользователя, выход), заглушки: Home, QuestDetail, MyQuests, Team, Profile — без данных, с TODO-комментариями на нужный эндпоинт. |

---

## 2. Можно делать прямо сейчас — backend полностью готов

Ничего не блокирует, но у части эндпоинтов есть **известные гонки**
(отмечено 🟡 в `endpoints.md`) — учитывать в error-handling UI (см.
раздел 4), не считать багом frontend, если backend вернёт `409`.

### 2.1 Quest — просмотр и участие

- `GET /api/quests/upcoming` → **Home** — список квестов.
- `GET /api/quests/{questId}` → **QuestDetail**.
- `POST /api/quests/register/{questId}/{teamId}` → подать заявку от команды.
- `DELETE /api/quests/register/{questId}` → отменить заявку.
- `GET /api/quests/register/{questId}` → список заявок (для отображения статуса "заявка отправлена").

### 2.2 Team

- `POST /api/teams` → создание команды.
- `GET /api/teams/my`, `GET /api/teams/{teamId}`, `GET /api/teams/{teamId}/members` → **Team** экран.
- `GET /api/teams/search` → поиск команды для вступления.
- `POST /api/teams/{teamId}/request`, `GET /api/teams/requests`, `.../approve`, `.../reject` → вступление в команду (капитан подтверждает).
- `DELETE /api/teams/leave`, `POST /api/teams/transfer-captain/{userId}` → управление составом (видно только капитану — `design-system.md` §11).

### 2.3 Author — CRUD квеста (не игровая механика)

- `POST/PUT/DELETE /api/quests/{questId}` → создание/редактирование/удаление (Quest всегда стартует в `DRAFT`).
- `POST /api/quests/{questId}/publish` → `DRAFT → REGISTRATION`.
- `POST /api/quests/{questId}/finish` → `RUNNING → FINISHED`.
- `POST/GET/PUT/DELETE /api/quests/{questId}/levels`, `/api/levels/{levelId}` → уровни.
- CRUD hints/codes (`.../hints`, `.../codes`) → **редактирование** автором. Не путать с игровым вводом (раздел 4) — это разные вещи под похожими URL.
- `PUT /api/quests/register/{questId}/approve/{teamId}`, `.../teams/{teamId}/reject` → рассмотрение заявок автором.

### 2.4 Административное (если/когда появится Admin UI)

- `PUT /api/users/{userId}/role`, `POST /api/users/{userId}/reset-password`, `GET /api/users/search`.

---

## 3. Готово, но со скрытой оговоркой — учесть в UI

Backend работает, но у этих операций есть подтверждённые гонки
(`03-architecture/concurrency-scenarios.md`) — они не "иногда падают
рандомно", а **систематически** дают `409 Conflict` при одновременных
запросах. Frontend должен относиться к `409` здесь как к ожидаемому
исходу, а не как к неизвестной ошибке:

| Операция | Что может случиться |
|---|---|
| `PUT /api/quests/register/{questId}/approve/{teamId}` | Гонка на лимите команд — Сценарий 1. Показать "место уже заняли" вместо общего "ошибка сервера". |
| `POST /api/quests/progress/{questId}/enter` | Незащищённый повторный вызов — Сценарий 2. На UI: блокировать повторный клик после первого успешного входа (debounce), не полагаться только на backend. |
| CRUD кодов (`/api/codes`) | Проблема глобальной уникальности значения кода — см. `code-submission.md`. Показать понятную ошибку "такой код уже используется в другом уровне", а не сырой `409`. |

---

## 4. Заблокировано backend'ом

### 4.1 Полностью не реализовано на backend

| Что | Где заблокировано |
|---|---|
| **Statistics** | Пакет `statistic/` пуст (нет модели/сервиса/эндпоинта вообще). Экран статистики, SSE (`architecture.md` §11.1) — нечего показывать. |
| **Ввод кода командой во время игры** | Есть только CRUD-редактирование автором (раздел 2.3). Игровой `POST .../submit` не существует. Блокирует **Game Mode** целиком. |
| **Открытие подсказки командой** | Аналогично — только CRUD автором, игровой механики открытия нет. |
| **DNF (Did Not Finish)** | `setDnf()` есть в сервисе, но не выведен ни в один контроллер — недостижимо через API даже для админской панели. |

### 4.2 Auth — модель ещё старая

- `POST /api/auth/login` возвращает **один JWT на 24ч**, не пару access+refresh (ADR-0015 ещё не реализован на backend).
- `POST /api/auth/refresh`, `POST /api/auth/logout` — не существуют. Logout сейчас **только локальный** (чистит `lib/auth-token.ts`, ничего не отзывает на backend).
- Практическое следствие: обновление страницы разлогинивает пользователя (access token в памяти, восстановить нечем — нет refresh-механизма). Не чинить на frontend раньше времени — правильное решение появится вместе с backend (`architecture.md` §9.1 уже описывает целевой интерцептор).

### 4.3 Найдено при подготовке этого плана — нет `GET /api/users/me`

`UserController` умеет: `PUT .../role`, `POST .../reset-password`,
`GET /users/search`. **Эндпоинта "получить свой профиль" нет.** `UserResponse`
уже содержит нужные поля (`id`, `publicName`, `email`, `role`, `createdAt`) —
не хватает только самого метода, который отдаёт его для текущего
аутентифицированного пользователя.

Практическое следствие для двух экранов:

- **Profile** — построить полноценно нельзя: `publicName` есть (пришёл при
  логине), но `email`, `role`, `createdAt` — взять неоткуда без обходного
  пути.
- **Role-based UI** (`architecture.md` §14, пункт "AUTHOR → показать
  Авторскую") — тоже нечем управлять: frontend не может узнать роль
  собственного пользователя.

Обходной путь существует (дёрнуть `GET /api/users/search?username=...`
своим же именем), но это подмена смысла эндпоинта, предназначенного для
админской панели, а не "мой профиль" — использовать его для этого не
стоит. Правильное решение — добавить `GET /api/users/me` на backend;
занести в `roadmap/backlog.md` (сделано, см. ниже).

---

## 5. Рекомендуемый порядок дальнейшей реализации

Обновление прежнего плана с учётом того, что Auth уже готов:

1. ~~Auth + shell~~ — 🔵 готово.
2. **Home / QuestDetail / MyQuests** — backend полностью готов (раздел 2.1). Следующий логичный шаг.
3. **Team** — backend полностью готов (раздел 2.2), чуть больше форм (создание, поиск, заявки).
4. **Author — CRUD раздел** — backend полностью готов (раздел 2.3), самая объёмная часть по количеству форм.
5. **Profile** — частично (раздел 4.3) до появления `GET /api/users/me`, либо отложить до его появления.
6. **Game Mode** — 🔴 заблокирован (раздел 4.1), делать на моках (`testing-strategy.md`, MSW) параллельно с ожиданием backend, не как финальную интеграцию.
7. **Statistics** — 🔴 полностью заблокирован (раздел 4.1), нечего интегрировать раньше, чем появится хоть один эндпоинт.

---

## Чек-лист поддержания актуальности

Обновлять этот документ при:

- [ ] Каждом новом реализованном экране/фиче frontend — переносить из раздела 2/5 в раздел 1.
- [ ] Каждом закрытом backend-пробеле из раздела 4 — переносить в раздел 2, ссылаясь на обновлённый `04-api/endpoints.md`.
- [ ] Появлении `GET /api/users/me` — закрыть находку 4.3, разблокировать Profile.
- [ ] Переходе backend на access+refresh (ADR-0015) — закрыть 4.2, реализовать интерцептор из `architecture.md` §9.1 (сейчас — заглушка с `TODO`).
