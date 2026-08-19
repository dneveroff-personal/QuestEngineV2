# QuestEngine — Документация

Это техническое задание и спецификация движка. Порядок в проекте: **сначала документация → потом архитектура → потом код**.

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
| [product-vision.md](00-vision/product-vision.md) — продукт, аналогия с Encounter, наши отличия | 🟡 *(3 открытых вопроса — аудитория, офлайн/онлайн формат, конкретные отличия от Encounter)* |

## 01. Domain — предметная область ("что")

Статические сущности (шаблон игры) и Runtime-сущности (прохождение игры).

| Документ | Статус |
|---|---|
| [quest.md](01-domain/quest.md) | 🟢 |
| [level.md](01-domain/level.md) | 🟢 |
| [team.md](01-domain/team.md) | 🟢 |
| [registration.md](01-domain/registration.md) | 🟢 |
| [progress.md](01-domain/progress.md) — QuestProgress / LevelProgress | 🟢 |
| [runtime.md](01-domain/runtime.md) — сводная runtime-модель | 🟡 *(частично дублирует progress.md — требует слияния)* |
| [domain-events.md](01-domain/domain-events.md) | 🟢 |
| [invariants.md](01-domain/invariants.md) | 🟢 |
| [statistics-ranking.md](01-domain/statistics-ranking.md) | 🟡 *(ranking для первого уровня не формализован)* |
| [code-submission.md](01-domain/code-submission.md) — правила ввода кодов, попытки, брутфорс | 🟡 *(5 открытых вопросов, включая конфликт с текущей реализацией — глобальная уникальность кода)* |
| [hint-progress.md](01-domain/hint-progress.md) — правила открытия подсказок | 🟡 *(3 открытых вопроса — auto vs manual reveal, штраф за подсказку)* |
| [bonus-penalty.md](01-domain/bonus-penalty.md) — правила начисления/отмены, аудит | 🟡 *(разрешает конфликт между "начисляет только автор" и уже существующими `CodeType.BONUS/PENALTY`)* |

## 02. Processes — бизнес-процессы ("как", без привязки к транспорту)

| Документ | Статус |
|---|---|
| [quest-lifecycle.md](02-processes/quest-lifecycle.md) | 🟡 *(шаг 6 противоречит progress.md/ADR-002 — см. scheduling.md)* |
| [sequence-diagrams.md](02-processes/sequence-diagrams.md) | 🟢 |
| [concurrency-scenarios.md](02-processes/concurrency-scenarios.md) — гонки: лимит команд, параллельный автостарт | 🟡 *(5 сценариев, 2 из них — подтверждённые уязвимости в текущем коде)* |

## 03. Architecture — технические решения

| Документ | Статус |
|---|---|
| [domain-model.md](03-architecture/domain-model.md) — ER-диаграмма, слои модели | 🟢 |
| [state-machines.md](03-architecture/state-machines.md) | 🟢 |
| [scheduling.md](03-architecture/scheduling.md) — механизм автостарта Quest / автоперехода уровней | 🟡 *(механизм не реализован вообще; найден блокер — нет эндпоинта публикации Quest)* |
| [adr/](03-architecture/adr/) — architecture decision records | 🟢 *(19 ADR: 001–003 исходные, 004–018 закрывают найденные расхождения, 019 — единственный формально открытый продуктовый вопрос)* |

## 04. API — контракт

| Документ | Статус |
|---|---|
| [conventions.md](04-api/conventions.md) — формат ошибок, аутентификация, пагинация, версионирование | 🟡 *(2 несоответствия статус-кодов найдены в коде, требуют решения)* |
| [endpoints.md](04-api/endpoints.md) — карта ресурсов, статус по каждому | 🟡 *(4 отсутствующих эндпоинта найдены, включая блокер публикации Quest)* |

## 05. Security

| Документ | Статус |
|---|---|
| [permissions.md](05-security/permissions.md) | 🟢 |
| [threat-model.md](05-security/threat-model.md) — rate limiting, отзыв JWT, CORS, IDOR | 🟡 *(4 открытых вопроса, включая отсутствие rate limiting нигде в проекте)* |

