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
| **Home / QuestDetail / MyQuests** | Home — список `upcoming`-квестов. QuestDetail — карточка квеста + `RegistrationPanel` (все статусы: нет команды / не подано / PENDING / APPROVED / REJECTED, плюс статусы квеста DRAFT/REGISTRATION/RUNNING/FINISHED). MyQuests — работает через временный N+1 workaround (см. 4.4 — обратного эндпоинта на backend нет). |
| **Team (полностью)** | Просмотр состава, создание, поиск по названию, заявка на вступление (своя), приглашение капитаном по username, подтверждение/отклонение заявок и приглашений (`JoinRequestsPanel` — один компонент для обеих ролей заявки), передача капитанства, выход из команды. |
| **Author-CRUD** | Quest CRUD (создание/редактирование/удаление/публикация/завершение), Level CRUD, вложенные Hints/Codes CRUD внутри уровня, рассмотрение заявок команд автором (`RegistrationReviewPanel`). Пункт "Авторская" в навигации виден по роли из JWT. Список "моих квестов" — через тот же обходной резолв userId, что и в Team (см. 4.6). |
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
  логине), но `email`, `createdAt` — взять неоткуда без обходного пути.
- **Role-based UI** (`architecture.md` §14, пункт "AUTHOR → показать
  Авторскую") — **частично закрыто находкой при реализации Team**: JWT
  реально содержит `role` в claims (`JwtService.generateToken` — `claims.put("role", role)`),
  и frontend теперь декодирует его на клиенте (`lib/jwt.ts`, только для UI,
  не для авторизации — та по-прежнему целиком на backend). См.
  `useAuth().role`. `GET /api/users/me` всё ещё нужен для Profile
  (`email`/`createdAt` в JWT нет и не должно быть), но для Role-based UI
  он уже не блокер.

Обходной путь для `email`/`createdAt` существует (дёрнуть
`GET /api/users/search?username=...` своим же именем), но это подмена
смысла эндпоинта, предназначенного для админской панели, а не "мой
профиль" — использовать его для этого не стоит. Правильное решение —
добавить `GET /api/users/me` на backend; занести в `roadmap/backlog.md`
(сделано, см. ниже).

### 4.4 Найдено при реализации — нет обратного эндпоинта "квесты моей команды"

`GET /api/quests/register/{questId}` отдаёт список команд, зарегистрированных
на конкретный квест — но нет обратной операции "квесты, на которые
зарегистрирована моя команда". Для экрана **MyQuests** это означает, что
чистого способа его построить нет.

**Временное решение** (`pages/quests/MyQuestsPage.tsx`): берём
`/api/quests/upcoming` и для каждого квеста отдельно запрашиваем его
регистрации (N+1), фильтруем на клиенте по `myTeam.id`. Работает, но:

- N+1 запросов — приемлемо только пока квестов мало (pet-проект), не
  паттерн для копирования;
- показывает только `upcoming` квесты — прошедшие (`FINISHED`), в которых
  команда участвовала, не попадают, т.к. `/upcoming` их не отдаёт вообще.

Правильное решение — `GET /api/teams/{teamId}/quests` (или аналог) на
backend; занесено в `roadmap/backlog.md`. Когда появится — заменить
workaround в `MyQuestsPage.tsx` (он снабжён комментарием с этой же
инструкцией).

### 4.5 Найдено при реализации Team — три несостыковки в DTO

Все три подробно описаны с точными ссылками на backend-код в
`roadmap/backlog.md` (там же — рекомендации по починке). Коротко:

1. **`TeamMemberDto.id`** — это id записи `TeamMember`, не `User.id`.
   `POST /api/teams/transfer-captain/{userId}` ожидает именно `User.id`,
   которого нигде в ответе `GET /api/teams/my` просто нет.
2. **`TeamMemberDto.name` / `TeamResponse.captainName`** заполняются через
   `User.getUsername()`, а не `getPublicName()` — расходится с Auth
   (`LoginResponse.publicName`). Frontend вынужден декодировать JWT
   (`sub`-claim, `lib/jwt.ts`) только чтобы получить свой username и
   сравнить его с этими полями (`useAuth().username`, использован в
   `features/teams/utils.ts`, `isCaptainOf`).
3. **`GET /api/users/search`** ищет по username через `LIKE`, но не
   возвращает username в ответе — при неоднозначном совпадении frontend
   не может понять, какой результат верный.

Практическое следствие для "передать капитанство"
(`TeamMembersList.tsx`): резолвим `userId` через (3), но **отказываемся
действовать**, если результат неоднозначен (0 или 2+ совпадений), вместо
того чтобы молча брать первый — риск ошибиться получателем капитанства
слишком велик, чтобы гадать. Разово это можно так и оставить, но при
масштабировании (общая база пользователей растёт) отказы будут
происходить всё чаще — эта заплатка не рассчитана жить долго.

### 4.6 Найдено при реализации Author-CRUD

1. **`QuestResponse` не отдаёт `authorId`/`authorName` нигде** — самая
   значимая находка раздела. Ни `GET /api/quests/{questId}`, ни
   `GET /api/quests/upcoming` не говорят, кто автор. Практические
   следствия:
   - Список "мои квесты как автор" (`AuthorQuestsPage.tsx`) резолвит
     `userId` через тот же обходной путь, что и в Team
     (`features/authoring/useMyAuthorId.ts`) — риск здесь ниже, чем при
     передаче капитанства (худший случай — пустой/неверный список,
     сразу заметно, ничего необратимого), поэтому решение чуть мягче:
     при неоднозначности просто показываем предупреждение, а не
     блокируем страницу целиком.
   - Кнопка "Редактировать" на `QuestDetailPage` и весь `/author/quests/:id/edit`
     **не могут** проверить "это правда ваш квест?" — показываются любому
     с ролью AUTHOR/ADMIN, а фактическую проверку авторства делает
     backend через `403` при попытке сохранить/опубликовать/удалить.
     Это осознанный компромисс, а не недосмотр — см. комментарий в
     `EditQuestPage.tsx`.
2. **`delete(questId)` не проверяет статус квеста** — можно удалить
   `RUNNING`/`FINISHED` квест (в отличие от `publish`/`finish`, которые
   строго проверяют статус). Frontend компенсирует усиленным
   предупреждением в `QuestLifecycleActions.tsx`, если статус
   `RUNNING`/`FINISHED`, но это не защита — реальное решение должно быть
   на backend.

Обе находки подробно описаны с точными ссылками на backend-код в
`roadmap/backlog.md`.

---

## 5. Рекомендуемый порядок дальнейшей реализации

Обновление прежнего плана с учётом того, что Auth уже готов:

1. ~~Auth + shell~~ — 🔵 готово.
2. ~~Home / QuestDetail / MyQuests~~ — 🔵 готово (MyQuests — через временный workaround, см. 4.4).
3. ~~Team~~ — 🔵 готово (просмотр, создание, поиск, заявки/приглашения, передача капитанства, выход — см. 4.5 про сопутствующие DTO-находки).
4. ~~Author — CRUD раздел~~ — 🔵 готово (Quest/Level/Hint/Code CRUD, рассмотрение заявок автором — см. 4.6 про DTO-находки).
5. **Profile** — частично (раздел 4.3) до появления `GET /api/users/me` (email/createdAt), роль уже доступна через JWT. Следующий логичный шаг — небольшой объём работы.
6. **Game Mode** — 🔴 заблокирован (раздел 4.1), делать на моках (`testing-strategy.md`, MSW) параллельно с ожиданием backend, не как финальную интеграцию.
7. **Statistics** — 🔴 полностью заблокирован (раздел 4.1), нечего интегрировать раньше, чем появится хоть один эндпоинт.

---

## Чек-лист поддержания актуальности

Обновлять этот документ при:

- [ ] Каждом новом реализованном экране/фиче frontend — переносить из раздела 2/5 в раздел 1.
- [ ] Каждом закрытом backend-пробеле из раздела 4 — переносить в раздел 2, ссылаясь на обновлённый `04-api/endpoints.md`.
- [ ] Появлении `GET /api/users/me` — закрыть находку 4.3, разблокировать Profile.
- [ ] Переходе backend на access+refresh (ADR-0015) — закрыть 4.2, реализовать интерцептор из `architecture.md` §9.1 (сейчас — заглушка с `TODO`).
