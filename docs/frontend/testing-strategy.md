# Frontend Testing Strategy & Definition of Done

Статус: 🟡 Draft.

## Назначение

Зафиксировать подход к тестированию frontend до того, как написана первая
строка кода — по аналогии с `07-quality/testing-strategy.md` backend-а,
чтобы дисциплина тестирования не появлялась случайно, feature за feature,
а была решена один раз и применялась осознанно ко всем новым features
(`auth`, `code-submission`, `quest-progress`, ...).

---

## Три уровня теста

По аналогии с backend (`*ServiceImplTest` / `*ControllerTest` / `*ControllerIT`),
для frontend фиксируется свой трёхуровневый паттерн:

1. **Unit-тесты** (`Vitest`) — чистые функции и hooks в изоляции: утилиты
   `lib/`, схемы валидации (`zod`), нетривиальные вычисления (например,
   форматирование `remaining = autoTransitionAt - currentTime`, раздел 18
   `architecture.md`). Без рендера компонентов.
2. **Component-тесты** (`Vitest` + `React Testing Library`) — рендер
   отдельного компонента/feature с замоканным API-слоем (`msw` — Mock
   Service Worker). Проверяют поведение: что видит пользователь, как
   компонент реагирует на клики, ввод, состояния `loading/error/success`.
   Тестируется через доступный пользователю интерфейс (роли, текст), а не
   через внутренние детали реализации компонента.
3. **E2E-тесты** (`Playwright`) — не делаем!

Component-тесты — основной, наиболее часто пишущийся уровень (аналог
`*ServiceImplTest` у backend по частоте использования). 

---

## Mock Service Worker как основной способ разработки против backend

Учитывая находку из `docs/frontend/architecture.md` о том, что часть
backend API (`CodeSubmission` runtime, `HintProgress`, `statistics`) ещё не
реализована (см. `roadmap/backlog.md`), **MSW используется не только в
тестах, но и в разработке** — как способ строить UI против зафиксированного
контракта (`04-api/endpoints.md`) до того, как соответствующий backend
эндпоинт реально существует.

```text
Компонент / feature
      │
      ▼
api/client.ts  →  fetch("/api/...")
      │
      ▼
MSW перехватывает запрос (dev/test)
      │
      ▼
Возвращает данные по зафиксированному контракту
```

Когда backend-эндпоинт становится реально доступен — мок для него
удаляется, а не остаётся "на всякий случай": расхождение между живым мок-ом
и давно готовым реальным API — источник незамеченных багов.

---

## Тестируемость time-based UI

Игровой таймер (раздел 18 `architecture.md`) вычисляется на клиенте из
серверного `autoTransitionAt`. Как и в backend-требовании про инъекцию
`Clock` (`ADR-0018`), логика, зависящая от текущего времени, не должна
напрямую вызывать `Date.now()` внутри тестируемой функции — время должно
быть параметром (или явно замоканным через `vi.useFakeTimers()`), иначе
тест на "осталось 0 секунд" превращается в нестабильный (flaky) тест,
зависящий от реальной скорости выполнения.

---

## Definition of Done для новой feature

- [ ] Соответствующий backend-контракт существует в `04-api/endpoints.md`
      (готов реально или зафиксирован для MSW-мока с явной пометкой
      "ожидает backend").
- [ ] Unit-тесты — для нетривиальной логики вне компонентов (схемы,
      вычисления, утилиты).
- [ ] Component-тесты — happy path и хотя бы одно `error`-состояние на
      каждый компонент, выполняющий запрос к API.
- [ ] Соблюдена `7.1 Feature Boundaries` — feature не импортируется в обход
      её `index.ts`.
- [ ] Формы используют `react-hook-form` + `zod` (раздел `12.1`), ошибка
      backend-валидации отображается корректно, даже если клиентская схема
      её не поймала.
- [ ] Проверена доступность (раздел `23. Accessibility` `architecture.md`)
      — как минимум клавиатурная навигация и `aria-label` для icon-only
      controls.
- [ ] `roadmap/backlog.md` обновлён, если feature закрывает пункт оттуда.

---

## Открытые вопросы

- Порог покрытия для frontend (аналог `ADR-0017`, 70% для backend) — не
  зафиксирован. Решить, когда объём тестов вырастет настолько, что порог
  станет осмысленным ограничением, а не произвольной цифрой.

## Статус на сегодня (обновлено)

- **`vite.config.ts`** содержит `test`-блок (`environment: 'jsdom'`,
  `setupFiles: ['./src/test/setup.ts']`) — раньше отсутствовал вообще,
  `npm run test` падал бы немедленно (не было даже `jsdom`). Исправлено.
- **CI** (`.github/workflows/build.yml`) запускает `npm run test -- --run`
  без `--passWithNoTests` — флаг был нужен только на переходный период,
  пока тестов не было вообще; убран, как только появились первые.
- **MSW** подключён для тестов (`src/test/msw/`, `setupServer`, не
  `setupWorker` — тесты выполняются в Node/jsdom, не в браузере),
  `onUnhandledRequest: "error"` — тест провалится явно, если компонент
  дёрнёт эндпоинт без зарегистрированного handler'а.
- Написанные тесты покрывают unit-уровень (`jwt.ts`, `errors.ts`, `format.ts`)
  и component-уровень по одному наиболее сложному компоненту на каждую
  feature: `auth` (`LoginForm`, `RegisterForm`), `teams` (`TeamMembersList`
  — передача капитанства), `quests` (`RegistrationPanel` — вся матрица
  статусов регистрации), `authoring` (`QuestLifecycleActions` — publish/
  finish/delete, включая type-to-confirm), `gameplay` (`CodeSubmitForm` —
  все 4 исхода `CodeSubmissionResult`). Остальные компоненты каждой
  feature (формы Team — `CreateTeamForm`/`SearchTeamsForm`/
  `TeamManagementPanel`/`JoinRequestsPanel`; Author — `QuestForm`/
  `LevelsEditor`/`HintsPanel`/`CodesPanel`/`RegistrationReviewPanel`;
  Gameplay — `ShownHintsList`) — без тестов, следующий шаг в том же
  направлении, не новый вид работы.
- **`src/test/fixtures.ts`** — общие тестовые helper'ы (`fakeJwt`,
  `loginAs`) для компонентов, которым нужен залогиненный `useAuth()`, без
  реального похода в `/api/auth/login`.
- Побочная находка при написании тестов: `Quest.startTime`/`finishTime`
  (`api/quests.ts`) были типизированы как non-nullable `string`, хотя
  `QuestResponse.java` не гарантирует это (`@NotNull` нет), и наша же
  `QuestForm.tsx` осознанно отправляет `null` для незаполненных дат.
  `formatDateTime(null)` до фикса тихо показал бы 1 января 1970 года
  (`new Date(null)` не бросает исключение) — реальный, хоть и мелкий, баг,
  пойманный тестом, а не найденный вручную.
