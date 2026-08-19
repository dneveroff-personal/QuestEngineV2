# ADR-0018 — Clock Injection Required For Time-Dependent Services

## Status

Accepted

## Context

`LevelProgressServiceImpl` и `QuestProgressServiceImpl` уже используют инъекцию `java.time.Clock` вместо прямого вызова `Instant.now()` — правильный паттерн, позволяющий детерминированно тестировать логику, зависящую от времени (например, «прошло ли `autoTransitionAt`»).

`Instant.now()` вызывается напрямую (без инъекции) ещё в 16 файлах — часть из них уместно (audit-поля `createdAt`/`updatedAt`, где детерминированность в тестах не критична), но как минимум `QuestRegistrationServiceImpl` потребует детерминированного времени при доработках (`ADR-0010`). Найдено при написании `07-quality/testing-strategy.md`.

Будущие `CodeSubmission` (`01-domain/code-submission.md`), `HintProgress` (`01-domain/hint-progress.md`) и Job 1/Job 2 планировщика (`03-architecture/scheduling.md`) — всё это логика, где текущее время напрямую определяет бизнес-решение (доступна ли подсказка, истёк ли уровень, наступил ли момент старта).

## Decision

Любой сервис, для которого текущее время влияет на бизнес-логику (не только на audit-поля `createdAt`/`updatedAt`), обязан принимать `java.time.Clock` через конструктор — по уже установленному в `LevelProgressServiceImpl`/`QuestProgressServiceImpl` паттерну — и не вызывать `Instant.now()`/`System.currentTimeMillis()` напрямую внутри бизнес-методов.

Это правило применяется как обязательное code-review требование к новым сервисам (`CodeSubmission`, `HintProgress`, Job 1/Job 2) с момента принятия ADR, и как желательный рефакторинг для существующих сервисов при следующей содержательной правке (не форсируется отдельной задачей ради самого рефакторинга).

## Consequences

Преимущества:
- Детерминированное тестирование границ временных условий (ровно в момент `autoTransitionAt`, за секунду до/после) без `Thread.sleep()` и flaky-тестов.
- Единообразный паттерн по всей кодовой базе для time-dependent логики.

Издержки:
- Минимальные — паттерн уже частично внедрён, распространение на новый код не требует архитектурных изменений, только дисциплины при код-ревью.
