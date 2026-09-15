# Frontend Realtime

## 1. Назначение

QuestEngine использует REST как основной API и отдельные realtime-транспорты
для сценариев, которым нужен push от backend.

Realtime является частью API boundary, но не содержит бизнес-логику.
Backend остаётся единственным источником истины.

## 2. Transport Strategy

```text
Frontend
   │
   ├── REST / HTTP
   │      └── initial state, queries, mutations, CRUD
   │
   ├── SSE
   │      └── live statistics
   │
   └── WebSocket / STOMP
          └── gameplay realtime events
```

REST используется для первоначальной загрузки состояния, обычных queries и
mutations, авторизации, Quest, Level, Team, Registration, QuestProgress,
LevelProgress и получения актуального состояния.

SSE используется только для live statistics согласно ADR-0014.

WebSocket используется для server-driven событий игрового режима.

WebSocket **не заменяет REST** и **не заменяет SSE для statistics**.

## 3. Source of Truth

Realtime-событие сообщает, что состояние изменилось. Оно не становится
самостоятельным источником истины.

```text
REST
  │
  └── current state

SSE / WebSocket
  │
  └── state changed / event
           │
           ▼
    TanStack Query cache
           │
           ▼
           UI
```

Если событие содержит полный актуальный state, frontend может записать его
в соответствующий TanStack Query cache.

Для gameplay на текущем этапе предпочтительны **события-уведомления**:
WebSocket сообщает об изменении состояния, а frontend инвалидирует/refetch
соответствующий REST query. Это уменьшает дублирование DTO и не превращает
WebSocket в второй API состояния.

## 4. WebSocket / STOMP Contract

### 4.1 Connection endpoint

Production WebSocket endpoint:

```text
/ws
```

Используется STOMP поверх WebSocket. Spring поддерживает STOMP как
sub-protocol для WebSocket и application/broker destinations.

### 4.2 Destinations

На текущем этапе фиксируем следующие destinations:

```text
SUBSCRIBE /topic/quests/{questId}/gameplay
SUBSCRIBE /topic/quest-progress/{questProgressId}/gameplay
SUBSCRIBE /user/queue/gameplay
```

Назначение:

* `/topic/quests/{questId}/gameplay` — события, видимые участникам
  конкретного Quest; используется для общего игрового состояния;
* `/topic/quest-progress/{questProgressId}/gameplay` — события конкретного
  командного прохождения; доступ только участникам соответствующего
  QuestProgress и пользователям с соответствующими правами;
* `/user/queue/gameplay` — персональные события текущего пользователя.

Spring STOMP поддерживает `/user/` destinations для адресной доставки
сообщений конкретному пользователю.

Frontend не выбирает destination исходя только из UI-роли. Backend проверяет
право пользователя на QuestProgress/Quest при подключении и подписке.

### 4.3 Server → Client events

Минимальный production-набор:

```text
CODE_ACCEPTED
CODE_REJECTED
LEVEL_COMPLETED
LEVEL_AUTO_TRANSITIONED
HINT_REVEALED
QUEST_FINISHED
```

События являются уведомлениями об изменении состояния. Payload должен быть
минимальным и содержать идентификаторы/метаданные, необходимые для выбора
query для refetch.

Например:

```json
{
  "type": "LEVEL_COMPLETED",
  "version": 1,
  "eventId": "01J...",
  "questId": 123,
  "questProgressId": 456,
  "levelProgressId": 789,
  "occurredAt": "2026-09-15T10:20:30.123Z",
  "payload": {
    "levelId": 12
  }
}
```

Обязательные поля:

* `type` — тип события;
* `version` — версия схемы события;
* `eventId` — уникальный ID события;
* `questId` — Quest;
* `questProgressId` — командное прохождение, если применимо;
* `occurredAt` — серверное время возникновения;
* `payload` — event-specific данные.

`eventId` нужен для защиты frontend от повторной обработки одного и того же
события при reconnect/re-delivery.

### 4.4 Event visibility

```text
Quest-wide
  → /topic/quests/{questId}/gameplay

Team / QuestProgress
  → /topic/quest-progress/{questProgressId}/gameplay

Personal
  → /user/queue/gameplay
```

Нельзя отправлять team-specific события в Quest-wide destination только для
удобства frontend. Visibility является частью backend authorization.

### 4.5 Client → Server

Gameplay mutations остаются REST operations.

WebSocket на первом этапе используется только как **server → client push**.
Клиент не отправляет через STOMP команды изменения игрового состояния.

```text
Code submit / hint action / other mutation
                │
                ▼
               REST
                │
                ▼
             Backend
                │
       ┌────────┴────────┐
       ▼                 ▼
 REST response      WebSocket event
```

