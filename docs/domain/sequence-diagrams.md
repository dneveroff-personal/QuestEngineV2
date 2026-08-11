# Sequence Diagrams

## Назначение

Диаграммы описывают основные сценарии работы QuestEngine.
Диаграммы показывают бизнес-последовательность событий и не привязаны к конкретным REST endpoint, микросервисам или брокерам сообщений.

---

# 1. Создание Quest

```mermaid
sequenceDiagram
    actor Author
    participant QuestEngine
    participant Quest

    Author->>QuestEngine: Create Quest
    QuestEngine->>Quest: Create
    Quest-->>QuestEngine: Quest DRAFT
    QuestEngine-->>Author: Quest created
```

---

# 2. Подача заявки

Заявку подаёт капитан команды.

```mermaid
sequenceDiagram
    actor Captain
    participant QuestEngine
    participant Team
    participant Quest
    participant Registration

    Captain->>QuestEngine: Apply Team to Quest
    QuestEngine->>Team: Check Captain
    Team-->>QuestEngine: Captain confirmed

    QuestEngine->>Quest: Check registration availability
    Quest-->>QuestEngine: Registration available

    QuestEngine->>Registration: Create PENDING registration
    Registration-->>QuestEngine: Registration created

    QuestEngine-->>Captain: Application submitted
```

---

# 3. Подтверждение команды автором

```mermaid
sequenceDiagram
    actor Author
    participant QuestEngine
    participant Registration

    Author->>QuestEngine: Approve registration
    QuestEngine->>Registration: APPROVED
    Registration-->>QuestEngine: Approved

    QuestEngine-->>Author: Team approved
```

Подтверждение может произойти как до старта Quest, так и после его начала.

---

# 4. Старт Quest

```mermaid
sequenceDiagram
    participant Scheduler
    participant Quest
    participant QuestEngine

    Scheduler->>QuestEngine: startDateTime reached
    QuestEngine->>Quest: RUNNING

    loop For every APPROVED registration
        QuestEngine->>QuestEngine: Create QuestProgress
        QuestEngine->>QuestEngine: Create first LevelProgress
    end

    QuestEngine->>QuestEngine: Start live statistics
```

Игровое время начинает отсчитываться для всех участников одновременно.

---

# 5. Поздний вход команды

```mermaid
sequenceDiagram
    actor Team
    participant QuestEngine
    participant QuestProgress
    participant LevelProgress

    Team->>QuestEngine: Enter Quest
    QuestEngine->>QuestProgress: Load progress
    QuestProgress->>LevelProgress: Get current level

    QuestEngine->>LevelProgress: Calculate remaining time
    LevelProgress-->>QuestEngine: Remaining time

    QuestEngine-->>Team: Show current level
```

Время входа команды не изменяет время начала первого уровня.

---

# 6. Успешное прохождение уровня кодами

```mermaid
sequenceDiagram
    actor Team
    participant QuestEngine
    participant LevelProgress
    participant Code

    Team->>QuestEngine: Submit code
    QuestEngine->>Code: Validate code

    Code-->>QuestEngine: Correct

    QuestEngine->>LevelProgress: Record CodeSubmission

    alt Required number of codes reached
        QuestEngine->>LevelProgress: Finish level
        QuestEngine->>QuestEngine: Create next LevelProgress
        QuestEngine-->>Team: Show next level
    else More codes required
        QuestEngine-->>Team: Continue level
    end
```

---

# 7. Неправильный код

```mermaid
sequenceDiagram
    actor Team
    participant QuestEngine
    participant Code

    Team->>QuestEngine: Submit code
    QuestEngine->>Code: Validate code
    Code-->>QuestEngine: Incorrect

    QuestEngine->>QuestEngine: Record rejected submission
    QuestEngine-->>Team: Code rejected
```

Неправильный код не завершает уровень.

---

# 8. Автопереход

