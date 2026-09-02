# Frontend Architecture

## 1. Назначение

Frontend является отдельным клиентским приложением QuestEngine.

Frontend отвечает за:

* пользовательский интерфейс;
* навигацию;
* отображение состояния приложения;
* формы;
* взаимодействие пользователя с backend API;
* отображение игровых состояний;
* адаптивность;
* доступность;
* визуальное представление статистики.

Frontend **не является источником истины для игровых и доменных правил**.

Правила проведения Quest, переходов между уровнями, проверки кодов,
расчёта времени, изменения состояния игры, определения результатов и
авторизации принадлежат backend.

Frontend получает состояние от backend, отображает его и отправляет
пользовательские команды.

---

# 2. Общая архитектура

Целевая архитектура:

```text
┌──────────────────────────────────────┐
│              Browser                 │
│                                      │
│        React + TypeScript             │
│                                      │
│  React Router                         │
│  TanStack Query                       │
│  Tailwind CSS                         │
│  shadcn/ui                            │
│  Lucide Icons                         │
└──────────────────┬───────────────────┘
                   │
                   │ HTTP / API
                   ▼
┌──────────────────────────────────────┐
│             Backend API              │
│                                      │
│          QuestEngine API             │
└──────────────────┬───────────────────┘
                   │
                   ▼
             Domain / Database
```

Frontend не обращается непосредственно к базе данных.

Frontend не должен зависеть от внутренней структуры Java-классов backend.

---

# 3. Архитектурный boundary

Frontend взаимодействует с backend только через API boundary.

```text
Frontend
    │
    │ API contract
    ▼
Backend API
    │
    ▼
Domain
```

Frontend не должен знать:

* структуру database;
* JPA entities;
* внутренние service classes;
* repository;
* Kafka topology;
* внутреннее распределение backend по модулям;
* внутреннее распределение backend по микросервисам.

Frontend знает только API-контракт, необходимый для работы пользовательского
интерфейса.

Это позволяет в будущем изменить backend:

```text
                ┌── Auth Service
                │
Frontend ──► API Gateway ──► Quest Service
                │
                ├── Team Service
                │
                └── Game Service
```

без необходимости переписывать frontend-компоненты только из-за внутренней
перестройки backend.

---

# 4. Основные технологические решения

Целевой frontend stack:

* React;
* TypeScript;
* Vite;
* React Router;
* TanStack Query;
* Tailwind CSS;
* shadcn/ui;
* Lucide Icons.

## React

Используется как основной UI framework.

## TypeScript

Используется для:

* компонентов;
* hooks;
* API-клиента;
* API-моделей;
* форм;
* состояния UI;
* типизации server state.

Frontend не должен использовать `any` там, где можно определить тип.

## Vite

Используется как build tool и development environment.

## React Router

Отвечает за маршрутизацию приложения.

Маршруты должны отражать пользовательскую информационную архитектуру.

Например:

```text
/
 /quests/{questId}
 /my-quests
 /team
 /profile
 /author
 /author/quests/{questId}/edit
 /quests/{questId}/play
 /quests/{questId}/statistics
```

## TanStack Query

Используется для server state.

Server state включает, например:

* список Quest;
* данные Quest;
* Team;
* регистрацию команды;
* QuestProgress;
* LevelProgress;
* submissions;
* hints;
* statistics;
* author data.

TanStack Query отвечает за загрузку, кэширование, обновление и синхронизацию
данных с backend.

Frontend не должен без необходимости копировать server state в собственное
глобальное состояние.

## Tailwind CSS

Используется как основной механизм layout и styling.

## shadcn/ui

Используется как основа reusable UI components.

Компоненты являются частью frontend-проекта и могут адаптироваться под
Design System QuestEngine.

## Lucide Icons

Используются для интерфейсных иконок.

---

# 5. Архитектурные слои

Frontend условно разделяется следующим образом:

```text
Pages
  ↓
Features
  ↓
API / Queries
  ↓
Backend API
```

Дополнительный слой UI:

```text
Pages
  ↓
Features
  ↓
UI Components
```

---

# 6. Pages

`pages` содержит страницы приложения.

Page отвечает за композицию интерфейса конкретного маршрута.

Page не должна содержать большой объём бизнес-логики.

