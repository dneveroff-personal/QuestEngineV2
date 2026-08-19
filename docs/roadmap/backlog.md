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
| **Автоматический старт Quest** (создание QuestProgress для APPROVED-регистраций в момент `Quest.startTime`) | 🟢 `01-domain/progress.md`, ADR-002 (требует правки) | ⚪ | Механизм (планировщик) не реализован — см. `03-architecture/scheduling.md` |
| **Публикация Quest** (`DRAFT → REGISTRATION`) | 🟢 `02-processes/quest-lifecycle.md` шаг 3 | ⚪ | **Блокер уровня "ничего не работает"** — нет ни одного эндпоинта для смены статуса Quest вообще. Обнаружено при написании `scheduling.md` |
| QuestProgress / LevelProgress runtime | 🟢 `01-domain/progress.md` | 🟡 | Есть `QuestProgressServiceImpl`, но полный runtime-цикл (первый вход команды → создание LevelProgress → автопереход) требует проверки на соответствие доку |
| Hint — редактирование автором | 🟢 | 🔵 | `hint/service` — CRUD реализован |
| **Hint — открытие командой во время игры (HintProgress)** | 🟡 `01-domain/hint-progress.md` | ⚪ | Не начато. Блокируется открытыми вопросами (auto vs manual reveal) |
| Code — редактирование автором | 🟢 | 🟡 | `code/service` — CRUD реализован, но с проблемой глобальной уникальности (см. `code-submission.md`) |
| **Code — ввод командой во время игры (CodeSubmission)** | 🟡 `01-domain/code-submission.md` | ⚪ | Не начато. Блокируется открытыми вопросами (критерий завершения уровня, rate limiting) |
| **Bonus/Penalty Time** | 🟡 `01-domain/bonus-penalty.md` | ⚪ | Не начато. Ни ручного начисления автором, ни эффекта от `CodeType.BONUS/PENALTY` — не реализовано |
| Statistics / Ranking | 🟡 `01-domain/statistics-ranking.md` | ⚪ | Пакет `statistic/` пуст. Live ranking для первого уровня не формализован |
| Permissions / Security | 🟢 `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| API-контракт | 🟡 `04-api/conventions.md`, `04-api/endpoints.md` | 🟡 | Swagger/OpenAPI подключён, но найдены пробелы: 2 отсутствующих эндпоинта (publish/finish Quest), неполная пагинация, несоответствие HTTP-статусов ошибок |
| Завершение Quest автором (`RUNNING → FINISHED`) | 🟢 `02-processes/quest-lifecycle.md` шаг 13 | ⚪ | Нет эндпоинта. Обнаружено при написании `04-api/endpoints.md` |
| DNF для команды | 🟢 `01-domain/registration.md`, `progress.md` | 🟡 | Метод `setDnf()` реализован в сервисе, но не выведен ни в один контроллер |
| Live-статистика (транспорт) | 🟡 `06-nfr/requirements.md` | ⚪ | Ни один транспорт не выбран и не реализован (рекомендация — SSE) |
| Rate limiting | 🟡 `05-security/threat-model.md` | ⚪ | Ни одной библиотеки для этого в проекте нет — уязвим как минимум `/auth/login` |
| JWT revocation | 🟡 `05-security/threat-model.md` | ⚪ | Отзыв токена раньше истечения (24ч) невозможен технически — открытый вопрос, критичен ли сценарий |
| Тестовое покрытие CI-порогом | 🟡 `07-quality/testing-strategy.md` | ⚪ | JaCoCo подключён, отчёт генерируется, но сборку не проваливает |

## Немедленные блокеры реализации (по приоритету)

0. **Реализовать эндпоинты публикации и завершения Quest** (`DRAFT → REGISTRATION`, `RUNNING → FINISHED`) — без первого весь остальной flow недостижим даже вручную.
1. Принять ADR по открытым вопросам из `code-submission.md` (уникальность кода, критерий завершения уровня) — без этого нельзя проектировать `CodeSubmission`.
2. Спроектировать и реализовать `Job 1`/`Job 2` из `03-architecture/scheduling.md` — без механизма автостарта QuestProgress не создаются вообще, это блокирует весь runtime.
3. Закрыть подтверждённые гонки в `approveTeam()` и `enterQuest()` (`02-processes/concurrency-scenarios.md`, Сценарии 1–2) — это не гипотетические, а прочтением кода подтверждённые уязвимости, исправление не требует новой функциональности, только защиты существующей.
4. Решить формат `HintProgress` (auto vs manual reveal).
5. Развести семантику HTTP-статусов ошибок (`403`/`404`/`409`, см. `04-api/conventions.md`) — не блокирует MVP функционально, но усложнит клиентский код тем сильнее, чем позже будет исправлено.
6. Вывести `setDnf()` в контроллер, добавить пагинацию с метаданными (`Page<T>`) в listing-эндпоинты.
7. Внедрить rate limiting как минимум для `/auth/login` до первого публичного релиза (`05-security/threat-model.md`).
8. Настроить `jacocoTestCoverageVerification` с согласованным порогом, чтобы регресс покрытия проваливал сборку.

## Статус документации

Первый полный проход документации завершён — все разделы `docs/` содержат содержательный контент, ни одного файла в статусе ⚪ TBD не осталось (полная сводка статусов — `docs/ReadMe.md`). Следующий шаг: оформить перечисленные там 16 пунктов "Решения, требующие ADR" как фактические ADR-документы в `03-architecture/adr/`, и только затем переходить к архитектурному проектированию и реализации.
