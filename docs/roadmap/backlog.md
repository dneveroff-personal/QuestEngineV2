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
| Quest CRUD, статусы, lifecycle | `01-domain/quest.md` | 🔵 | CRUD и lifecycle реализованы |
| Level CRUD | `01-domain/level.md`, ADR-0005 | 🔵 | `requiredMainCodesCount` реализован |
| Team, Captain, membership | `01-domain/team.md` | 🔵 | Реализовано |
| QuestRegistration | `01-domain/registration.md` | 🔵 | approve/reject, лимит команд |
| DNF для команды | `01-domain/progress.md` | 🔵 | finishQuest → DNF незавершённым; ручной setDnf только при Quest=FINISHED (в т.ч. FINISHED→DNF) |
| Late registration | `01-domain/registration.md` | 🔵 | REGISTRATION + RUNNING; DRAFT/FINISHED/archived — нельзя |
| Quest soft-delete (`archived`) | `01-domain/quest.md` | 🔵 | DELETE → archived=true; publish/finish/update запрещены |
| Team username + displayName | Team DTOs | 🔵 | username + displayName |
| Team quest registrations | `/teams/.../quests` | 🔵 | включая FINISHED |
| `GET /api/users/me` | endpoints | 🔵 | |
| Current level (Game Mode) | `.../current-level` | 🔵 | |
| Access + refresh tokens | ADR-0015 | 🔵 | |
| Rate limiting | ADR-0016 | 🔵 | |
| CI / JaCoCo / k6 | ADR-0017 | 🔵 | без fail-build по coverage |

## Текущие задачи

### 1. Качество и тестирование

1. 🔵 Контрактные тесты API (403/404/409, PageResponse).
2. 🟡 Runtime Bonus/Penalty — агрегат трёх источников.
3. ⚪ Повтор CodeSubmission после потери соединения.
4. ⚪ **Тесты по бизнес-правилам 14–16** — за Odissey (setDnf precondition, late registration, archive).

### 3. API для frontend / Game Mode

*(закрыт)*

### 4. Бизнес-правила

*(п. 14–16 решены в коде и спеке; тесты — за Odissey)*

14. 🔵 **DNF** — finishQuest автоматически DNF незавершённым; ручной setDnf только при `Quest.status = FINISHED` (дисквалификация FINISHED→DNF разрешена).
15. 🔵 **Регистрация** — `REGISTRATION` и `RUNNING`; запрет в `DRAFT`, `FINISHED`, archived.
16. 🔵 **Удаление Quest** — soft-delete `archived`; unarchive не в MVP.

### 5. Домен и функциональность

17. ⚪ **Прокинуть `Quest.maximumTeams` в DTO**
18. ⚪ **Statistics / Ranking**
19. ⚪ **Live-статистика через SSE** (ADR-0014)

### 6. Технический долг

20. ⚪ Deprecated `LevelProgressServiceImpl.autoTransitionLevel()`
21. ⚪ Свести `progress.md` и `runtime.md`

## Осознанно отложено

- 💤 Персональные подсказки / ручное открытие подсказки автором.
- 💤 Unarchive Quest.

## Итог

**Немедленных блокеров нет.** Фокус: `maximumTeams` в DTO, статистика или тесты 14–16 (Odissey).
