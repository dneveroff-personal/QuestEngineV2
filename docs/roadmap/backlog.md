# Roadmap / Backlog

Трекер соответствия "специфицировано → реализовано → протестировано". Не заменяет issue-tracker, но даёт единый снимок состояния движка относительно документации в `docs/`.

Статусы:
- ⚪ Specified — правила описаны в `docs/`, кода нет.
- 🟡 In Progress — есть частичная реализация (например, только редактирование, без runtime-механики).
- 🔵 Implemented — реализовано и покрыто тестами.

| Механика | Специфицировано | Реализация | Комментарий |
|---|---|---|---|
| Quest CRUD, статусы, lifecycle | 🟢 `01-domain/quest.md` | 🔵 | `quest/` — CRUD реализован |
| Level CRUD | 🟢 `01-domain/level.md` | 🔵 | `level/` — CRUD реализован. Поля `groupIndex` (ADR-0005), `requiredCodesCount` — ещё не добавлены |
| Team, Captain, membership | 🟢 `01-domain/team.md` | 🔵 | `team/` реализован |
| QuestRegistration (заявки команд) | 🟢 `01-domain/registration.md` | 🔵 | `quest/` registration flow |
| **Автоматический старт Quest** (Job 1) | 🟢 `01-domain/progress.md`, ADR-002 | ⚪ | Планировщик не реализован — см. `03-architecture/scheduling.md` |
| **Публикация Quest** (`DRAFT → REGISTRATION`) | 🟢 `02-processes/quest-lifecycle.md` шаг 3 | ⚪ | **Блокер уровня "ничего не работает"** — нет эндпоинта для смены статуса Quest |
| Завершение Quest автором (`RUNNING → FINISHED`) | 🟢 `02-processes/quest-lifecycle.md` шаг 13 | ⚪ | Нет эндпоинта |
| **Оркестрация завершения уровня / перехода / завершения QuestProgress** | 🟢 ADR-0009 | 🟡 | `LevelProgressServiceImpl.completeLevel()` + `QuestProgressServiceImpl.completeLevel()` уже реализованы и **уже корректно следуют ADR-0009** (без разбора между CODES/AUTO_TRANSITION). Не вызывается ни из контроллера, ни из планировщика — только напрямую из теста |
| `autoTransitionLevel()` (Job 2 building block) | 🟡 `03-architecture/scheduling.md` | 🟡 | Метод реализован в `LevelProgressServiceImpl`, но нигде не вызывается (нет планировщика) |
| Hint — редактирование автором | 🟢 | 🔵 | `hint/service` — CRUD реализован. Поля `type`, `bonusPenaltySeconds` (ADR-0020) — не добавлены |
| **Hint — auto-reveal показ во время игры (HintProgress)** | 🟢 `01-domain/hint-progress.md`, ADR-0020 | ⚪ | Не начато |
| Code — редактирование автором | 🟢 | 🟡 | `code/service` — CRUD реализован, уникальность всё ещё глобальная (ADR-0004 принят, не реализован) |
| **Code — ввод командой во время игры (CodeSubmission)** | 🟢 `01-domain/code-submission.md`, ADR-0005 | ⚪ | Не начато. Модель решена, можно проектировать |
| **Bonus/Penalty Time** | 🟢 `01-domain/bonus-penalty.md`, ADR-0007 | ⚪ | Ни один из трёх источников (manual/code/hint) не реализован |
| Statistics / Ranking | 🟡 `01-domain/statistics-ranking.md` | ⚪ | Пакет `statistic/` пуст |
| Permissions / Security | 🟢 `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| API-контракт | 🟢 `04-api/conventions.md`, `04-api/endpoints.md` | 🟡 | Swagger/OpenAPI подключён; решения по статусам/пагинации приняты (ADR-0011/0012), не реализованы |
| DNF для команды | 🟢 `01-domain/registration.md`, `progress.md` | 🟡 | `setDnf()` реализован в сервисе, не выведен в контроллер |
| Live-статистика (транспорт) | 🟢 `06-nfr/requirements.md`, ADR-0014 | ⚪ | SSE выбран, не реализован |
| Rate limiting | 🟢 `05-security/threat-model.md`, ADR-0016 | ⚪ | `bucket4j` выбран, не реализован |
| JWT revocation | 🟢 `05-security/threat-model.md`, ADR-0015 | ⚪ | Denylist выбран, не реализован |
| Тестовое покрытие CI-порогом | 🟢 `07-quality/testing-strategy.md`, ADR-0017 | ⚪ | Порог (70%) выбран, `jacocoTestCoverageVerification` не добавлена в build |
| CI (сборка, тесты, Docker-образ) | — | 🔵 | `.github/workflows/build.yml` — spotless, тесты, публикация образа в GHCR, JaCoCo-артефакт. Хорошо реализовано |
| CD (деплой на VPS) | — | 🔴 | `.github/workflows/deploy.yml` **не может сработать в текущем виде** — см. находки ниже |

## Немедленные блокеры реализации (по приоритету)

0. **Реализовать эндпоинты публикации и завершения Quest** (`DRAFT → REGISTRATION`, `RUNNING → FINISHED`) — без первого весь остальной flow недостижим даже вручную.
1. **Закрыть найденную уязвимость в проде: открытый JDWP debug-порт** (см. находки ниже) — критичнее любого функционального блокера, если проект уже выставлен в интернет.
2. Реализовать `CodeSubmission` по модели ADR-0005 (main-коды, синонимы, порог) — модель полностью решена, можно приступать к схеме БД и коду.
3. Спроектировать и реализовать `Job 1`/`Job 2` из `03-architecture/scheduling.md`, подключив уже готовую оркестрацию `completeLevel()`.
4. Закрыть подтверждённые гонки в `approveTeam()` и `enterQuest()` (ADR-0010).
5. Реализовать `HintProgress` (auto-reveal, ADR-0020) — включая добавление полей `Hint.type`/`bonusPenaltySeconds`.
6. Реализовать три источника Bonus/Penalty (ADR-0007) — `ManualTimeAdjustment`, эффект кода, эффект подсказки.
7. Развести семантику HTTP-статусов ошибок (ADR-0011), добавить `PageResponse<T>` (ADR-0012).
8. Вывести `setDnf()` в контроллер.
9. Внедрить rate limiting (ADR-0016) и JWT denylist (ADR-0015) до первого публичного релиза.
10. Настроить `jacocoTestCoverageVerification` (ADR-0017).
11. Починить `deploy.yml` (см. находки ниже) — иначе CD не работает даже после включения триггера.

## Находки при просмотре реализованного кода (0.5.14)

Хорошие новости:
- **`completeLevel()`-оркестрация уже реализована и уже корректно соответствует ADR-0009** (переход `QuestProgress → FINISHED` без разбора CODES/AUTO_TRANSITION) — не нужно ничего переделывать, только подключить к реальным точкам входа (ввод кода, планировщик).
- `Clock`-инъекция (ADR-0018) уже применяется в `LevelProgressServiceImpl`/`QuestProgressServiceImpl` — паттерн для будущего кода задан правильно.
- CI (`build.yml`) — качественный современный пайплайн: `spotlessCheck` → тесты → сборка Docker-образа → публикация в GHCR → артефакты (JaCoCo, тестовые отчёты). Хорошая база для учебных целей проекта (максимально современный стек).

Проблемы, требующие внимания:
- **🔴 Критично: JDWP debug-порт (5004) открыт в `docker-compose.prod.yml`** и зашит в единый `Dockerfile`, используемый и для локальной разработки, и для продакшен-образа (`-agentlib:jdwp=...address=*:5004`, без аутентификации). Удалённый debug-агент Java без аутентификации — известный вектор удалённого выполнения кода. Нужно разделить Dockerfile (или использовать multi-stage/профиль сборки) так, чтобы продакшен-образ не содержал debug-агент вообще, либо как минимум не пробрасывать порт 5004 наружу в `docker-compose.prod.yml`.
- `docker-compose.prod.yml` пробрасывает наружу порт PostgreSQL (`5432`) — обычно не нужно в проде, если приложение обращается к БД через внутреннюю docker-сеть; лишняя поверхность атаки.
- `.github/workflows/deploy.yml` в текущем виде **не может сработать**: автоматический триггер (`workflow_run`) закомментирован, оставлен только `workflow_dispatch` (ручной запуск), но условие `if: ${{ github.event.workflow_run.conclusion == 'success' }}` относится к `workflow_run`-триггеру и всегда ложно при ручном запуске — то есть job `deploy` будет пропущен даже при ручном вызове workflow. Также команда `cd ~/ts-wc-scores` — явно скопирована из другого проекта, не существующая директория для QuestEngineV2.
- `LevelProgressServiceImpl.autoTransitionLevel()` реализован, но нигде не вызывается — заготовка под Job 2 планировщика, которую можно переиспользовать при реализации `scheduling.md`.

## Статус документации

Документация закрыта на 100%. Все 19 ADR приняты (Accepted) — реестр в `docs/ReadMe.md`. Открытых архитектурных развилок, требующих решения продукта, не осталось; оставшиеся открытые вопросы внутри документов — детали реализации (конкретные числа, не развилки направления).
