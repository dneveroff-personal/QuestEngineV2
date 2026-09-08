# QuestEngine — Frontend Документация

Frontend-документация читается вместе с корневой (`docs/ReadMe.md`), в
частности `03-architecture/`, `04-api/` и `05-security/` — frontend
реализует UI поверх уже принятых там решений и не дублирует их.

Статусы те же, что и в корневой документации:

- 🟢 **Accepted** — согласовано, можно проектировать/реализовывать поверх этого.
- 🟡 **Draft** — основа есть, но требует уточнений.
- ⚪ **TBD** — раздел выделен, содержание ещё не написано.

| Документ | Статус |
|---|---|
| [roadmap.md](roadmap.md) — задачи: что можно делать сейчас, что ждёт backend | 🟢 |
| [architecture.md](architecture.md) — технологический стек, слои, API-клиент, SSE, формы, границы features | 🟢 |
| [design-system.md](design-system.md) — цвета, типографика, spacing, компоненты | 🟢 |
| [information-architecture.md](information-architecture.md) — разделы приложения, роли, навигация | 🟢 |
| [screens.md](screens.md) — конкретные экраны | 🟢 |
| [user-flows.md](user-flows.md) — пользовательские сценарии | 🟢 |
| [testing-strategy.md](testing-strategy.md) — уровни тестов, MSW, DoD | 🟢 |

## Деплой

Frontend разворачивается как отдельный Docker-образ (nginx + собранная
статика), за общим reverse-proxy с backend — решение и вся топология
зафиксированы в `../08-ops/deployment.md`. Локально `npm run dev`
дополнительно проксирует `/api` на backend через `vite.config.ts`, чтобы
same-origin выполнялся и в dev-режиме.
