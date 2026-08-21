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
| [product-vision.md](00-vision/product-vision.md) — продукт, аналогия с Encounter, учебные цели, аудитория | 🟡 *(1 открытый вопрос — точные содержательные отличия от Encounter; аудитория решена)* |

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
| [statistics-ranking.md](01-domain/statistics-ranking.md) | 🟢 *(модель ranking полностью формализована, включая первый уровень; дублирующий раздел устранён)* |
| [code-submission.md](01-domain/code-submission.md) — коды, синонимы, порог | 🟢 *(модель полностью решена — ADR-0004/0005/0006, rate limiting, видимость статистики, формат прогресса)* |
| [hint-progress.md](01-domain/hint-progress.md) — auto-reveal, типы Regular/Bonus/Penalty | 🟢 *(модель решена ADR-0020; ручное досрочное открытие — не в MVP, персональные подсказки — будущее улучшение)* |
| [bonus-penalty.md](01-domain/bonus-penalty.md) — три источника бонус/штрафа, аудит | 🟢 *(модель решена ADR-0007/0020; видимость reason и отсутствие лимитов решены)* |

## 02. Processes — бизнес-процессы ("как", без привязки к транспорту)

| Документ | Статус |
|---|---|
| [quest-lifecycle.md](02-processes/quest-lifecycle.md) | 🟢 |
| [sequence-diagrams.md](02-processes/sequence-diagrams.md) | 🟢 |
| [concurrency-scenarios.md](02-processes/concurrency-scenarios.md) — гонки: лимит команд, автостарт, порог кодов под нагрузкой | 🟢 *(6 сценариев, все с решением — ADR-0010 + Сценарий 6 для высокой конкурентной нагрузки ввода кода)* |

## 03. Architecture — технические решения

| Документ | Статус |
|---|---|
| [domain-model.md](03-architecture/domain-model.md) — ER-диаграмма, слои модели | 🟢 |
| [state-machines.md](03-architecture/state-machines.md) | 🟢 |
| [scheduling.md](03-architecture/scheduling.md) — механизм автостарта Quest / автоперехода уровней | 🟢 *(модель полностью решена, включая интервал polling — 1 сек; сам механизм ещё не реализован в коде)* |
| [adr/](03-architecture/adr/) — architecture decision records | 🟢 *(20 ADR, все Accepted — см. реестр ниже)* |

## 04. API — контракт

| Документ | Статус |
|---|---|
| [conventions.md](04-api/conventions.md) — формат ошибок, аутентификация (access+refresh), пагинация, версионирование | 🟢 *(решения приняты, реализация ещё не выполнена)* |
| [endpoints.md](04-api/endpoints.md) — карта ресурсов, статус по каждому | 🟡 *(6 отсутствующих эндпоинтов найдены, включая блокер публикации Quest — см. `roadmap/backlog.md`)* |

## 05. Security

| Документ | Статус |
|---|---|
| [permissions.md](05-security/permissions.md) | 🟢 |
| [threat-model.md](05-security/threat-model.md) — access+refresh токены, rate limiting (только login), CORS (same-origin) | 🟢 *(все решения приняты, реализация ещё не выполнена)* |

## 06. NFR — нефункциональные требования

| Документ | Статус |
|---|---|
| [requirements.md](06-nfr/requirements.md) — нагрузка, транспорт статистики, часовые пояса, локализация, отказоустойчивость | 🟢 *(все решения приняты; список конкретных отличий от Encounter — отдельный открытый вопрос в product-vision.md, не здесь)* |

## 07. Quality — тестирование и Definition of Done

