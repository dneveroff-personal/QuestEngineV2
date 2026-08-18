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
| product-vision.md — продукт, аналогия с Encounter, наши отличия | ⚪ *(не создан)* |

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
| [quest-lifecycle.md](02-processes/quest-lifecycle.md) | 🟢 |
| [sequence-diagrams.md](02-processes/sequence-diagrams.md) | 🟢 |
| concurrency-scenarios.md — гонки: лимит команд, параллельный автостарт | ⚪ *(не создан)* |

## 03. Architecture — технические решения

| Документ | Статус |
|---|---|
| [domain-model.md](03-architecture/domain-model.md) — ER-диаграмма, слои модели | 🟢 |
| [state-machines.md](03-architecture/state-machines.md) | 🟢 |
| scheduling.md — механизм автостарта Quest / автоперехода уровней | ⚪ *(не создан)* |
| [adr/](03-architecture/adr/) — architecture decision records | 🟢 |

## 04. API — контракт

См. [04-api/ReadMe.md](04-api/ReadMe.md). Статус: ⚪ TBD.

## 05. Security

| Документ | Статус |
|---|---|
| [permissions.md](05-security/permissions.md) | 🟢 |
| threat-model.md — rate limiting на коды, аудит действий автора | ⚪ *(не создан)* |

## 06. NFR — нефункциональные требования

См. [06-nfr/ReadMe.md](06-nfr/ReadMe.md). Статус: ⚪ TBD.

## 07. Quality — тестирование и Definition of Done

См. [07-quality/ReadMe.md](07-quality/ReadMe.md). Статус: ⚪ TBD.

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
3. ⏭ Автоматизация: `scheduling.md`, `concurrency-scenarios.md`.
4. ⏭ `04-api/`, `06-nfr/`.

Известный технический долг документации: `01-domain/progress.md` и `01-domain/runtime.md` частично пересказывают одни и те же правила (жизненный цикл QuestProgress/LevelProgress, расчёт `autoTransitionAt` для первого/последующих уровней) разными словами — при следующей правке одного из них нужно свести их в один документ, чтобы не рассинхронизировать.

## Решения, требующие ADR (найдены при написании раздела "Игровая механика")

При сверке новых доков с фактическим кодом (`code/`, `hint/`, `level/`) вскрылись расхождения между текущей реализацией и духом спеки — до реализации CodeSubmission/HintProgress их нужно закрыть отдельными ADR:

1. **Глобальная уникальность `code_value`** — сейчас код проверяется на уникальность по всей БД, а не в пределах `Level`. См. `code-submission.md` → «Существующая проблема в реализации».
2. **Критерий завершения уровня по кодам** — не формализовано, ввести ли "любой один MAIN-код" или "все MAIN-коды" (влияет на модель `LevelProgress`). См. `code-submission.md`.
3. **Единица измерения `Code.points`** и его переименование в `bonusPenaltySeconds` — сейчас поле без описанной единицы измерения. См. `bonus-penalty.md`.
4. **Согласование двух источников бонус/штраф-времени** (ручное решение автора vs `CodeType.BONUS/PENALTY`) — ранее описывались независимо, теперь сведены в единую модель агрегации в `bonus-penalty.md`.
