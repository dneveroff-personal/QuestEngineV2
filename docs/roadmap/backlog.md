# Roadmap / Backlog

Трекер соответствия "специфицировано → реализовано → протестировано". Не заменяет issue-tracker, но даёт единый снимок состояния движка относительно документации в `docs/`.

Статусы:
- ⚪ Specified — правила описаны в `docs/`, кода нет.
- 🟡 In Progress — есть частичная реализация (например, только редактирование, без runtime-механики).
- 🔵 Implemented — реализовано и покрыто тестами.

| Механика | Специфицировано | Реализация | Комментарий |
|---|---|---|---|
| Quest CRUD, статусы, lifecycle | 🟢 `01-domain/quest.md` | 🔵 | `quest/` — CRUD реализован |
| Level CRUD | 🟢 `01-domain/level.md` | 🔵 | `level/` — CRUD реализован. Поля `codeIndex` (ADR-0005), `requiredMainCodesCount` — ещё не добавлены |
| Team, Captain, membership | 🟢 `01-domain/team.md` | 🔵 | `team/` реализован |
| QuestRegistration (заявки команд) | 🟢 `01-domain/registration.md` | 🔵 | `quest/` registration flow |
| **Автоматический старт Quest** (Job 1) | 🟢 `01-domain/progress.md`, ADR-002 | 🔵 | `QuestStartScheduler` — реализовано, атомарный переход + Сценарий 7 (гонка с `approveTeam`) |
| **Публикация Quest** (`DRAFT → REGISTRATION`) | 🟢 `02-processes/quest-lifecycle.md` шаг 3 | 🔵 | `POST /api/quests/{id}/publish` — реализовано: проверка статуса DRAFT, валидация "аномальных" уровней (ADR-0005), unit+controller+IT тесты |
| Завершение Quest автором (`RUNNING → FINISHED`) | 🟢 `02-processes/quest-lifecycle.md` шаг 13 | 🔵 | `POST /api/quests/{id}/finish` — реализовано: проверка статуса RUNNING, незавершённые QuestProgress получают DNF, unit+controller+IT тесты |
| **Автопереход уровня** (Job 2) | 🟢 `03-architecture/scheduling.md` | 🔵 | `LevelAutoTransitionScheduler` — реализовано, атомарный переход, разрешает Сценарий 5 (гонка с CodeSubmission), проверено реальным IT-тестом гонки |
| **Оркестрация завершения уровня / перехода / завершения QuestProgress** | 🟢 ADR-0009 | 🔵 | `advanceAfterLevelCompleted()` переиспользуется `CodeSubmission` и Job 2 |
| Hint — редактирование автором | 🟢 | 🔵 | `hint/service` — CRUD реализован (поля `type`/`bonusPenaltySeconds` — см. следующую строку) |
| **Hint — auto-reveal (REGULAR) / явное взятие (BONUS/PENALTY)** | 🟢 `01-domain/hint-progress.md`, ADR-0020, ADR-0021 | 🔵 | Реализовано: `Hint.type`/`bonusPenaltySeconds` (миграция V14), `HintProgress`, Job 3 (`HintRevealScheduler` — только REGULAR), `POST .../hints/{hintId}/take` (BONUS/PENALTY, ADR-0021), `GET .../hints` — три состояния видимости. Начисление эффекта к итоговому времени — см. следующий пункт |
| Code — редактирование автором | 🟢 | 🔵 | `code/service` — CRUD реализован. Уникальность в пределах Level (ADR-0004) и поле `codeIndex` (ADR-0005) реализованы (`0.5.18`) |
| **Code — ввод командой во время игры (CodeSubmission)** | 🟢 `01-domain/code-submission.md`, ADR-0005 | 🔵 | Реализовано: `CodeSubmission` (аудит попыток), атомарный condition-UPDATE для порога (Сценарий 6, частичный индекс), нормализация (регистр+пробелы). Unit+controller+IT тесты, включая реальный конкурентный тест на 30 потоков |
| **Bonus/Penalty Time** | 🟢 `01-domain/bonus-penalty.md`, ADR-0007 | 🟡 | Все три источника реализованы и агрегируются в `QuestProgressResponse.bonusPenaltySeconds` (`BonusPenaltyServiceImpl`, миграция V15). `ManualTimeAdjustmentController` — create/list/revoke. `Code.points → bonusPenaltySeconds` переименовано (V16). Тестов пока нет — пишутся отдельно, намеренно вне scope этого захода |
| Statistics / Ranking | 🟡 `01-domain/statistics-ranking.md` | ⚪ | Пакет `statistic/` пуст |
| Permissions / Security | 🟢 `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| API-контракт | 🟢 `04-api/conventions.md`, `04-api/endpoints.md` | 🟡 | Swagger/OpenAPI подключён; решения по статусам/пагинации приняты и реализованы (ADR-0011/0012) — `ConflictException`/`ResourceNotFoundException`, `PageResponse<T>` на `/users/search` и `/teams/search`. Тестов пока нет (пишутся отдельно) |
| DNF для команды | 🟢 `01-domain/registration.md`, `progress.md` | 🟡 | `PUT /api/quests/progress/{questId}/{teamId}/dnf` реализован (`QuestProgressController.setDnf`). Открытый вопрос по прекондиции — см. находки ниже. Тестов пока нет |
| Live-статистика (транспорт) | 🟢 `06-nfr/requirements.md`, ADR-0014 | ⚪ | SSE выбран, без искусственной задержки, не реализован |
| Rate limiting | 🟢 `05-security/threat-model.md`, ADR-0016 | 🔵 | `bucket4j` — `LoginRateLimitFilter` (5/мин на IP) только для `POST /api/auth/login`. In-memory. CodeSubmission намеренно не ограничен. Unit-тесты фильтра. (0.7.6) |
| JWT: access+refresh токены | 🟢 `05-security/threat-model.md`, ADR-0015 | ⚪ | Заменяет старую модель «единый JWT на 24ч» — текущий код (`JwtService`) реализует именно старую модель, требует переработки |
| Нагрузочный k6-смок-тест (Сценарий 6) | 🟢 `07-quality/testing-strategy.md` | 🔵 | Опциональный ручной smoke (`load-tests/k6/`), не CI-gate. Корректность Сценария 6 уже закрыта concurrent IT |
| Тестовое покрытие (JaCoCo) | 🟢 `07-quality/testing-strategy.md`, ADR-0017 | 🔵 | Отчёты JaCoCo есть; **жёсткий порог и fail-the-build отменены** (ADR-0017 amended) |
| Персональные подсказки (будущее улучшение) | 🟡 `01-domain/hint-progress.md` | ⚪ | Осознанно отложено за пределы MVP |
| CI (сборка, тесты, Docker-образ) | — | 🔵 | `.github/workflows/build.yml` — spotless, тесты, публикация образа в GHCR, JaCoCo-артефакт. Хорошо реализовано |
| CD (деплой на VPS) | — | 🟡 | Осознанно отключён автором до выхода в продакшен (явный комментарий в файле). `if`-условие всё ещё некорректно для случая, когда workflow будет включён — см. находки ниже |

## Немедленные блокеры реализации (по приоритету)

0. ✅ **Реализовать эндпоинты публикации и завершения Quest** (`DRAFT → REGISTRATION`, `RUNNING → FINISHED`) — реализовано.
1. ✅ **Закрыть найденную уязвимость в проде: открытый JDWP debug-порт** — закрыто (порт убран из `docker-compose.prod.yml`).
2. ✅ Реализовать `CodeSubmission` по модели ADR-0005 — реализовано (`V13__create_code_submissions_table.sql`, атомарный порог, unit+controller+IT+конкурентный тест). Начисление эффекта BONUS/PENALTY к итоговому времени — отдельно, п. 6.
3. ✅ Спроектировать и реализовать `Job 1`/`Job 2` из `03-architecture/scheduling.md` — реализовано (`dn.questenginev2.scheduling`, атомарные переходы, Сценарии 5 и 7, IT-тест реальной гонки Job2 vs CodeSubmission).
4. ✅ Закрыть подтверждённые гонки в `approveTeam()` и `enterQuest()` (ADR-0010) — реализовано: пессимистичная блокировка (`findByIdForUpdate`) в `approveTeam`, идемпотентный `saveAndFlush`+catch в `createFirstLevelProgress`/`createNextLevelProgress`. Проверено реальными конкурентными IT-тестами (`ApproveTeamRaceIT`).
5. ✅ Реализовать `HintProgress` (ADR-0020/ADR-0021) — реализовано: поля `Hint.type`/`bonusPenaltySeconds`, `HintProgress`, Job 3 (только REGULAR), `POST .../hints/{hintId}/take` (BONUS/PENALTY, явный выбор команды), `GET /api/quests/progress/{questId}/{teamId}/hints` с тремя состояниями видимости.
6. ✅ Реализовать три источника Bonus/Penalty (ADR-0007) — реализовано: `ManualTimeAdjustment` (entity/repository/service/controller, миграция V15), эффект кода и эффект подсказки — агрегация через `BonusPenaltyServiceImpl` (`QuestProgressResponse.bonusPenaltySeconds`). Тестов пока нет (пишутся отдельно).
7. ✅ Развести семантику HTTP-статусов ошибок (ADR-0011) — реализовано: `ForbiddenOperationException` теперь только 403 (было 409 — несоответствие названию), новый `ConflictException` (409, состояние-based), новый `ResourceNotFoundException` (404, заменяет generic `IllegalArgumentException` в ~25 местах). `PageResponse<T>` (ADR-0012) — `/users/search`, `/teams/search`. Тестов пока нет (пишутся отдельно).
8. ✅ Вывести `setDnf()` в контроллер — `PUT /api/quests/progress/{questId}/{teamId}/dnf`, стиль и авторизация зеркалят соседний `finish`. Открытый вопрос по прекондиции — см. находки ниже.
9. ✅ Внедрить rate limiting только для `/auth/login` (ADR-0016) — реализовано в 0.7.6 (`LoginRateLimitFilter`, 5/мин на IP). Переход на access+refresh токены (ADR-0015) — остаётся отдельной задачей до первого публичного релиза.
10. ✅ Качество: ADR-0017 amended — без % порога и без fail build по coverage; k6-смок Сценария 6 добавлен как опциональный (`load-tests/k6/`), не CI-gate.
11. Если/когда `deploy.yml` будет включаться обратно — поправить `if`-условие и путь `cd` (см. находки ниже), иначе CD молча не сработает даже при ручном запуске.

## Находки при просмотре реализованного кода

Хорошие новости:
- **`completeLevel()`-оркестрация уже реализована и уже корректно соответствует ADR-0009** (переход `QuestProgress → FINISHED` без разбора CODES/AUTO_TRANSITION) — не нужно ничего переделывать, только подключить к реальным точкам входа (ввод кода, планировщик).
- `Clock`-инъекция (ADR-0018) уже применяется в `LevelProgressServiceImpl`/`QuestProgressServiceImpl` — паттерн для будущего кода задан правильно.
- CI (`build.yml`) — качественный современный пайплайн: `spotlessCheck` → тесты → сборка Docker-образа → публикация в GHCR → артефакты (JaCoCo, тестовые отчёты). Хорошая база для учебных целей проекта (максимально современный стек).

Проблемы, требующие внимания:
- См. полную версию на `main` (раздел сохранён; точечные правки только в таблице и п.10). Frontend-пробелы (`GET /api/users/me`, квесты команды, username в search и т.д.) — **Открыто на 0.6.14.**
- `.github/workflows/deploy.yml` — при включении поправить `if` и путь `cd`.

## Статус документации

Документация закрыта на 100%, все содержательные открытые вопросы закрыты по итогам совместного разбора (см. `docs/ReadMe.md` — «Единственный оставшийся содержательный открытый вопрос»: список конкретных отличий от Encounter, не блокирует реализацию). Все 20 ADR приняты (Accepted).