| Документ | Статус |
|---|---|
| [testing-strategy.md](07-quality/testing-strategy.md) — паттерн тестов, DoD, k6-смок-тест на Сценарий 6 | 🟢 *(все решения приняты — порог покрытия, Clock-инъекция, нагрузочное тестирование)* |

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
| [ADR-005](03-architecture/adr/ADR-005-level-completion-model.md) | Модель завершения уровня: коды, синонимы, порог | 🟢 Accepted |
| [ADR-006](03-architecture/adr/ADR-006-code-points-renamed-to-bonus-penalty-seconds.md) | `Code.points` → `bonusPenaltySeconds`, единица — секунды | 🟢 Accepted |
| [ADR-007](03-architecture/adr/ADR-007-bonus-penalty-aggregation-model.md) | Итоговое время — агрегат ручных корректировок + кодов + подсказок | 🟢 Accepted |
| [ADR-009](03-architecture/adr/ADR-009-automatic-quest-progress-completion.md) | QuestProgress завершается автоматически при любом способе завершения последнего уровня | 🟢 Accepted |
| [ADR-010](03-architecture/adr/ADR-010-pessimistic-locking-for-registration-and-entry-races.md) | `SELECT FOR UPDATE` + идемпотентная обработка для найденных гонок | 🟢 Accepted |
| [ADR-011](03-architecture/adr/ADR-011-http-error-status-semantics.md) | Разведение `403`/`404`/`409` по трём разным исключениям | 🟢 Accepted |
| [ADR-012](03-architecture/adr/ADR-012-pagination-page-response.md) | Listing-эндпоинты возвращают `PageResponse<T>` с метаданными | 🟢 Accepted |
| [ADR-013](03-architecture/adr/ADR-013-quest-start-time-display-timezone.md) | `startTime` всегда отображается в поясе устройства пользователя | 🟢 Accepted |
| [ADR-014](03-architecture/adr/ADR-014-sse-for-live-statistics.md) | SSE как транспорт live-статистики, без искусственной задержки | 🟢 Accepted |
| [ADR-015](03-architecture/adr/ADR-015-access-refresh-token-pattern.md) | Access (15 мин) + refresh (в БД, с ротацией) токены | 🟢 Accepted |
| [ADR-016](03-architecture/adr/ADR-016-rate-limiting-bucket4j.md) | Rate limiting через `bucket4j` — только для `/auth/login`, явно НЕ для ввода кода | 🟢 Accepted |
| [ADR-017](03-architecture/adr/ADR-017-jacoco-coverage-threshold.md) | Порог покрытия 70% (сервисный слой), проваливает сборку | 🟢 Accepted |
| [ADR-018](03-architecture/adr/ADR-018-clock-injection-required-for-time-dependent-services.md) | `Clock`-инъекция обязательна для time-dependent сервисов | 🟢 Accepted |
| [ADR-019](03-architecture/adr/ADR-019-single-entity-model-for-all-game-formats.md) | Одна модель данных для всех форматов игры, без offline/online split | 🟢 Accepted |
| [ADR-020](03-architecture/adr/ADR-020-hint-auto-reveal-bonus-penalty-types.md) | Hint: auto-reveal, типы Regular/Bonus/Penalty | 🟢 Accepted |

**Все 19 ADR приняты (Accepted).** Открытых архитектурных вопросов не осталось.

---

## Известный технический долг документации

`01-domain/progress.md` и `01-domain/runtime.md` частично пересказывают одни и те же правила (жизненный цикл QuestProgress/LevelProgress, расчёт `autoTransitionAt`) разными словами — при следующей содержательной правке одного из них нужно свести их в один документ.

## Будущие улучшения (осознанно отложены за пределы MVP)

- **Персональные подсказки** — автор сможет создавать подсказку, видимую только конкретной команде (или нескольким), не всем участникам квеста (`01-domain/hint-progress.md`). Отдельная модель данных, не специфицирована подробно.
- **Ручное досрочное открытие подсказки автором** — не в MVP, только auto-reveal по таймеру.

## Единственный оставшийся содержательный открытый вопрос

Список конкретных отличий движка от Encounter (`00-vision/product-vision.md`) — не заполнен, не блокирует ни архитектуру, ни реализацию; можно закрывать по мере развития продукта, не откладывая переход к коду.

## Обнаруженные функциональные блокеры (не про документацию — про недостающий код)

- Эндпоинта «опубликовать Quest» (`DRAFT → REGISTRATION`) не существует ни в одном контроллере — Quest после создания навсегда остаётся в `DRAFT`, весь дальнейший процесс (регистрация, старт) недостижим. Блокер приоритета 0.
- Эндпоинта «завершить Quest» (`RUNNING → FINISHED`, автор) тоже не существует.
- Метод `setDnf()` реализован в сервисе, но не выведен ни в один контроллер.
- Пакет `statistic/` создан пустым — статистика не реализована совсем.
- Оставлен диагностический `GET /api/test/secure` — решить, удалять перед релизом или документировать намеренно.
- В проекте нет ни одной библиотеки rate limiting — уязвимо как минимум `/auth/login` (ADR-0016 принят, не реализован).
- `JwtService` и `GlobalExceptionHandler` не имеют собственных unit-тестов.
- CORS не требуется по решённой архитектуре (same-origin), но стоит явно проверить конфигурацию reverse-proxy при первом деплое.
- 🔴 Критично: открытый JDWP debug-порт (5004) в `docker-compose.prod.yml` — см. `roadmap/backlog.md`.
- `.github/workflows/deploy.yml` в текущем виде не может сработать (ошибка в `if`-условии + неверный путь) — см. `roadmap/backlog.md`.
- Текущая аутентификация в коде (`JwtService`, `AuthController`) реализует старую модель (единый JWT на 24ч) — требует переработки под access+refresh (ADR-0015).

Полная таблица соответствия "специфицировано → реализовано" — в `roadmap/backlog.md`.