Пример:

```text
QuestPage
    ├── QuestHeader
    ├── QuestInfo
    ├── QuestParticipants
    └── QuestAction
```

Page собирает компоненты и features в единый пользовательский экран.

---

# 7. Features

Feature представляет законченный пользовательский сценарий или
функциональную область.

Примеры:

```text
auth
team
quest
registration
quest-progress
level-progress
code-submission
hints
statistics
author
```

Feature может содержать:

* components;
* hooks;
* queries;
* mutations;
* forms;
* local UI state;
* feature-specific utilities.

Feature не должна дублировать backend business logic.

## 7.1 Feature Boundaries

Feature экспортирует наружу только то, что явно перечислено в её `index.ts`
(public API этой feature).

Пример:

```text
features/
└── code-submission/
    ├── index.ts              ← единственная точка входа наружу
    ├── SubmitCodeForm.tsx
    ├── useSubmitCode.ts
    ├── submissionSchema.ts
    └── ...
```

`index.ts` реэкспортирует только то, что действительно нужно снаружи:

```ts
export { SubmitCodeForm } from "./SubmitCodeForm";
export { useSubmitCode } from "./useSubmitCode";
```

Другая feature (например, `quest-progress`) может импортировать только из
`code-submission` (публичный API), но не из внутренних файлов:

```text
✅ import { SubmitCodeForm } from "@/features/code-submission";
❌ import { submissionSchema } from "@/features/code-submission/submissionSchema";
```

Это правило дешёво соблюдать с самого начала и дорого навести задним числом,
когда features начнут напрямую импортировать внутренности друг друга.

Исключение: `pages/` может импортировать из нескольких features одновременно
— это её прямая задача (композиция экрана).

---

# 8. UI Components

Общие UI-компоненты находятся отдельно от бизнес-функций.

Например:

```text
components/
└── ui/
    ├── Button
    ├── Input
    ├── Dialog
    ├── Card
    ├── Badge
    ├── Table
    └── ...
```

Эти компоненты не должны знать о QuestProgress, Team или Quest.

Например:

```text
Button
```

не должен знать, что такое Quest.

А:

```text
SubmitCodeButton
```

может находиться внутри соответствующей feature.

---

# 9. API Layer

API layer отвечает за непосредственное взаимодействие с backend.

Пример:

```text
api/
├── client.ts
├── auth.ts
├── quests.ts
├── teams.ts
├── progress.ts
├── submissions.ts
└── statistics.ts
```

API layer содержит:

* HTTP requests;
* API-specific types;
* обработку HTTP ошибок;
* authentication credentials;
* преобразование технического HTTP-ответа в используемый frontend формат,
  если такое преобразование действительно необходимо.

API layer не должен содержать UI-логику.

## 9.1 HTTP Client и Auth Interceptor

`api/client.ts` — единая точка выполнения HTTP-запросов. Все файлы в
`api/` (`quests.ts`, `teams.ts`, ...) используют только его, а не сырой
`fetch` напрямую.

Backend использует access+refresh модель (см. `04-api/conventions.md`,
ADR-0015): access token живёт 15 минут, refresh — хранится и ротируется
на backend.

Клиент обязан реализовать единый механизм:

```text
Запрос
  │
  ▼
401 Unauthorized?
  │
  ├── нет → вернуть ответ как есть
  │
  └── да → POST /api/auth/refresh
              │
              ├── успех → сохранить новый access token
              │             → повторить исходный запрос один раз
              │
              └── ошибка → считать сессию завершённой
                            → очистить состояние аутентификации
                            → redirect на экран входа
```

Важные ограничения этого механизма:

* повторный запрос выполняется **не более одного раза** — если после
  `refresh` запрос снова вернул `401`, второй `refresh` не запускается;
* если несколько запросов одновременно получили `401`, `refresh` должен
  быть вызван **один раз**, а не по разу на каждый запрос (иначе
  конкурентные refresh-вызовы гонятся друг с другом за ротацию токена на
  backend);
* frontend не хранит refresh token самостоятельно — он используется
  backend-ом согласно контракту `04-api/conventions.md` и не должен быть
  доступен клиентскому JavaScript-коду для чтения.

### Формат ошибок

