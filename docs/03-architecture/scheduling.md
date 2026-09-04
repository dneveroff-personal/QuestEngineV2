# Scheduling — автоматизация по времени

Статус: 🟢 Accepted, Implemented. Job 1 (`QuestStartScheduler`), Job 2 (`LevelAutoTransitionScheduler`) и Job 3 (`HintRevealScheduler`) реализованы в пакете `dn.questenginev2.scheduling` — `@Scheduled(fixedDelay = 1000)`, single-instance (ShedLock отложен, см. ниже). Отключается в тестах (`scheduling.enabled=false`), чтобы не мешать остальным `@SpringBootTest`.

## Назначение

В движке есть три независимых процесса, управляемых **временем**, а не действием пользователя — то, что в описании Encounter называется «игра идёт сама», без нажатия кнопок:

1. **Старт Quest** — переход `REGISTRATION → RUNNING` в момент `Quest.startTime`.
2. **Автопереход уровня** — переход `LevelProgress.ACTIVE → AUTO_TRANSITIONED` в момент `LevelProgress.autoTransitionAt`.
3. **Auto-reveal подсказки** — показ `Hint` команде в момент `LevelProgress.openedAt + Hint.delaySeconds` (ADR-0020).

Оба процесса должны срабатывать **без участия автора или команды** — иначе вся идея честного игрового времени (ADR-002) не работает.

---

## История (для контекста — состояние на момент написания документа, уже неактуально)

Изначально (до реализации): в проекте не было ни одного `@Scheduled`, ни эндпоинта публикации Quest. Оба блокера закрыты в последующих итерациях — публикация (`POST /api/quests/{id}/publish`) реализована раньше планировщика, `createProgress()` использован как строительный блок именно так, как и предполагалось здесь изначально.

---

## Job 1 — Quest Start

### Триггер
`Quest.status == REGISTRATION AND Quest.startTime <= now()`.

### Действие (реализовано)
1. `QuestStartScheduler.startDueQuests()` находит кандидатов (`QuestRepository.findByStatusAndStartTimeLessThanEqual`), атомарно переводит каждый в `RUNNING` (`QuestRepository.tryStartQuest` — условный JPQL UPDATE `WHERE status = REGISTRATION`).
2. Если этот вызов выиграл переход — для каждой `APPROVED`-регистрации вызывается `questProgressService.createProgress(questId, teamId)`, с перехватом `IllegalArgumentException` на уровне отдельной команды (Сценарий 7, `concurrency-scenarios.md`).

### Уточнение относительно ADR-002 (уже учтено в финальной редакции ADR-002)

`ADR-002-quest-start-is-global.md` в финальной редакции уже фиксирует правильную модель: `QuestProgress` создаётся при старте Quest, а `LevelProgress` — лениво, при входе команды. Честность игрового времени не зависит от момента создания строки `LevelProgress` — она обеспечена тем, что `autoTransitionAt` для первого уровня считается от `Quest.startTime` (`QuestProgress.questStartedAt`), а не от `LevelProgress.openedAt`. Job 1 должен создавать только `QuestProgress`, не `LevelProgress`.

### Идемпотентность (реализовано)

- Переход статуса — условный UPDATE `QuestRepository.tryStartQuest` (`WHERE status = REGISTRATION`), а не read-then-write.
- Создание `QuestProgress` — защищено уникальным индексом `(quest_id, team_id)`; повторный/конкурирующий вызов для уже обработанной команды перехватывается как `IllegalArgumentException` и пропускается (Сценарий 7).

---

## Job 2 — Level Auto-Transition

### Триггер
`LevelProgress.status == ACTIVE AND LevelProgress.autoTransitionAt <= now()`.

### Действие (реализовано)
`LevelAutoTransitionScheduler.autoTransitionDueLevels()`: находит кандидатов (`LevelProgressRepository.findByStatusAndAutoTransitionAtLessThanEqual`), атомарно переводит каждый в `AUTO_TRANSITIONED` (`tryAutoTransition` — условный native UPDATE `WHERE status = 'ACTIVE'`, то же условие, что и у `CodeSubmission`, разрешает Сценарий 5). Если этот вызов выиграл — вызывает `advanceAfterLevelCompleted(...)`, который открывает следующий `Level` либо завершает `QuestProgress` (ADR-0009).

### Завершение QuestProgress при завершении последнего уровня (ADR-0009)

`QuestProgress` переводится в `FINISHED` автоматически при завершении последнего Level команды — **независимо** от способа завершения (`CODES` или `AUTO_TRANSITION`), разницы нет (ADR-0009, окончательное решение владельца продукта).

