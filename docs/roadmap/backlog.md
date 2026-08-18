# Roadmap / Backlog

Трекер соответствия "специфицировано → реализовано → протестировано". Не заменяет issue-tracker, но даёт единый снимок состояния движка относительно документации в `docs/`.

Статусы:
- ⚪ Specified — правила описаны в `docs/`, кода нет.
- 🟡 In Progress — есть частичная реализация (например, только редактирование, без runtime-механики).
- 🔵 Implemented — реализовано и покрыто тестами.

| Механика | Специфицировано | Реализация | Комментарий |
|---|---|---|---|
| Quest CRUD, статусы, lifecycle | 🟢 `01-domain/quest.md` | 🔵 | `quest/` — CRUD, переходы статусов реализованы |
| Level CRUD | 🟢 `01-domain/level.md` | 🔵 | `level/` — CRUD реализован |
| Team, Captain, membership | 🟢 `01-domain/team.md` | 🔵 | `team/` реализован |
| QuestRegistration (заявки команд) | 🟢 `01-domain/registration.md` | 🔵 | `quest/` registration flow |
| **Автоматический старт Quest** (создание QuestProgress для APPROVED-регистраций в момент `Quest.startTime`) | 🟢 `01-domain/progress.md`, ADR-002 | ⚪ | Механизм (планировщик) не реализован — см. `03-architecture/scheduling.md` (TBD) |
| QuestProgress / LevelProgress runtime | 🟢 `01-domain/progress.md` | 🟡 | Есть `QuestProgressServiceImpl`, но полный runtime-цикл (первый вход команды → создание LevelProgress → автопереход) требует проверки на соответствие доку |
| Hint — редактирование автором | 🟢 | 🔵 | `hint/service` — CRUD реализован |
| **Hint — открытие командой во время игры (HintProgress)** | 🟡 `01-domain/hint-progress.md` | ⚪ | Не начато. Блокируется открытыми вопросами (auto vs manual reveal) |
| Code — редактирование автором | 🟢 | 🟡 | `code/service` — CRUD реализован, но с проблемой глобальной уникальности (см. `code-submission.md`) |
| **Code — ввод командой во время игры (CodeSubmission)** | 🟡 `01-domain/code-submission.md` | ⚪ | Не начато. Блокируется открытыми вопросами (критерий завершения уровня, rate limiting) |
| **Bonus/Penalty Time** | 🟡 `01-domain/bonus-penalty.md` | ⚪ | Не начато. Ни ручного начисления автором, ни эффекта от `CodeType.BONUS/PENALTY` — не реализовано |
| Statistics / Ranking | 🟡 `01-domain/statistics-ranking.md` | ⚪ | Пакет `statistic/` пуст. Live ranking для первого уровня не формализован |
| Permissions / Security | 🟢 `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| API-контракт | ⚪ `04-api/` (TBD) | — | Есть Swagger-конфиг, но контракт не задокументирован отдельно |

## Немедленные блокеры реализации (по приоритету)

1. Принять ADR по открытым вопросам из `code-submission.md` (уникальность кода, критерий завершения уровня) — без этого нельзя проектировать `CodeSubmission`.
2. Спроектировать `03-architecture/scheduling.md` — без механизма автостарта QuestProgress не создаются вообще, это блокирует весь runtime.
3. Решить формат `HintProgress` (auto vs manual reveal).
