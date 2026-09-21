# Runtime Model

## Назначение

Runtime Model описывает состояние Quest во время фактического проведения игры.

> Статические сущности описывают правила игры; runtime-сущности — фактическое прохождение командами.

Детальные правила `QuestProgress` / `LevelProgress` / DNF / времени — в **`01-domain/progress.md`** (единый источник для lifecycle и инвариантов). Этот документ — обзор модели и связей.

---

## Static vs Runtime

Автор заранее создаёт:

```text
Quest
 └── Level
      ├── Code
      └── Hint
```

Во время игры появляются:

```text
QuestProgress
 └── LevelProgress
       ├── CodeSubmission
       └── HintProgress
```

---

## QuestProgress (кратко)

- Один `Quest` × одна `Team` → один `QuestProgress` на прохождение.
- Статусы: `WAITING` → `RUNNING` → `FINISHED` | `DNF`.
- Создание при старте Quest (APPROVED) или при позднем approve.
- Полные правила: **`progress.md`**.

---

## LevelProgress (кратко)

- Один активный `LevelProgress` на команду в момент времени.
- Завершение: коды (`COMPLETED`) или автопереход (`AUTO_TRANSITIONED`) — оба ведут к следующему уровню / финишу QuestProgress (ADR-0009).
- Автопереход в runtime выполняет **только** атомарный `LevelProgressRepository.tryAutoTransition` (Job 2).

---

## CodeSubmission / HintProgress

- Попытки ввода кода — audit trail; видимость — `statistics-ranking.md`.
- REGULAR — auto-reveal (Job 3); BONUS/PENALTY — take (ADR-0020/0021).

---

## Bonus / Penalty

Агрегация при чтении (ADR-0007). См. `bonus-penalty.md`.

---

## Runtime Isolation

Изменение прогресса одной команды не влияет на другие команды того же Quest.

---

## Statistics

Правила мест — `statistics-ranking.md`.  
Snapshot: `GET /api/quests/{id}/statistics`. Live: SSE (ADR-0014, PR #22).
