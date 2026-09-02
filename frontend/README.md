# QuestEngine — Frontend

React + TypeScript + Vite + TanStack Query + Tailwind CSS + shadcn/ui.

**Что готово, что можно делать прямо сейчас и что ждёт backend —
[`../docs/frontend/roadmap.md`](../docs/frontend/roadmap.md).** Это
основной документ для того, "что делать дальше".

Архитектурные и продуктовые решения задокументированы отдельно и являются
источником истины:

- [`../docs/frontend/architecture.md`](../docs/frontend/architecture.md) — технологии, слои, границы features, HTTP-клиент, SSE, формы.
- [`../docs/frontend/design-system.md`](../docs/frontend/design-system.md) — визуальный язык, semantic tokens.
- [`../docs/frontend/information-architecture.md`](../docs/frontend/information-architecture.md) — разделы приложения.
- [`../docs/frontend/screens.md`](../docs/frontend/screens.md) — конкретные экраны.
- [`../docs/frontend/user-flows.md`](../docs/frontend/user-flows.md) — пользовательские сценарии.
- [`../docs/frontend/testing-strategy.md`](../docs/frontend/testing-strategy.md) — уровни тестов, MSW, Definition of Done.

## Запуск

```bash
npm install
npm run dev
```

## Другие команды

```bash
npm run build     # tsc -b && vite build
npm run lint       # oxlint
npm run test        # vitest
npm run test:e2e     # playwright test
```

## Состояние

Что реализовано, что можно делать прямо сейчас (backend готов) и что
заблокировано — [`../docs/frontend/roadmap.md`](../docs/frontend/roadmap.md).
Состояние backend в целом — `../docs/roadmap/backlog.md`.