Реализовано: `QuestProgressServiceImpl.advanceAfterLevelCompleted(LevelProgress)` — выделен из `completeLevel()` отдельным методом (рефакторинг при реализации `CodeSubmission`, `0.5.18`+) специально для переиспользования и `CodeSubmission`, и Job 2 — без ветвления по способу завершения предыдущего уровня, соответствует ADR-0009.

### Идемпотентность

Аналогично Job 1: переход статуса через условный UPDATE (`WHERE status='ACTIVE'`), создание следующего `LevelProgress` защищено уникальным индексом `(quest_progress_id, level_id)` (`V12__create_level_progress_table.sql`).

---

## Job 3 — Hint Auto-Reveal (реализовано)

`01-domain/hint-progress.md`, ADR-0020.

### Триггер
Для каждого `LevelProgress.status == ACTIVE`, для каждой подсказки его уровня: `LevelProgress.openedAt + Hint.delaySeconds <= now()` и подсказка ещё не показана.

### Действие
`HintRevealScheduler.revealDueHints()`: находит все `LevelProgress` в статусе `ACTIVE`, для каждого — подсказки уровня, для каждой ещё не показанной и с истёкшей задержкой создаёт `HintProgress`.

### Отличие от Job 1/Job 2

Здесь **нет конкурирующего пути** — показ подсказки не может произойти никаким другим способом, кроме этого планировщика (в отличие от Job 2, который конкурирует с `CodeSubmission` за один и тот же `LevelProgress`, Сценарий 5). Идемпотентность обеспечивается уникальным индексом `(level_progress_id, hint_id)` и перехватом нарушения — defense-in-depth, а не разрешение гонки между двумя разными путями.

---

## Механизм выполнения

### Рекомендация для MVP: периодический polling-job

`@Scheduled(fixedDelay = ...)` в Spring — простейший вариант, не требует внешней инфраструктуры (очередей, брокеров), укладывается в уже выбранный стек (Spring Boot). Для масштаба «десятки одновременных Quest, сотни команд» — достаточно.

Альтернативы, рассмотренные и отклонённые для MVP:
- **Точный таймер на каждое событие** (`ScheduledExecutorService.schedule(delay)` на каждый `Quest.startTime`/`autoTransitionAt`) — даёт точность до миллисекунд, но требует пересчёта таймеров при редактировании (сдвиг `startTime`, `autoTransition`) и не переживает перезапуск инстанса без восстановления из БД при старте. Усложняет реализацию непропорционально выгоде на данном масштабе.
- **Очередь отложенных сообщений** (например, через брокер с delay-delivery) — правильное решение для по-настоящему большого масштаба, но избыточная инфраструктура для текущей стадии проекта.

### Точность (решено — 1 секунда)

Интервал polling — **1 секунда**. Выбрано как максимально чёткое значение из рассмотренных, согласовано с общим приоритетом проекта на скорость и честность игрового времени (см. `06-nfr/requirements.md`, решение не троттлить ввод кода — «скорость — часть сути игры», то же самое соображение применено и здесь). Запрос к БД раз в секунду (по обоим Job) — пренебрежимая нагрузка на заявленном масштабе (`06-nfr/requirements.md`, «Нагрузка» — единицы-десятки одновременных Quest).

### Множественные инстансы приложения (решено — не в MVP)

**MVP разворачивается в единственном экземпляре** (`06-nfr/requirements.md`) — ShedLock **откладывается**, распределённая блокировка не нужна прямо сейчас.

Тем не менее идемпотентность самих Job (условные UPDATE, см. Job 1/Job 2 выше) реализуется сразу, независимо от этого решения — она защищает и от одиночных ретраев в единственном инстансе, и заранее готовит код к горизонтальному масштабированию без переделки бизнес-логики, когда/если понадобится multi-instance деплой (тогда потребуется только добавить ShedLock поверх уже идемпотентного кода — рекомендация на будущее: библиотека **ShedLock**, `net.javacrumbs.shedlock`, поверх уже используемого PostgreSQL, не требует новой инфраструктуры).

---

## Открытые вопросы

Нет открытых вопросов — интервал polling (1 секунда), ShedLock (отложен до multi-instance), эндпоинт публикации Quest (блокер, зафиксирован в `roadmap/backlog.md`) — все решения приняты, см. соответствующие разделы выше.

Правка ADR-002 (eager vs ленивое создание LevelProgress) и автоматическое завершение QuestProgress (ADR-0009) — решены, см. соответствующие ADR выше по тексту документа.