Backend возвращает ошибки в едином формате (`04-api/conventions.md`).
`client.ts` разбирает этот формат один раз и передаёт наверх
типизированную ошибку, а не сырой HTTP response — features не должны
самостоятельно парсить тело ответа на ошибку.

---

# 10. API Types

Frontend должен иметь типизированное представление API-контрактов.

Например:

```text
api/
└── types/
    ├── auth.ts
    ├── quest.ts
    ├── team.ts
    ├── progress.ts
    ├── submission.ts
    └── statistics.ts
```

Эти типы описывают данные, которыми frontend обменивается с backend.

Они не являются второй копией domain model backend.

Например, frontend может иметь:

```text
QuestResponse
LevelResponse
QuestProgressResponse
LevelProgressResponse
StatisticsResponse
```

но не должен самостоятельно реализовывать domain rules этих объектов.

---

# 11. Server State

Backend является источником истины для server state.

Примеры:

```text
Quest
Team
Registration
QuestProgress
LevelProgress
CodeSubmission
Hint
Statistics
```

Для server state используется TanStack Query.

Frontend должен учитывать состояния:

```text
loading
success
error
```

а также background refetching и другие состояния запроса.

## 11.1 Real-time Server State (SSE)

Live-статистика передаётся через Server-Sent Events, не через polling
(ADR-0014). Это единственный server state в проекте, который обновляется
push-ом от backend, а не по запросу клиента — остальной server state
(Quest, Team, QuestProgress, ...) следует обычной модели TanStack Query
(request → response → кэш).

Подход:

```text
useStatisticsStream(questId)
    │
    ├── открывает EventSource на SSE-эндпоинт статистики
    │
    ├── на каждое событие
    │       → queryClient.setQueryData(["statistics", questId], data)
    │
    └── закрывает соединение при размонтировании компонента
```

SSE-события пишутся напрямую в кэш TanStack Query того же query key,
которым бы пользовался обычный `useQuery` для первичной загрузки —
это позволяет компонентам читать статистику одним и тем же способом
независимо от того, пришли данные первым запросом или SSE-событием.

Frontend обязан обрабатывать разрыв соединения:

* `EventSource` переподключается автоматически (браузерное поведение по
  умолчанию) — этого достаточно для MVP, отдельная ручная логика
  реконнекта не требуется;
* пока соединение не установлено или прервано, интерфейс должен явно
  показывать это состояние (например, "обновление приостановлено"), а не
  тихо показывать устаревшие данные как актуальные.

SSE используется только для статистики. Остальной realtime в проекте не
требуется (ADR-0014 явно ограничивает scope — WebSocket не нужен).

---

# 12. Client State

Client state используется только для данных, принадлежащих интерфейсу.

Например:

```text
открыта ли модалка
выбранная вкладка
состояние sidebar
локальное состояние формы
состояние фильтра
настройки UI
```

Не следует помещать в глобальный client state:

```text
QuestProgress
LevelProgress
Statistics
Team
```

если это не требуется конкретным UI-сценарием.

## 12.1 Forms и валидация

Формы используют **react-hook-form** для состояния полей и **zod** для
схемы валидации. Это устоявшаяся связка в React-экосистеме, совместимая с
shadcn/ui `Form`-компонентами.

```text
Форма
  │
  ├── react-hook-form → состояние полей, dirty/touched, submit
  │
  └── zod schema → правила валидации, привязывается через
                    @hookform/resolvers/zod
```

Схема заводится рядом с формой, внутри соответствующей feature:

```text
features/
└── auth/
    ├── LoginForm.tsx
    └── loginSchema.ts
```

