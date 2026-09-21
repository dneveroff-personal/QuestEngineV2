# Roadmap / Backlog

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟣 Decision · 💤 Deferred

## Состояние

| Механика | Статус | Комментарий |
|---|---|---|
| Quest CRUD / lifecycle / archive / maximumTeams | 🔵 | |
| Registration + late reg / DNF | 🔵 | |
| Game Mode APIs (current-level, team quests) | 🔵 | |
| Statistics ranking snapshot | 🔵 | `GET /api/quests/{id}/statistics` |
| Live statistics SSE (ADR-0014) | 🔵 | `GET .../statistics/stream` — **merge PR #22** |
| Auth refresh / rate limit / CI | 🔵 | |
| Удалён unsafe `autoTransitionLevel` | 🔵 | только `tryAutoTransition` (Job 2) |
| `progress.md` / `runtime.md` | 🔵 | runtime = обзор, progress = детали |

## Текущие задачи

### Качество (не блокеры)

1. 🟡 Runtime Bonus/Penalty — полный integration-путь трёх источников.
2. ⚪ Повтор CodeSubmission после потери соединения (контракт).
3. ⚪ Тесты ranking/SSE / 14–16 — по мере необходимости (Odissey).

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Детальная статистика попыток кодов автору.

## Итог

**Доменный backlog MVP закрыт** (кроме опционального качества тестов).  
Открытый PR: **#22 SSE** — смержить, если ещё не в main.
