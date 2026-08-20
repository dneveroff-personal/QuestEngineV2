# QuestEngine — Документация

Это техническое задание и спецификация движка. Порядок в проекте: **сначала документация → потом архитектура → потом код**.

Учебный проект: цель — освоить максимально современный Java-стек так, как это делается в индустрии (проектирование, разработка, деплой, поддержка), двигаясь последовательно — от модульного монолита к микросервисам, когда проект дорастёт до нужной точки. Итоговый критерий готовности — рабочий движок, развёрнутый в интернете, на котором можно провести реальный квест. Подробнее — `00-vision/product-vision.md`.

Документы читаются по возрастанию номера папки — от бизнес-смысла к реализации.
Каждый документ имеет статус:

- 🟢 **Accepted** — согласовано, можно проектировать/реализовывать поверх этого.
- 🟡 **Draft** — основа есть, но требует уточнений (см. "Открытые вопросы" внутри файла, если есть).
- ⚪ **TBD** — раздел выделен, содержание ещё не написано.
- 🔵 **Implemented** — помимо описания, уже реализовано в коде и покрыто тестами.

---

## 00. Vision — что мы строим и зачем

| Документ | Статус |
|---|---|
| [glossary.md](00-vision/glossary.md) — словарь терминов (Ubiquitous Language) | 🟢 |
| [product-vision.md](00-vision/product-vision.md) — продукт, аналогия с Encounter, учебные цели | 🟡 *(2 открытых вопроса — точные отличия от Encounter, целевая аудитория; формат игры решён — ADR-0019)* |

## 01. Domain — предметная область ("что")

Статические сущности (шаблон игры) и Runtime-сущности (прохождение игры).

| Документ | Статус |
|---|---|
| [quest.md](01-domain/quest.md) | 🟢 |
| [level.md](01-domain/level.md) | 🟢 |
| [team.md](01-domain/team.md) | 🟢 |
| [registration.md](01-domain/registration.md) | 🟢 |
| [progress.md](01-domain/progress.md) — QuestProgress / LevelProgress | 🟢 |
| [runtime.md](01-domain/runtime.md) — сводная runtime-модель | 🟡 *(частично дублирует progress.md — требует слияния, известный техдолг)* |
| [domain-events.md](01-domain/domain-events.md) | 🟢 |
| [invariants.md](01-domain/invariants.md) | 🟢 |
| [statistics-ranking.md](01-domain/statistics-ranking.md) | 🟡 *(ranking для первого уровня не формализован)* |
| [code-submission.md](01-domain/code-submission.md) — main-коды, синонимы, порог, попытки, брутфорс | 🟢 *(модель завершения уровня решена ADR-0005; остаются 3 мелких открытых вопроса реализации)* |
| [hint-progress.md](01-domain/hint-progress.md) — auto-reveal, типы Regular/Bonus/Penalty | 🟢 *(модель решена ADR-0020; остаётся 1 открытый вопрос — ручное досрочное открытие автором)* |
| [bonus-penalty.md](01-domain/bonus-penalty.md) — три источника бонус/штрафа, аудит | 🟢 *(модель агрегации решена ADR-0007, дополнена ADR-0020)* |

## 02. Processes — бизнес-процессы ("как", без привязки к транспорту)

| Документ | Статус |
|---|---|
| [quest-lifecycle.md](02-processes/quest-lifecycle.md) | 🟢 |
| [sequence-diagrams.md](02-processes/sequence-diagrams.md) | 🟢 |
| [concurrency-scenarios.md](02-processes/concurrency-scenarios.md) — гонки: лимит команд, параллельный автостарт | 🟡 *(5 сценариев; фиксы для 2 подтверждённых уязвимостей описаны ADR-0010)* |

## 03. Architecture — технические решения

| Документ | Статус |
|---|---|
| [domain-model.md](03-architecture/domain-model.md) — ER-диаграмма, слои модели | 🟢 |
| [state-machines.md](03-architecture/state-machines.md) | 🟢 |
| [scheduling.md](03-architecture/scheduling.md) — механизм автостарта Quest / автоперехода уровней | 🟡 *(модель решена всеми зависимыми ADR; сам механизм ещё не реализован в коде)* |
| [adr/](03-architecture/adr/) — architecture decision records | 🟢 *(19 ADR, все Accepted — см. реестр ниже)* |

## 04. API — контракт

| Документ | Статус |
|---|---|
| [conventions.md](04-api/conventions.md) — формат ошибок, аутентификация, пагинация, версионирование | 🟢 *(решения приняты ADR-0011/0012, реализация ещё не выполнена)* |
| [endpoints.md](04-api/endpoints.md) — карта ресурсов, статус по каждому | 🟡 *(4 отсутствующих эндпоинта найдены, включая блокер публикации Quest — см. `roadmap/backlog.md`)* |

## 05. Security

| Документ | Статус |
|---|---|
| [permissions.md](05-security/permissions.md) | 🟢 |
| [threat-model.md](05-security/threat-model.md) — rate limiting, отзыв JWT, CORS, IDOR | 🟢 *(решения приняты ADR-0015/0016, реализация ещё не выполнена)* |

## 06. NFR — нефункциональные требования

| Документ | Статус |
|---|---|
| [requirements.md](06-nfr/requirements.md) — нагрузка, транспорт статистики, часовые пояса, локализация | 🟡 *(транспорт и часовые пояса решены — ADR-0014/0013; нагрузка и локализация — открытые вопросы к продукту)* |

## 07. Quality — тестирование и Definition of Done

| Документ | Статус |
|---|---|
| [testing-strategy.md](07-quality/testing-strategy.md) — паттерн тестов, DoD, найденные пробелы покрытия | 🟢 *(порог покрытия и Clock-инъекция решены ADR-0017/0018)* |

