# Quest Progress

## Назначение
`QuestProgress` представляет собой прохождение конкретного Quest конкретной командой.

Обзор static/runtime модели — в `01-domain/runtime.md`. Детали lifecycle и инвариантов — **этот файл**.

QuestProgress является runtime-сущностью игрового прохождения.
Активное прохождение существует во время проведения Quest.
После завершения Quest QuestProgress сохраняется как история прохождения и используется для формирования итоговой статистики и результатов.

---

# Жизненный цикл

```
Quest
        │
        ▼
QuestRegistration (APPROVED)
        │
        ▼
QuestProgress
        │
        ▼
LevelProgress
```

---

# Создание
QuestProgress создаётся автоматически при выполнении двух условий.

1. Quest имеет статус
```
RUNNING
```

2. Регистрация команды имеет статус
```
APPROVED
```

Если регистрация подтверждена до начала Quest,
QuestProgress создаётся в момент старта Quest.

Если регистрация подтверждена после старта Quest,
QuestProgress создаётся в момент подтверждения.

---

# Один QuestProgress

На одну пару (Quest, Team) существует не более одного QuestProgress.

---

# Игровое время

Игровое время начинается в `Quest.startTime` (`questStartedAt`), а не в момент входа команды.
`enteredAt` фиксирует фактический вход и не сдвигает старт времени.

---

# Первый уровень

Первый LevelProgress создаётся при входе команды (WAITING → RUNNING).
Если к моменту входа `autoTransitionAt` уже прошло — уровень сразу `AUTO_TRANSITIONED`.

---

# Последующие уровни

Следующий LevelProgress создаётся только после завершения предыдущего (CODES или AUTO_TRANSITION — ADR-0009).

---

# Завершение уровня

- `COMPLETED` — порог main-кодов;
- `AUTO_TRANSITIONED` — Job 2, атомарный `tryAutoTransition`.

Небезопасный service-метод `autoTransitionLevel` удалён (backlog #20).

---

# Завершение Quest

После последнего уровня QuestProgress → `FINISHED`.
При `finishQuest` автора незавершённые → `DNF`.

---

# Ручной DNF (дисквалификация)

Ручной DNF доступен только когда `Quest.status = FINISHED`.
Может быть применён и к команде со статусом `FINISHED` (замена результата на DNF).

---

# Повторное прохождение

QuestProgress одноразовый. История сохраняется.

---

# Инварианты QuestProgress
- Один QuestProgress — одна команда, один Quest.
- Игровое время не зависит от времени входа.

## Инварианты LevelProgress
- Одновременно один ACTIVE LevelProgress.
- Первый создаётся при входе; следующий — после завершения предыдущего.
- Автопереход не даёт дополнительного времени.
