# Roadmap / Backlog

Единый рабочий список: что уже реализовано и проверено, что ещё нужно сделать.

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟣 Decision · 💤 Deferred

## Состояние реализованной части

| Механика | Статус | Комментарий |
|---|---|---|
| Quest CRUD / lifecycle / archive / maximumTeams | 🔵 | |
| Registration + late reg / DNF | 🔵 | |
| Game Mode APIs (current-level, team quests) | 🔵 | |
| **Statistics ranking snapshot** | 🔵 | `GET /api/quests/{id}/statistics` — live + final rules |
| Auth refresh / rate limit / CI | 🔵 | |

## Текущие задачи

### 1. Качество и тестирование

1. 🔵 Контрактные тесты API.
2. 🟡 Runtime Bonus/Penalty — агрегат трёх источников.
3. ⚪ Повтор CodeSubmission после потери соединения.
4. ⚪ Тесты 14–16 и ranking — за Odissey / по мере необходимости.

### 5. Домен и функциональность

19. ⚪ **Live-статистика через SSE** (ADR-0014)
    - транспорт поверх уже готового snapshot ranking;
    - события при изменении QuestProgress / LevelProgress.

### 6. Технический долг

20. ⚪ Deprecated `LevelProgressServiceImpl.autoTransitionLevel()`
21. ⚪ Свести `progress.md` и `runtime.md`

## Осознанно отложено

- 💤 Персональные подсказки / unarchive.
- 💤 Детальная статистика попыток кода автору (отдельный endpoint при необходимости).

## Итог

**#18 ranking snapshot готов.** Следующее: SSE (#19) или техдолг (#20–21).
