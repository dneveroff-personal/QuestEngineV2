# QuestEngine — Frontend

React + TypeScript + Vite + TanStack Query + Tailwind CSS + shadcn/ui.

Архитектурные и продуктовые решения задокументированы отдельно и являются
источником истины — этот файл только про то, как запустить проект локально:

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

Актуальный статус реализации (что готово на backend, что заблокировано) —
`../docs/roadmap/backlog.md`.