Принципиально важное ограничение (следует из раздела 3, "Frontend не
является источником истины"):

**Клиентская валидация — это UX-ускорение, а не замена backend-валидации.**

Frontend может заранее подсветить пустое поле или неверный формат email,
чтобы не заставлять пользователя ждать round-trip ради очевидной ошибки.
Но окончательное решение о корректности данных всегда принимает backend
— frontend обязан корректно отобразить ошибку валидации, пришедшую от
backend, даже если клиентская схема её не поймала (что означает: схемы
могут разойтись, и это не баг frontend, а сигнал сверить их
преднамеренно).

---

# 13. Authentication

Frontend получает authentication state от backend.

Frontend отвечает за:

* хранение необходимого authentication state;
* передачу credentials/token;
* отображение авторизованного состояния;
* redirect на authentication screens;
* защиту UI-маршрутов.

Backend остаётся источником истины для authentication и authorization.

Frontend не должен самостоятельно определять:

```text
"пользователь действительно имеет право выполнить действие"
```

---

# 14. Authorization и Role-based UI

Frontend может использовать информацию о роли и доступных возможностях
для улучшения UX.

Например:

```text
AUTHOR
    → показать "Авторскую"

CAPTAIN
    → показать управление командой

ADMIN
    → показать административные функции
```

Однако это только механизм отображения.

Backend всегда повторно проверяет право выполнения операции.

Особенно важно учитывать, что пользовательские возможности могут
комбинироваться:

```text
PLAYER + MEMBER
PLAYER + CAPTAIN
AUTHOR + MEMBER
AUTHOR + CAPTAIN
ADMIN + MEMBER
ADMIN + CAPTAIN
```

Frontend не должен считать эти состояния взаимоисключающими.

---

# 15. Quest и Game Domain

Frontend следует backend domain model.

Основные понятия:

```text
Quest
 │
 ├── Level
 ├── Level
 └── Level

QuestProgress
 │
 ├── LevelProgress
 ├── LevelProgress
 └── LevelProgress
```

`Quest` описывает созданную автором игру.

`QuestProgress` описывает прохождение Quest конкретной командой.

`Level` описывает уровень Quest.

`LevelProgress` описывает прохождение конкретного Level конкретной
командой.

Frontend отображает эти состояния, но не управляет их жизненным циклом
самостоятельно.

---

# 16. Игровой режим

Игровой режим является отдельным UI-контекстом.

```text
Application
│
├── Normal Mode
│   ├── Home
│   ├── My Quests
│   ├── Team
│   ├── Profile
│   └── Author
│
└── Game Mode
    └── QuestProgress
        └── LevelProgress
```

В Game Mode обычная навигация приложения скрывается.

Основная задача:

> Пользователь должен максимально быстро понимать,
> что ему необходимо делать прямо сейчас.

Игровой интерфейс должен быть mobile-first.

---

# 17. Игровое состояние

Игровой экран получает актуальное состояние от backend.

Условно:

```text
QuestProgress
    │
    ├── status
    ├── questStartedAt
    └── currentLevelProgress
            │
            ├── level
            ├── status
            ├── openedAt
            ├── completedAt
            └── autoTransitionAt
```

Frontend отображает это состояние.

Frontend не создаёт следующий `LevelProgress`.

Frontend не переводит `QuestProgress` в `FINISHED`.

Frontend не определяет завершение уровня.

Все эти действия выполняет backend.

---

# 18. Timer

Таймер является визуальным представлением серверного состояния.

Frontend может рассчитывать отображаемое оставшееся время на основании
серверного `autoTransitionAt` и текущего времени.

```text
remaining = autoTransitionAt - currentTime
```

Frontend может обновлять отображение каждую секунду.

Однако:

```text
локальный таймер закончился
        ≠
frontend изменил состояние игры
```

Истечение времени на клиенте не является командой backend.

Фактический автопереход определяется backend.

---

# 19. Level Transition

Переход между уровнями полностью контролируется backend.

Frontend может получить:

```text
LevelProgress completed
```

после чего backend возвращает актуальное состояние следующего уровня.

Frontend должен отобразить новый уровень.

Frontend не должен самостоятельно:

* определять следующий уровень;
* создавать LevelProgress;
* вычислять autoTransitionAt;
* изменять QuestProgress.

---

# 20. Code Submission

Ввод кода является пользовательской командой.

Условно:

```text
User
 │
 ▼
Code Input
 │
 ▼
POST submission
 │
 ▼
Backend
 │
 ▼
Submission Result
```

Backend определяет результат:

```text
CORRECT
INCORRECT
DUPLICATE
```

Frontend только визуализирует результат.

История submission также является server state.

Frontend не должен самостоятельно решать:

* правильный ли код;
* duplicate ли код;
* закрывает ли код уровень;
* сколько кодов ещё необходимо;
* должен ли произойти переход.

---

# 21. Statistics

Статистика является server state.

Frontend получает готовые данные статистики от backend.

Frontend отвечает за:

* визуальное представление;
* адаптивность;
* сортировку только если она разрешена контрактом;
* раскрытие дополнительной информации;
* отображение текущего и финального состояния.

Frontend **не определяет победителя**.

Frontend **не рассчитывает позицию команды**.

Frontend **не пересчитывает итоговое время**.

Frontend визуализирует результат, определённый backend.

---

# 22. Responsive Architecture

Frontend является responsive web application.

Приоритет:

```text
Game Mode:
Mobile > Tablet > Desktop

Normal Mode:
Mobile = Desktop

Author/Admin:
Desktop-first, но responsive
```

Игровой интерфейс проектируется прежде всего для смартфонов.

Авторские и административные интерфейсы могут использовать более плотные
desktop-oriented layouts.

---

# 23. Accessibility

Frontend должен соблюдать базовые требования accessibility.

Минимальные требования:

* semantic HTML;
* keyboard navigation;
* visible focus;
* labels для form controls;
* достаточный contrast;
* отсутствие зависимости только от цвета;
* `aria-label` для icon-only controls;
* понятные сообщения об ошибках;
* корректное управление focus в dialogs и forms.

---

# 24. Design System Boundary

Визуальные правила определяются отдельным документом:

```text
design-system.md
```

Architecture не фиксирует конкретные цвета, размеры шрифтов, spacing или
визуальные детали компонентов.

Architecture определяет только технические и структурные принципы.

---

# 25. Information Architecture Boundary

Структура пользовательских разделов определяется:

```text
information-architecture.md
```

Конкретные экраны определяются:

```text
screens.md
```

Пользовательские сценарии определяются:

```text
user-flows.md
```

Таким образом:

```text
architecture.md
        │
        ├── information-architecture.md
        │
        ├── screens.md
        │
        ├── user-flows.md
        │
        └── design-system.md
```

Каждый документ отвечает за свою область и не должен дублировать остальные.

---

# 26. Project Structure

Предлагаемая структура:

```text
src/
├── app/
│   ├── router/
│   ├── providers/
│   └── layouts/
│
├── components/
│   └── ui/
│
├── features/
│   ├── auth/
│   ├── team/
│   ├── quest/
│   ├── registration/
│   ├── quest-progress/
│   ├── level-progress/
│   ├── code-submission/
│   ├── hints/
│   ├── statistics/
│   └── author/
│
├── pages/
│   ├── auth/
│   ├── quests/
│   ├── team/
│   ├── game/
│   ├── profile/
│   └── author/
│
├── api/
│   ├── client.ts
│   ├── auth.ts
│   ├── quests.ts
│   ├── teams.ts
│   ├── progress.ts
│   ├── submissions.ts
│   ├── statistics.ts
│   └── types/
│
├── hooks/
│
├── lib/
│
└── styles/
```

Структура является начальной архитектурой и может уточняться по мере
реализации.

Главный принцип:

```text
UI
↓
Feature
↓
API / Server State
↓
Backend
```

---

# 27. Архитектурные ограничения

Frontend не должен:

1. обращаться к базе данных;
2. реализовывать backend business rules;
3. определять победителя;
4. самостоятельно завершать QuestProgress;
5. самостоятельно завершать LevelProgress;
6. самостоятельно выполнять автопереход;
7. самостоятельно определять правильность кода;
8. самостоятельно вычислять итоговую позицию команды;
9. дублировать server state без необходимости;
10. зависеть от внутренней структуры backend;
11. предполагать, что роли пользователя взаимоисключающие.

---

# 28. Главный архитектурный принцип

Frontend QuestEngine является **presentation and interaction layer**.

```text
Backend
    ↓
Source of Truth
    ↓
Frontend
    ↓
Presentation + User Interaction
```

Frontend отвечает за то, чтобы пользователю было:

* понятно;
* быстро;
* удобно;
* красиво;
* доступно;
* безопасно взаимодействовать с системой.

Backend отвечает за то, чтобы система была:

* корректной;
* консистентной;
* авторизованной;
* предсказуемой;
* соответствующей доменным правилам.

Frontend и backend взаимодействуют через явный API-контракт.

Это является основой Specification First подхода QuestEngine.
