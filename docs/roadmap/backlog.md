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
| Hint — редактирование автором | 🟢 | 🔵 | `hint/service` — CRUD реализован. Поля `type`, `bonusPenaltySeconds` (ADR-0020) — не добавлены |
| **Hint — auto-reveal показ во время игры (HintProgress)** | 🟢 `01-domain/hint-progress.md`, ADR-0020 | ⚪ | Не начато |
| Code — редактирование автором | 🟢 | 🔵 | `code/service` — CRUD реализован. Уникальность в пределах Level (ADR-0004) и поле `codeIndex` (ADR-0005) реализованы (`0.5.18`) |
| **Code — ввод командой во время игры (CodeSubmission)** | 🟢 `01-domain/code-submission.md`, ADR-0005 | 🔵 | Реализовано: `CodeSubmission` (аудит попыток), атомарный condition-UPDATE для порога (Сценарий 6, частичный индекс), нормализация (регистр+пробелы), BONUS/PENALTY фиксируются в аудите (эффект — отдельная фича, п. 6). Unit+controller+IT тесты, включая реальный конкурентный тест на 30 потоков |
| **Bonus/Penalty Time** | 🟢 `01-domain/bonus-penalty.md`, ADR-0007 | ⚪ | Ни один из трёх источников (manual/code/hint) не реализован |
| Statistics / Ranking | 🟡 `01-domain/statistics-ranking.md` | ⚪ | Пакет `statistic/` пуст |
| Permissions / Security | 🟢 `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| API-контракт | 🟢 `04-api/conventions.md`, `04-api/endpoints.md` | 🟡 | Swagger/OpenAPI подключён; решения по статусам/пагинации приняты (ADR-0011/0012), не реализованы |
| DNF для команды | 🟢 `01-domain/registration.md`, `progress.md` | 🟡 | `setDnf()` реализован в сервисе, не выведен в контроллер |
| Live-статистика (транспорт) | 🟢 `06-nfr/requirements.md`, ADR-0014 | ⚪ | SSE выбран, без искусственной задержки, не реализован |
| Rate limiting | 🟢 `05-security/threat-model.md`, ADR-0016 | ⚪ | `bucket4j` выбран, только для `/auth/login` (5/мин на IP). Явно НЕ для ввода кода — см. ADR-0016 |
| JWT: access+refresh токены | 🟢 `05-security/threat-model.md`, ADR-0015 | ⚪ | Заменяет старую модель «единый JWT на 24ч» — текущий код (`JwtService`) реализует именно старую модель, требует переработки |
| Нагрузочный k6-смок-тест (Сценарий 6) | 🟢 `07-quality/testing-strategy.md` | ⚪ | Проверка атомарного UPDATE при подсчёте порога кодов под параллельной нагрузкой |
| Тестовое покрытие CI-порогом | 🟢 `07-quality/testing-strategy.md`, ADR-0017 | ⚪ | Порог (70%) выбран, `jacocoTestCoverageVerification` не добавлена в build |
| Персональные подсказки (будущее улучшение) | 🟡 `01-domain/hint-progress.md` | ⚪ | Осознанно отложено за пределы MVP |
| CI (сборка, тесты, Docker-образ) | — | 🔵 | `.github/workflows/build.yml` — spotless, тесты, публикация образа в GHCR, JaCoCo-артефакт. Хорошо реализовано |
| CD (деплой на VPS) | — | 🟡 | Осознанно отключён автором до выхода в продакшен (явный комментарий в файле). `if`-условие всё ещё некорректно для случая, когда workflow будет включён — см. находки ниже |
| **Frontend deployment topology** | 🟢 `08-ops/deployment.md` | 🔵 | nginx (frontend-образ) как единственная публичная точка входа, `app` без публичного порта в проде, same-origin без CORS и в dev (Vite proxy), и в проде. CI-job для frontend с path-фильтром (`dorny/paths-filter`) |

## Немедленные блокеры реализации (по приоритету)

0. ✅ **Реализовать эндпоинты публикации и завершения Quest** (`DRAFT → REGISTRATION`, `RUNNING → FINISHED`) — реализовано.
1. ✅ **Закрыть найденную уязвимость в проде: открытый JDWP debug-порт** — закрыто (порт убран из `docker-compose.prod.yml`).
2. ✅ Реализовать `CodeSubmission` по модели ADR-0005 — реализовано (`V13__create_code_submissions_table.sql`, атомарный порог, unit+controller+IT+конкурентный тест). Начисление эффекта BONUS/PENALTY к итоговому времени — отдельно, п. 6.
3. ✅ Спроектировать и реализовать `Job 1`/`Job 2` из `03-architecture/scheduling.md` — реализовано (`dn.questenginev2.scheduling`, атомарные переходы, Сценарии 5 и 7, IT-тест реальной гонки Job2 vs CodeSubmission).
4. ✅ Закрыть подтверждённые гонки в `approveTeam()` и `enterQuest()` (ADR-0010) — реализовано: пессимистичная блокировка (`findByIdForUpdate`) в `approveTeam`, идемпотентный `saveAndFlush`+catch в `createFirstLevelProgress`/`createNextLevelProgress`. Проверено реальными конкурентными IT-тестами (`ApproveTeamRaceIT`).
5. Реализовать `HintProgress` (auto-reveal, ADR-0020) — включая добавление полей `Hint.type`/`bonusPenaltySeconds`.
6. Реализовать три источника Bonus/Penalty (ADR-0007) — `ManualTimeAdjustment`, эффект кода, эффект подсказки.
7. Развести семантику HTTP-статусов ошибок (ADR-0011), добавить `PageResponse<T>` (ADR-0012).
8. Вывести `setDnf()` в контроллер.
9. Внедрить rate limiting только для `/auth/login` (ADR-0016) и перейти на access+refresh токены (ADR-0015) до первого публичного релиза.
10. Настроить `jacocoTestCoverageVerification` (ADR-0017) и k6-смок-тест на Сценарий 6.
11. Если/когда `deploy.yml` будет включаться обратно — поправить `if`-условие и путь `cd` (см. находки ниже), иначе CD молча не сработает даже при ручном запуске.

## Находки при просмотре реализованного кода (0.5.14)

Хорошие новости:
- **`completeLevel()`-оркестрация уже реализована и уже корректно соответствует ADR-0009** (переход `QuestProgress → FINISHED` без разбора CODES/AUTO_TRANSITION) — не нужно ничего переделывать, только подключить к реальным точкам входа (ввод кода, планировщик).
- `Clock`-инъекция (ADR-0018) уже применяется в `LevelProgressServiceImpl`/`QuestProgressServiceImpl` — паттерн для будущего кода задан правильно.
- CI (`build.yml`) — качественный современный пайплайн: `spotlessCheck` → тесты → сборка Docker-образа → публикация в GHCR → артефакты (JaCoCo, тестовые отчёты). Хорошая база для учебных целей проекта (максимально современный стек).

Проблемы, требующие внимания:
- **🔴 JDWP debug-порт (5004) в `docker-compose.prod.yml`** — **✅ Исправлено в `0.5.18`**: проброс порта наружу убран. Сам debug-агент в `Dockerfile` пока остаётся общим для local/prod (порт просто не пробрасывается наружу через compose) — разделение Dockerfile на local/prod варианты остаётся желательным улучшением, но не критичным, раз порт больше не достижим извне.
- `docker-compose.prod.yml` пробрасывает наружу порт PostgreSQL (`5432`) — обычно не нужно в проде, если приложение обращается к БД через внутреннюю docker-сеть; лишняя поверхность атаки.
- `.github/workflows/deploy.yml` **осознанно отключён** автором до выхода в продакшен (`# ВЫКЛЮЧЕН ДО МОМЕНТА ВЫХОДА В ПРОДАКШЕН`, `workflow_run`-триггер закомментирован, оставлен только ручной `workflow_dispatch`) — правильное решение на этом этапе. **При последующем включении** не забыть также поправить: условие `if: ${{ github.event.workflow_run.conclusion == 'success' }}` всегда ложно при ручном запуске (`github.event.workflow_run` не существует для `workflow_dispatch`) — если просто раскомментировать `workflow_run`-триггер, ручной запуск через `workflow_dispatch` продолжит молча пропускать деплой. Строка `cd ~/ts-wc-scores` (путь от другого проекта) закомментирована, но её отступ не совпадал с отступом блока `script: |` — YAML literal block scalar обрывался на этой строке раньше времени, что сломало бы весь workflow при первом же запуске (не только "не туда cd", а вообще невалидный YAML). **Исправлено** (отступ выровнен) при добавлении `08-ops/deployment.md`. Путь `cd` по-прежнему нужно заменить на актуальный при включении — сам факт, что путь не актуален, не исправлен, исправлена только YAML-валидность строки.
- `LevelProgressServiceImpl.autoTransitionLevel()` помечен `@Deprecated` — небезопасен для конкурентного вызова (Сценарий 5), планировщик использует новый атомарный `LevelProgressRepository.tryAutoTransition` напрямую. Метод оставлен для обратной совместимости существующих тестов.
- **Отсутствует `GET /api/users/me`** — найдено при подготовке `docs/frontend/roadmap.md`. `UserController` реализует `PUT .../role`, `POST .../reset-password`, `GET /users/search`, но нет эндпоинта "получить свой профиль". `UserResponse` уже содержит нужные поля (`id`, `publicName`, `email`, `role`, `createdAt`) — не хватает только метода, отдающего его для текущего аутентифицированного пользователя (`Authentication` уже доступен в других методах контроллера как параметр — паттерн есть, добавить некуда сложно). Блокирует на frontend: полноценный экран Profile и Role-based UI (`architecture.md` §14) — frontend физически не может узнать роль собственного пользователя.
- **Отсутствует обратный эндпоинт "квесты моей команды"** — найдено при реализации `MyQuestsPage.tsx` (frontend). Есть только `GET /api/quests/register/{questId}` (по квесту → список команд), нет `GET /api/teams/{teamId}/quests` или аналога (по команде → список её регистраций/квестов). Frontend сейчас обходит это N+1 запросами по всем `upcoming`-квестам (см. `docs/frontend/roadmap.md` §4.4) — работает, но не масштабируется и не показывает прошедшие (`FINISHED`) квесты вообще. Добавить нормальный эндпоинт — например, `QuestRegistrationRepository.findAllByTeamId(teamId)` уже почти наверняка возможен на уровне репозитория (по аналогии с `findAllByQuestId`, который уже используется в `getRegisteredTeams`), не хватает только контроллера/сервисного метода поверх него.
- **`TeamMemberDto.id` — id записи `TeamMember`, не `User.id`** — найдено при реализации "передать капитанство" на frontend (`TeamMembersList.tsx`). `POST /api/teams/transfer-captain/{userId}` ожидает `User.id`, а единственный список участников команды (`GET /api/teams/{teamId}/members`, вложенный в `TeamResponse.members`) отдаёт `TeamMemberDto.id = m.getId()` (id `TeamMember`, `TeamServiceImpl.teamMemberstoDto`). Frontend вынужден резолвить `User.id` окольным путём через `GET /api/users/search?username=...` (см. следующий пункт про его собственные проблемы) — это лишний запрос и источник хрупкости там, где не должно было быть никакой хрупкости. Простое решение — добавить `userId` в `TeamMemberDto` рядом с существующим `id`.
- **`TeamMemberDto.name` / `TeamResponse.captainName` — это `username`, не `publicName`** — тоже найдено там же. `TeamServiceImpl.teamMemberstoDto`/`buildTeamResponse` используют `User.getUsername()`, а не `getPublicName()`, при том что вся остальная система (Auth `LoginResponse.publicName`, вероятно и другие экраны) ориентируется на `publicName` как на "отображаемое имя". Не обязательно баг — возможно, осознанный выбор (username стабильнее publicName для этой формы), но стоит явно решить и задокументировать, а не оставлять как непреднамеренное расхождение: сейчас Team — единственное место в системе, где напрямую виден username, и frontend вынужден заново декодировать JWT (`sub`-claim), чтобы вообще получить свой username и сравнить его с этим полем (LoginResponse его не отдаёт).
- **`GET /api/users/search` не возвращает `username`, при этом ищет именно по нему (`LIKE %username%`, частичное совпадение)** — `UserResponse` содержит `publicName`, но не `username`, поэтому при неоднозначном результате поиска (несколько пользователей с похожим username) frontend не может понять, какой из результатов реально соответствует искомому username. Для действия "передать капитанство" (см. выше) это критично — molча взять первый результат означало бы риск передать капитанство не тому человеку. Frontend сейчас честно отказывается действовать при 0 или 2+ результатах вместо угадывания. Добавить `username` в `UserResponse` решит проблему полностью.
- **`GET /api/users/search` не проверяет роль вызывающего** — в отличие от `setUserRole`/`resetPassword` в том же контроллере, которые вызывают `validateAdmin` внутри сервиса, `searchUsers` не проверяет ничего — любой аутентифицированный пользователь может искать по `username`/`email`/`role`/дате регистрации любого другого пользователя и увидеть его email и роль. Не критично для pet-проекта на этом этапе, но стоит решить осознанно (например, ограничить поле `email` в ответе для не-ADMIN, либо явно принять как задуманное поведение и написать это в `05-security/threat-model.md`), а не оставлять как непреднамеренную дыру.

## Статус документации

Документация закрыта на 100%, все содержательные открытые вопросы закрыты по итогам совместного разбора (см. `docs/ReadMe.md` — «Единственный оставшийся содержательный открытый вопрос»: список конкретных отличий от Encounter, не блокирует реализацию). Все 19 ADR приняты (Accepted).
