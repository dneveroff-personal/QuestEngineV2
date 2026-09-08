# Frontend Testing Strategy & Definition of Done

Статус: 🟢 Accepted.

## Уровни тестов

E2E намеренно не делаем — решение принято.

1. **Unit-тесты** (`Vitest`) — чистые функции и hooks в изоляции: утилиты
   `lib/`, схемы валидации (`zod`), нетривиальные вычисления. Без рендера
   компонентов.
2. **Component-тесты** (`Vitest` + `React Testing Library` + `MSW`) —
   рендер компонента/feature с замоканным API-слоем. Проверяют поведение
   через доступный пользователю интерфейс (роли, текст), не через
   внутренние детали реализации. Основной, наиболее часто пишущийся
   уровень.

## MSW — не только для тестов

Часть backend API ещё не реализована (`docs/roadmap/backlog.md`). MSW
используется и в тестах, и в разработке — как способ строить UI против
зафиксированного контракта (`04-api/endpoints.md`) до появления
соответствующего endpoint'а. Когда endpoint становится реально доступен —
мок удаляется, а не остаётся "на всякий случай".

## Тестируемость time-based UI

Логика, зависящая от текущего времени (игровой таймер, `architecture.md`
§18), не должна напрямую вызывать `Date.now()` внутри тестируемой
функции — время должно быть параметром или явно замоканным
(`vi.useFakeTimers()`), иначе тест становится flaky.

## Definition of Done для новой feature

- [ ] Backend-контракт зафиксирован в `04-api/endpoints.md` (реально
      готов или замокан через MSW с пометкой "ожидает backend").
- [ ] Unit-тесты — для нетривиальной логики вне компонентов.
- [ ] Component-тесты — happy path и хотя бы одно `error`-состояние на
      каждый компонент, выполняющий запрос к API.
- [ ] Соблюдена `7.1 Feature Boundaries` (`architecture.md`) — feature не
      импортируется в обход её `index.ts`.
- [ ] Формы на `react-hook-form` + `zod` (`architecture.md` §12.1),
      ошибка backend-валидации отображается корректно.
- [ ] Проверена доступность (`architecture.md` §23) — клавиатурная
      навигация, `aria-label` для icon-only controls.
- [ ] `docs/roadmap/backlog.md` обновлён, если feature закрывает пункт оттуда.

## Инфраструктура

`vite.config.ts` — блок `test` (`jsdom`, `src/test/setup.ts`). MSW —
`src/test/msw/` (`setupServer`, не `setupWorker` — тесты в Node/jsdom, не
в браузере), `onUnhandledRequest: "error"`. Общие фикстуры —
`src/test/fixtures.ts` (`fakeJwt`, `loginAs`), `src/test/render.tsx`
(`renderWithProviders`). CI — `.github/workflows/build.yml`, `npm run
test -- --run`.

## Открытые вопросы

- Порог покрытия — не зафиксирован, решить при заметном росте объёма тестов.
