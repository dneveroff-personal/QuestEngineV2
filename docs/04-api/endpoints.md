# API Endpoints — обзор

## Statistics

| Метод | Путь | Статус |
|---|---|---|
| GET | `/api/quests/{questId}/statistics` | 🔵 *(snapshot ranking; SSE = backlog #19)* |

Доступно при `Quest.status` ∈ {RUNNING, FINISHED}. Аутентификация обязательна.

### Ranking rules (кратко)
- **RUNNING:** только команды с ≥1 завершённым уровнем; больше уровней выше; ничья — кто раньше закрыл последний завершённый уровень.
- **FINISHED:** FINISHED по `totalTimeSeconds` (стена + bonus/penalty); DNF внизу.

Остальные ресурсы — см. предыдущие ревизии / OpenAPI.