## 06. NFR — нефункциональные требования

| Документ | Статус |
|---|---|
| [requirements.md](06-nfr/requirements.md) — нагрузка, транспорт статистики, часовые пояса, локализация | 🟡 *(6 открытых вопросов, требующих решения продукта, не только техники)* |

## 07. Quality — тестирование и Definition of Done

| Документ | Статус |
|---|---|
| [testing-strategy.md](07-quality/testing-strategy.md) — паттерн тестов, DoD, найденные пробелы покрытия | 🟡 *(3 открытых вопроса, включая порог покрытия для CI)* |

## 08. Ops

| Документ | Статус |
|---|---|
| [actuator.md](08-ops/actuator.md) | 🔵 |

## Roadmap

| Документ | Статус |
|---|---|
| [backlog.md](roadmap/backlog.md) — что специфицировано, но ещё не реализовано | 🟡 |

---

## Текущий план работ над документацией

1. ✅ Реструктуризация `docs/` по разделам.
2. ✅ Игровая механика: `code-submission.md`, `hint-progress.md`, `bonus-penalty.md`.
3. ✅ Автоматизация: `scheduling.md`, `concurrency-scenarios.md`.
4. ✅ `04-api/`, `06-nfr/`.
5. ✅ `07-quality/testing-strategy.md`, `05-security/threat-model.md`, `00-vision/product-vision.md`.

**Первый проход документации завершён — все разделы `docs/` содержат содержательный контент**, ни одного файла в статусе ⚪ TBD не осталось. Все оставшиеся 🟡 Draft документы содержат явные разделы "Открытые вопросы" — это ожидаемо и осознанно: часть вопросов требует решения продукта (не техническое решение можно принять в одностороннем порядке), часть — согласования через ADR перед началом кодирования. Полный сводный список открытых вопросов, требующих ADR — ниже. Полный список открытых вопросов внутри каждого документа — соответственно в самих документах.

**Список "Решения, требующие ADR" ниже — закрыт.** Все 16 пунктов оформлены как ADR-004…ADR-019 в `03-architecture/adr/`. Из них:
- **14 приняты (Accepted)** — чисто инженерные решения, реализация может начинаться.
- **2 остаются Proposed**, т.к. это игровой дизайн/продуктовые вопросы, которые нельзя закрыть в одностороннем порядке: ADR-0005 (критерий завершения уровня — один код или все) и ADR-0009 (автозавершение QuestProgress) — по обоим дана чёткая рекомендация с обоснованием, но финальное подтверждение — за продуктом.
- **ADR-0013 заблокирован ADR-0019** — единственный по-настоящему нерешённый вопрос всей документации: целевая аудитория и формат игры (офлайн/онлайн). Это не инженерный вопрос, ответить на него может только владелец продукта.

**Следующий шаг проекта**: получить решение по трём Proposed/открытым ADR (0005, 0009, 0019 → разблокирует 0013), затем переходить к архитектурному проектированию и реализации `CodeSubmission`/`HintProgress`/планировщика — начиная с блокеров приоритета 0 в `roadmap/backlog.md`.

Известный технический долг документации: `01-domain/progress.md` и `01-domain/runtime.md` частично пересказывают одни и те же правила (жизненный цикл QuestProgress/LevelProgress, расчёт `autoTransitionAt` для первого/последующих уровней) разными словами — при следующей правке одного из них нужно свести их в один документ, чтобы не рассинхронизировать.

## Реестр ADR (все 16 найденных расхождений закрыты как формальные решения)

