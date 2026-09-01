# QuestEngine — Frontend Документация

Frontend-документация читается вместе с корневой (`docs/ReadMe.md`), в
частности `03-architecture/`, `04-api/` и `05-security/` — frontend
реализует UI поверх уже принятых там решений и не дублирует их.

Статусы те же, что и в корневой документации:

- 🟢 **Accepted** — согласовано, можно проектировать/реализовывать поверх этого.
- 🟡 **Draft** — основа есть, но требует уточнений.
- ⚪ **TBD** — раздел выделен, содержание ещё не написано.
- 🔵 **Implemented** — помимо описания, уже реализовано в коде и покрыто тестами.

| Документ | Статус |
|---|---|
| [architecture.md](architecture.md) — технологический стек, слои, API-клиент, SSE, формы, границы features | 🟢 |
| [design-system.md](design-system.md) — цвета, типографика, spacing, компоненты | 🟢 |
| [information-architecture.md](information-architecture.md) — разделы приложения, роли, навигация | 🟢 |
| [screens.md](screens.md) — конкретные экраны | 🟢 |
| [user-flows.md](user-flows.md) — пользовательские сценарии | 🟢 |
| [testing-strategy.md](testing-strategy.md) — уровни тестов, MSW как основа разработки против неготового backend, DoD | 🟡 *(порог покрытия и CI — открытые вопросы)* |

## Деплой

Frontend разворачивается как отдельный Docker-образ (nginx + собранная
статика), за общим reverse-proxy с backend — решение и вся топология
зафиксированы в `../08-ops/deployment.md`. Локально `npm run dev`
дополнительно проксирует `/api` и `/auth` на backend через
`vite.config.ts`, чтобы same-origin выполнялся и в dev-режиме.

## Состояние реализации

Frontend-код (`frontend/`) на данный момент — bootstrap-стадия: зависимости
и shadcn/ui подключены, но `src/` не содержит кода приложения (нет
`main.tsx`/`App.tsx`). Актуальное состояние — в `roadmap/backlog.md`
(корневой).

## Известная зависимость от backend

Часть экранов (игровой режим, статистика) блокируется неготовыми
backend-эндпоинтами (`CodeSubmission` runtime, `HintProgress`,
`statistics/`, `POST /auth/refresh`) — см. `04-api/endpoints.md` и
`roadmap/backlog.md`. Рекомендуемый порядок реализации frontend начинается
с экранов, у которых backend уже готов (Quest, Team, Registration), и
использует MSW-моки (`testing-strategy.md`) там, где backend ещё не
существует.
