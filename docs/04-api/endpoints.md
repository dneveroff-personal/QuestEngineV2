# API Endpoints — обзор ресурсов

Статус: 🟡 Draft.

---

## Auth — `/api/auth`

| Метод | Путь | Статус |
|---|---|---|
| POST | `/auth/login` | 🔵 |
| POST | `/auth/register` | 🔵 |
| POST | `/auth/refresh` | 🔵 |
| POST | `/auth/logout` | 🔵 |

## Users / Teams

См. предыдущие ревизии — `/users/me`, team displayName, `/teams/.../quests` — 🔵.

## Quests — `/api/quests`

| Метод | Путь | Статус |
|---|---|---|
| POST/GET/PUT | `/quests`, `/quests/{id}`, ... | 🔵 |
| DELETE | `/quests/{questId}` | 🔵 *(soft-delete → `archived=true`, данные сохраняются)* |
| POST | `/quests/{id}/publish` | 🔵 *(запрещено для archived)* |
| POST | `/quests/{id}/finish` | 🔵 *(→ DNF незавершённым; запрещено для archived)* |

## Quest Registration

| Метод | Путь | Статус |
|---|---|---|
| POST | `/quests/register/{questId}/{teamId}` | 🔵 *(REGISTRATION + RUNNING; DRAFT/FINISHED/archived — нет)* |
| GET/DELETE/approve/reject | ... | 🔵 |

## Quest Progress

| Метод | Путь | Статус |
|---|---|---|
| GET | `.../current-level` | 🔵 |
| PUT | `.../dnf` | 🔵 *(только Quest.status=FINISHED; FINISHED→DNF = дисквалификация)* |
| enter / codes / hints | ... | 🔵 |

## Сводка пробелов

1. Statistics / Ranking.
2. `Quest.maximumTeams` в DTO.
3. Тесты по 14–16 (Odissey).
