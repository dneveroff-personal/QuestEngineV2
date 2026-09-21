# Roadmap / Backlog

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟣 Decision · 💤 Deferred

## Состояние

| Механика | Статус |
|---|---|
| Ranking snapshot `GET .../statistics` | 🔵 |
| **Live statistics SSE** (ADR-0014) | 🔵 |
| maximumTeams / archive / DNF / late reg | 🔵 |
| Game Mode APIs | 🔵 |

## Текущие задачи

### 1. Качество
2. 🟡 Runtime Bonus/Penalty — агрегат трёх источников.
3. ⚪ Повтор CodeSubmission.
4. ⚪ Тесты ranking/SSE — по мере необходимости.

### 6. Технический долг
20. ⚪ Deprecated `LevelProgressServiceImpl.autoTransitionLevel()`
21. ⚪ Свести `progress.md` и `runtime.md`

## Осознанно отложено
- 💤 WebSocket игрового процесса (ADR-022) — отдельный трек.
- 💤 Unarchive / персональные подсказки.

## Итог
**#19 SSE готов.** Следующее: техдолг #20–21 или качество Bonus/Penalty.