| ADR | Решение | Статус |
|---|---|---|
| [ADR-004](03-architecture/adr/ADR-004-code-value-uniqueness-scoped-to-level.md) | Уникальность `code_value` в пределах Level, не глобально | 🟢 Accepted |
| [ADR-005](03-architecture/adr/ADR-005-level-completion-any-single-main-code.md) | Уровень завершается любым одним MAIN-кодом | 🟡 Proposed — игровой дизайн, нужно подтверждение продукта |
| [ADR-006](03-architecture/adr/ADR-006-code-points-renamed-to-bonus-penalty-seconds.md) | `Code.points` → `bonusPenaltySeconds`, единица — секунды | 🟢 Accepted |
| [ADR-007](03-architecture/adr/ADR-007-bonus-penalty-aggregation-model.md) | Итоговое время — агрегат ручных корректировок + кодов | 🟢 Accepted |
| [ADR-008](03-architecture/adr/ADR-008-remove-eager-level-progress-creation-at-quest-start.md) | Амендирует ADR-002: LevelProgress создаётся лениво, не eager | 🟢 Accepted |
| [ADR-009](03-architecture/adr/ADR-009-automatic-quest-progress-completion-on-codes-only.md) | QuestProgress завершается автоматически только по кодам | 🟡 Proposed — игровой дизайн, нужно подтверждение продукта |
| [ADR-010](03-architecture/adr/ADR-010-pessimistic-locking-for-registration-and-entry-races.md) | `SELECT FOR UPDATE` + идемпотентная обработка для найденных гонок | 🟢 Accepted |
| [ADR-011](03-architecture/adr/ADR-011-http-error-status-semantics.md) | Разведение `403`/`404`/`409` по трём разным исключениям | 🟢 Accepted |
| [ADR-012](03-architecture/adr/ADR-012-pagination-page-response.md) | Listing-эндпоинты возвращают `PageResponse<T>` с метаданными | 🟢 Accepted |
| [ADR-013](03-architecture/adr/ADR-013-quest-start-time-display-timezone.md) | Часовой пояс отображения `startTime` | 🔴 Заблокирован ADR-019 |
| [ADR-014](03-architecture/adr/ADR-014-sse-for-live-statistics.md) | SSE как транспорт live-статистики | 🟢 Accepted |
| [ADR-015](03-architecture/adr/ADR-015-jwt-revocation-denylist.md) | Denylist для отзыва JWT раньше истечения | 🟢 Accepted |
| [ADR-016](03-architecture/adr/ADR-016-rate-limiting-bucket4j.md) | Rate limiting через `bucket4j` | 🟢 Accepted |
| [ADR-017](03-architecture/adr/ADR-017-jacoco-coverage-threshold.md) | Порог покрытия 70% (сервисный слой), проваливает сборку | 🟢 Accepted |
| [ADR-018](03-architecture/adr/ADR-018-clock-injection-required-for-time-dependent-services.md) | `Clock`-инъекция обязательна для time-dependent сервисов | 🟢 Accepted |
| [ADR-019](03-architecture/adr/ADR-019-target-audience-and-game-format-open-decision.md) | Целевая аудитория и формат игры (офлайн/онлайн) | 🔴 Proposed — открытый продуктовый вопрос, не решается инженерами |

**Единственные три пункта, ожидающие решения от владельца продукта, а не от инженерной команды: ADR-0005, ADR-0009, ADR-0019** (последний дополнительно блокирует ADR-0013).

## Обнаруженные функциональные блокеры (не про документацию — про недостающий код)

- Эндпоинта «опубликовать Quest» (`DRAFT → REGISTRATION`) не существует ни в одном контроллере — Quest после создания навсегда остаётся в `DRAFT`, весь дальнейший процесс (регистрация, старт) недостижим. Блокер приоритета 0.
- Эндпоинта «завершить Quest» (`RUNNING → FINISHED`, автор) тоже не существует.
- Метод `setDnf()` реализован в сервисе, но не выведен ни в один контроллер.
- Пакет `statistic/` создан пустым — статистика не реализована совсем.
- Оставлен диагностический `GET /api/test/secure` — решить, удалять перед релизом или документировать намеренно.
- В проекте нет ни одной библиотеки rate limiting — уязвимо как минимум `/auth/login`.
- `JwtService` и `GlobalExceptionHandler` не имеют собственных unit-тестов.
- CORS не сконфигурирован явно нигде в проекте.

Полная таблица соответствия "специфицировано → реализовано" — в `roadmap/backlog.md`.
