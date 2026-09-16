# QuestEngine — Документация

Это техническое задание и спецификация движка. Порядок в проекте: **сначала документация → потом архитектура → потом код**.

Учебный проект: цель — освоить максимально современный Java-стек так, как это делается в индустрии (проектирование, разработка, деплой, поддержка), двигаясь последовательно — от модульного монолита к микросервисам, когда проект дорастёт до нужной точки. Итоговый критерий готовности — рабочий движок, развёрнутый в интернете, на котором можно провести реальный квест. Подробнее — `00-vision/product-vision.md`.

Документы читаются по возрастанию номера папки — от бизнес-смысла к реализации.
Каждый документ имеет статус:

- 🟢 **Accepted** — согласовано.
- 🟡 **Draft** — основа есть, но требует уточнений.
- ⚪ **TBD** — раздел ещё не написан.
- 🔵 **Implemented** — реализовано в коде и покрыто тестами.

---

## 00. Vision — что мы строим и зачем

| Документ | Статус |
|---|---|
| [glossary.md](00-vision/glossary.md) | 🟢 |
| [product-vision.md](00-vision/product-vision.md) | 🟡 *(1 открытый вопрос — точные содержательные отличия от Encounter; не блокирует работу)* |

## 01. Domain — предметная область

| Документ | Статус |
|---|---|
| [quest.md](01-domain/quest.md) | 🟢 |
| [level.md](01-domain/level.md) | 🟢 |
| [team.md](01-domain/team.md) | 🟢 |
| [registration.md](01-domain/registration.md) | 🟢 |
| [progress.md](01-domain/progress.md) | 🟢 |
| [runtime.md](01-domain/runtime.md) | 🟡 *(частично дублирует progress.md — техдолг)* |
| [domain-events.md](01-domain/domain-events.md) | 🟢 |
| [invariants.md](01-domain/invariants.md) | 🟢 |
| [statistics-ranking.md](01-domain/statistics-ranking.md) | 🟢 *(модель формализована; реализация ещё отсутствует)* |
| [code-submission.md](01-domain/code-submission.md) | 🟢 *(модель решена; runtime-механика реализована)* |
| [hint-progress.md](01-domain/hint-progress.md) | 🔵 *(Job 3 + take endpoint; персональные подсказки — будущее улучшение)* |
| [bonus-penalty.md](01-domain/bonus-penalty.md) | 🟢 *(модель решена; реализация есть; полный API/runtime coverage остаётся в backlog)* |

## 02. Processes — бизнес-процессы

| Документ | Статус |
|---|---|
| [quest-lifecycle.md](02-processes/quest-lifecycle.md) | 🟢 |
| [sequence-diagrams.md](02-processes/sequence-diagrams.md) | 🟢 |
| [concurrency-scenarios.md](02-processes/concurrency-scenarios.md) | 🟢 *(ключевые гонки покрыты конкурентными тестами)* |

## 03. Architecture — технические решения

| Документ | Статус |
|---|---|
| [domain-model.md](03-architecture/domain-model.md) | 🟢 |
| [state-machines.md](03-architecture/state-machines.md) | 🟢 |
| [scheduling.md](03-architecture/scheduling.md) | 🟢 *(Job 1/Job 2 реализованы)* |
| [adr/](03-architecture/adr/) | 🟢 *(ADR приняты)* |

## 04. API — контракт

| Документ | Статус |
|---|---|
| [conventions.md](04-api/conventions.md) | 🟡 *(HTTP-статусы и пагинация реализованы; access+refresh ещё нет)* |
| [endpoints.md](04-api/endpoints.md) | 🟡 *(оставшиеся API gaps перечислены в `roadmap/backlog.md`)* |

## 05. Security

| Документ | Статус |
|---|---|
| [permissions.md](05-security/permissions.md) | 🟢 |
| [threat-model.md](05-security/threat-model.md) | 🟢 *(rate limiting login реализован; access+refresh ещё нет)* |

## 06. NFR — нефункциональные требования

| Документ | Статус |
|---|---|
| [requirements.md](06-nfr/requirements.md) | 🟢 |

## 07. Quality — тестирование и Definition of Done

| Документ | Статус |
|---|---|
| [testing-strategy.md](07-quality/testing-strategy.md) | 🟢 *(JaCoCo и k6 smoke готовы; k6 не является CI gate)* |

## 08. Ops

| Документ | Статус |
|---|---|
| [actuator.md](08-ops/actuator.md) | 🔵 |

## Roadmap

| Документ | Статус |
|---|---|
| [backlog.md](roadmap/backlog.md) | 🟢 |

---

## Технический долг документации

`01-domain/progress.md` и `01-domain/runtime.md` частично пересказывают одни и те же правила (жизненный цикл QuestProgress/LevelProgress, расчёт `autoTransitionAt`) разными словами. При следующей содержательной правке их нужно свести в один источник истины.

## Будущие улучшения (осознанно отложены за пределы MVP)

- **Персональные подсказки** — отдельная модель данных, не специфицирована подробно.
- **Ручное досрочное открытие подсказки автором** — не в MVP.

## Единственный оставшийся содержательный открытый вопрос

Список конкретных отличий движка от Encounter (`00-vision/product-vision.md`) можно уточнять по мере развития продукта; это не блокирует реализацию.

## Текущий статус

**Немедленных блокеров нет.**

Актуальный список незавершённых задач, решений и технического долга ведётся только в [roadmap/backlog.md](roadmap/backlog.md). Завершённые находки и разовые проверки не дублируются здесь.