```mermaid
sequenceDiagram
    participant Timer
    participant QuestEngine
    participant LevelProgress

    Timer->>QuestEngine: Level timer expired
    QuestEngine->>LevelProgress: AUTO_TRANSITION
    LevelProgress-->>QuestEngine: Level finished

    QuestEngine->>QuestEngine: Create next LevelProgress
```

---

# 9. Подсказка

```mermaid
sequenceDiagram
    participant Timer
    participant QuestEngine
    participant LevelProgress
    participant HintProgress

    Timer->>QuestEngine: Hint unlock time reached
    QuestEngine->>LevelProgress: Check active level
    QuestEngine->>HintProgress: Record hint opened
    QuestEngine-->>QuestEngine: Apply hint effect
```

Подсказка открывается независимо для каждой команды.

---

# 10. Завершение Quest командой

```mermaid
sequenceDiagram
    actor Team
    participant QuestEngine
    participant LevelProgress
    participant QuestProgress
    participant Statistics

    Team->>QuestEngine: Complete final level
    QuestEngine->>LevelProgress: FINISHED
    QuestEngine->>QuestProgress: FINISHED

    QuestEngine->>Statistics: Recalculate ranking
    Statistics-->>QuestEngine: Current result

    QuestEngine-->>Team: Quest completed
```

---

# 11. Принудительное завершение Quest автором

```mermaid
sequenceDiagram
    actor Author
    participant QuestEngine
    participant Quest
    participant QuestProgress
    participant Statistics

    Author->>QuestEngine: Finish Quest
    QuestEngine->>Quest: FINISHED

    loop For unfinished QuestProgress
        QuestEngine->>QuestProgress: DNF
    end

    QuestEngine->>Statistics: Calculate final results
    Statistics-->>QuestEngine: Final ranking

    QuestEngine-->>Author: Quest finished
```

---

# 12. Общая последовательность игры

```mermaid
sequenceDiagram
    actor Author
    actor Captain
    participant QuestEngine
    participant Quest
    participant Registration
    participant QuestProgress
    participant LevelProgress
    participant Statistics

    Author->>QuestEngine: Create Quest
    QuestEngine->>Quest: DRAFT

    Author->>QuestEngine: Publish Quest
    QuestEngine->>Quest: REGISTRATION

    Captain->>QuestEngine: Submit registration
    QuestEngine->>Registration: PENDING

    Author->>QuestEngine: Approve registration
    QuestEngine->>Registration: APPROVED

    Note over QuestEngine: Start time reached

    QuestEngine->>Quest: RUNNING

    QuestEngine->>QuestProgress: Create for approved teams
    QuestEngine->>LevelProgress: Start Level 1

    loop Until QuestProgress finished
        QuestProgress->>LevelProgress: Active level

        alt Correct codes
            LevelProgress->>LevelProgress: FINISHED
        else Timer expired
            LevelProgress->>LevelProgress: AUTO_TRANSITION
        end

        QuestEngine->>Statistics: Update ranking
    end

    QuestProgress->>QuestProgress: FINISHED

    QuestEngine->>Statistics: Calculate final ranking

    Note over QuestEngine: Quest finished

    QuestEngine->>Quest: FINISHED
```

---

# Important runtime principle

Время начала Quest является общим для всех команд.

Время входа команды в Quest не является началом её игрового времени.

```text
Quest start
     │
     ├── Team A enters +10 sec
     │
     ├── Team B enters +5 min
     │
     └── Team C enters +30 min
```

Все команды участвуют в одном и том же соревновании и используют общий момент старта.

Разница во времени входа не должна создавать преимущества.

---

# Important isolation principle

Команды не взаимодействуют с runtime-состоянием друг друга.

```text
                 Quest
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   QuestProgress QuestProgress QuestProgress
      Team A        Team B        Team C
        │             │             │
     Levels        Levels        Levels
```

Общее только описание Quest и правила игры.

Состояние прохождения полностью индивидуально.
