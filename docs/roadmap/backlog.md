# Roadmap / Backlog

Единый рабочий список: что уже реализовано и проверено, что ещё нужно сделать. Завершённые находки и разовые заметки сюда не переносятся — после исправления они исчезают из backlog.

Статусы:
- 🔵 Done — реализовано и покрыто соответствующими тестами.
- 🟡 In Progress — работа начата, но ещё не завершена.
- ⚪ Planned — задача ещё не реализована.
- 🟣 Decision — требуется сначала принять бизнес-решение.
- 💤 Deferred — осознанно отложено за пределы MVP.

## Состояние реализованной части

| Механика | Спецификация | Статус | Комментарий |
|---|---|---|---|
| Quest CRUD, статусы, lifecycle | `01-domain/quest.md` | 🔵 | |
| `Quest.maximumTeams` в DTO | create/update/response | 🔵 | default 100; Min 1 / Max 10000 |
| Level CRUD | ADR-0005 | 🔵 | |
| Team, membership, displayName | team.md | 🔵 | |
| QuestRegistration + late reg | registration.md | 🔵 | REGISTRATION + RUNNING |
| DNF | progress.md | 🔵 | finishQuest + manual after FINISHED |
| Soft-delete archive | quest.md | 🔵 | |
| Current level API | Game Mode | 🔵 | |
| Team quests API | `/teams/.../quests` | 🔵 | |
| Access + refresh | ADR-0015 | 🔵 | |
| Rate limiting | ADR-0016 | 🔵 | |
| CI / JaCoCo / k6 | ADR-0017 | 🔵 | |

## Текущие задачи

### 1. Качество и тестирование

1. 🔵 Контрактные тесты API.
2. 🟡 Runtime Bonus/Penalty — агрегат трёх источников.
3. ⚪ Повтор CodeSubmission после потери соединения.
4. ⚪ Тесты по бизнес-правилам 14–16 — за Odissey.

### 5. Домен и функциональность

18. ⚪ **Statistics / Ranking**
    - пакет `statistic/` по `01-domain/statistics-ranking.md`;
    - минимальный набор для игрового результата, затем live.

19. ⚪ **Live-статистика через SSE** (ADR-0014)

### 6. Технический долг

20. ⚪ Deprecated `LevelProgressServiceImpl.autoTransitionLevel()`
21. ⚪ Свести `progress.md` и `runtime.md`

## Осознанно отложено

- 💤 Персональные подсказки / unarchive.

## Итог

**Немедленных блокеров нет.** Следующий крупный блок — Statistics/Ranking (#18) или техдолг (#20–21). Тесты 14–16 — Odissey.
