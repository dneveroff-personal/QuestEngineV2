# API Endpoints — Statistics

| Метод | Путь | Статус |
|---|---|---|
| GET | `/api/quests/{questId}/statistics` | 🔵 snapshot |
| GET | `/api/quests/{questId}/statistics/stream` | 🔵 SSE (event `statistics`) |

Auth: Bearer **или** query `access_token` (для native EventSource).

При каждом изменении ranking (enter / level complete / finish / DNF / finishQuest) клиентам уходит полный snapshot.