Это позволяет сохранить один authoritative mutation path и не дублировать
бизнес-логику между REST Controller и WebSocket `@MessageMapping`.

## 5. Initial State + Events

Realtime-клиент не должен пытаться восстановить состояние только по потоку
событий.

При входе в Game Mode:

```text
Open Game Mode
      │
      ▼
GET current QuestProgress
      │
      ▼
GET current LevelProgress / related state
      │
      ▼
Render current state
      │
      ▼
Connect WebSocket
      │
      ▼
Subscribe to required events
```

После получения gameplay event frontend инвалидирует соответствующий
TanStack Query и получает authoritative state через REST.

Это также является основным способом восстановления состояния после
reconnect.

## 6. Event Processing

Для каждого события frontend:

```text
receive event
     │
     ├── already processed eventId? → ignore
     │
     └── new event
            │
            ▼
      mark eventId processed
            │
            ▼
      invalidate/refetch query
```

Event ordering не используется как источник истины для состояния.
Если события пришли в другом порядке или часть событий была потеряна,
REST refetch восстанавливает актуальное состояние.

## 7. TanStack Query Integration

Realtime-события должны обновлять тот же server state, который используется
обычными queries.

Например:

```text
WebSocket LEVEL_COMPLETED
        │
        ▼
invalidate
["gameplay", questProgressId]
        │
        ▼
GET current gameplay state
        │
        ▼
TanStack Query cache
```

Для statistics используется отдельный SSE lifecycle:

```text
GET /statistics/{questId}
        │
        ▼
["statistics", questId]
        ▲
        │
SSE STATISTICS_UPDATED
```

Frontend не создаёт отдельное глобальное состояние только для realtime.

## 8. Reconnection

WebSocket-клиент должен:

* автоматически восстанавливать соединение после временного разрыва;
* повторять необходимые subscriptions;
* не создавать повторно бизнес-операции только из-за reconnect;
* показывать состояние realtime connection в Game Mode, если потеря
  соединения влияет на игровой UX.

После reconnect frontend выполняет REST refetch authoritative gameplay state.
Reconnect не должен повторять пользовательские mutations.

SSE для statistics использует отдельный lifecycle и не смешивается с
игровым WebSocket lifecycle.

## 9. Business-level idempotency

Проблема доставки команды и проблема доставки события различаются.

Для code submission нам не требуется отдельный технический журнал HTTP-
операций или replay исходного REST response. Повторная отправка кода должна
быть безопасной за счёт бизнес-правил самого gameplay:

* каждая попытка сохраняется в `CodeSubmission` как история;
* повторный правильный основной код не увеличивает количество решённых
  кодов, потому что прогресс считается по distinct `codeIndex`;
* завершение `LevelProgress` выполняется атомарно и может произойти только
  один раз;
* переход на следующий уровень выполняется только после успешного
  завершения текущего `LevelProgress`;
* если первый запрос завершил уровень, а ответ потерялся, повторный запрос
  не должен повторно завершать уже завершённый уровень.

Таким образом, повтор запроса после потери соединения не требует отдельной
`CodeSubmissionOperation`-сущности.

`Idempotency-Key` остаётся возможным инструментом для будущих операций, где
повторное выполнение действительно опасно или дорого, например финансовых
операций, импорта или создания внешнего ресурса. Для code submission он
сейчас не используется.

## 10. Timer

Realtime не используется для передачи countdown каждую секунду.

Backend передаёт абсолютный timestamp:

```text
autoTransitionAt
```

Frontend вычисляет:

```text
remaining = autoTransitionAt - currentTime
```

и обновляет только визуальное отображение.

Истечение локального countdown не изменяет игровое состояние.
Фактический переход выполняет backend, после чего frontend получает новое
состояние через REST и/или WebSocket event.

## 11. Security

Backend авторизует:

* WebSocket connection;
* subscriptions;
* доступ к конкретным Quest/Team/QuestProgress;
* server-side event visibility.

Spring Security предоставляет механизм авторизации STOMP-сообщений через
message-channel interception.

Frontend не считает наличие realtime connection доказательством доступа.

## 12. Scope

На текущем этапе:

* SSE остаётся transport для live statistics;
* WebSocket/STOMP является transport для gameplay realtime;
* REST остаётся основным API и authoritative state transport;
* WebSocket используется только для server → client gameplay events;
* code submission не использует отдельный HTTP idempotency ledger;
* точные event payloads уточняются при реализации соответствующего backend
  use case.

Production implementation WebSocket выполняется после фиксации backend
contract и соответствующих ADR.
