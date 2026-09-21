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
3. ⚪ Повтор CodeSubmission после потери соединения.
4. ⚪ **Тесты по бизнес-правилам 14–16** — за Odissey (setDnf precondition, late registration, archive).

### 5. Домен и функциональность

18. ⚪ **Statistics / Ranking**
    - реализовать пакет `statistic/` согласно `01-domain/statistics-ranking.md`;
    - начать с минимального набора данных, необходимого для игрового результата, затем расширять live-статистику.

19. ⚪ **Live-статистика через SSE**
    - реализовать ADR-0014;
    - определить события/данные, которые действительно нужны статистике;
    - подключить frontend без polling.

### 6. Технический долг

20. ⚪ **Удалить/заменить deprecated `LevelProgressServiceImpl.autoTransitionLevel()`**
    - оставить единственную безопасную конкурентную реализацию через атомарный repository method;
    - после миграции тестов удалить deprecated метод.

21. ⚪ **Свести `progress.md` и `runtime.md`**
    - убрать дублирование правил QuestProgress/LevelProgress и `autoTransitionAt`;
    - оставить один источник истины.

## Осознанно отложено
- 💤 WebSocket игрового процесса (ADR-022) — отдельный трек.
- 💤 Unarchive / персональные подсказки.

## Итог
**#19 SSE готов.** Следующее: техдолг #20–21 или качество Bonus/Penalty.