## 08. Ops

| Документ | Статус |
|---|---|
| [actuator.md](08-ops/actuator.md) | 🔵 |

## Roadmap

| Документ | Статус |
|---|---|
| [backlog.md](roadmap/backlog.md) — что специфицировано, что реализовано, что предстоит | 🟢 |

---

## Реестр ADR

| ADR | Решение | Статус |
|---|---|---|
| [ADR-001](03-architecture/adr/ADR-001-quest-is-the-core-domain-object.md) | Quest — корневой доменный объект | 🟢 Accepted |
| [ADR-002](03-architecture/adr/ADR-002-quest-start-is-global.md) | Старт Quest глобален; QuestProgress создаётся по расписанию, LevelProgress — лениво при входе | 🟢 Accepted |
| [ADR-003](03-architecture/adr/ADR-003-naming-of-runtime-entities.md) | Именование runtime-сущностей | 🟢 Accepted |
| [ADR-004](03-architecture/adr/ADR-004-code-value-uniqueness-scoped-to-level.md) | Уникальность `code_value` в пределах Level, не глобально | 🟢 Accepted |
| [ADR-005](03-architecture/adr/ADR-005-level-completion-model.md) | Модель завершения уровня: main-коды, синонимы, порог | 🟢 Accepted |
| [ADR-006](03-architecture/adr/ADR-006-code-points-renamed-to-bonus-penalty-seconds.md) | `Code.points` → `bonusPenaltySeconds`, единица — секунды | 🟢 Accepted |
| [ADR-007](03-architecture/adr/ADR-007-bonus-penalty-aggregation-model.md) | Итоговое время — агрегат ручных корректировок + кодов + подсказок | 🟢 Accepted |
| [ADR-009](03-architecture/adr/ADR-009-automatic-quest-progress-completion.md) | QuestProgress завершается автоматически при любом способе завершения последнего уровня | 🟢 Accepted |
| [ADR-010](03-architecture/adr/ADR-010-pessimistic-locking-for-registration-and-entry-races.md) | `SELECT FOR UPDATE` + идемпотентная обработка для найденных гонок | 🟢 Accepted |
| [ADR-011](03-architecture/adr/ADR-011-http-error-status-semantics.md) | Разведение `403`/`404`/`409` по трём разным исключениям | 🟢 Accepted |
| [ADR-012](03-architecture/adr/ADR-012-pagination-page-response.md) | Listing-эндпоинты возвращают `PageResponse<T>` с метаданными | 🟢 Accepted |
| [ADR-013](03-architecture/adr/ADR-013-quest-start-time-display-timezone.md) | `startTime` всегда отображается в поясе устройства пользователя | 🟢 Accepted |
| [ADR-014](03-architecture/adr/ADR-014-sse-for-live-statistics.md) | SSE как транспорт live-статистики | 🟢 Accepted |
| [ADR-015](03-architecture/adr/ADR-015-jwt-revocation-denylist.md) | Denylist для отзыва JWT раньше истечения | 🟢 Accepted |
| [ADR-016](03-architecture/adr/ADR-016-rate-limiting-bucket4j.md) | Rate limiting через `bucket4j` | 🟢 Accepted |
| [ADR-017](03-architecture/adr/ADR-017-jacoco-coverage-threshold.md) | Порог покрытия 70% (сервисный слой), проваливает сборку | 🟢 Accepted |
| [ADR-018](03-architecture/adr/ADR-018-clock-injection-required-for-time-dependent-services.md) | `Clock`-инъекция обязательна для time-dependent сервисов | 🟢 Accepted |
| [ADR-019](03-architecture/adr/ADR-019-single-entity-model-for-all-game-formats.md) | Одна модель данных для всех форматов игры, без offline/online split | 🟢 Accepted |
| [ADR-020](03-architecture/adr/ADR-020-hint-auto-reveal-bonus-penalty-types.md) | Hint: auto-reveal, типы Regular/Bonus/Penalty | 🟢 Accepted |

**Все 19 ADR приняты (Accepted).** Открытых продуктовых вопросов, блокирующих архитектуру, не осталось. Оставшиеся открытые вопросы внутри отдельных документов — это детали реализации (конкретные числа rate limiting, точный порог polling и т.п.), не архитектурные развилки.

---

## Известный технический долг документации

`01-domain/progress.md` и `01-domain/runtime.md` частично пересказывают одни и те же правила (жизненный цикл QuestProgress/LevelProgress, расчёт `autoTransitionAt`) разными словами — при следующей содержательной правке одного из них нужно свести их в один документ.

## Обнаруженные функциональные блокеры (не про документацию — про недостающий код)

- Эндпоинта «опубликовать Quest» (`DRAFT → REGISTRATION`) не существует ни в одном контроллере — Quest после создания навсегда остаётся в `DRAFT`, весь дальнейший процесс (регистрация, старт) недостижим. Блокер приоритета 0.
- Эндпоинта «завершить Quest» (`RUNNING → FINISHED`, автор) тоже не существует.
- Метод `setDnf()` реализован в сервисе, но не выведен ни в один контроллер.
- Пакет `statistic/` создан пустым — статистика не реализована совсем.
- Оставлен диагностический `GET /api/test/secure` — решить, удалять перед релизом или документировать намеренно.
- В проекте нет ни одной библиотеки rate limiting — уязвимо как минимум `/auth/login` (ADR-0016 принят, не реализован).
- `JwtService` и `GlobalExceptionHandler` не имеют собственных unit-тестов.
- CORS не сконфигурирован явно нигде в проекте.

Полная таблица соответствия "специфицировано → реализовано" — в `roadmap/backlog.md`.
