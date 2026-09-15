# ADR-0016: Realtime transport boundaries

## Status

Accepted

## Context

QuestEngine uses realtime updates in different parts of the application. The
transport must be selected by use case rather than introducing WebSocket as a
replacement for every push mechanism.

Encounter-style gameplay benefits from low-latency bidirectional communication,
while live statistics can remain a simple server-to-client stream.

## Decision

QuestEngine uses **two realtime transports with explicit boundaries**:

### SSE — live statistics

Server-Sent Events remain the transport for live quest statistics.

```text
Statistics page
      │
      ▼
     SSE
      │
      ▼
TanStack Query cache
```

SSE is one-way and is sufficient because the statistics client does not need to
send realtime commands over the same connection.

The existing statistics SSE decision from ADR-0014 remains valid.

### WebSocket — gameplay realtime

WebSocket is reserved for realtime game events and future gameplay
synchronization.

```text
Game Mode
   │
   ├── REST → initial/current state and normal mutations
   │
   └── WebSocket → realtime game events
```

Examples of WebSocket events:

* `LEVEL_COMPLETED`
* `LEVEL_AUTO_TRANSITIONED`
* `HINT_REVEALED`
* `CODE_ACCEPTED`
* `CODE_REJECTED`
* `QUEST_FINISHED`

WebSocket does not replace REST and does not become a source of domain truth.

## Consequences

Positive:

* statistics remain simple and compatible with the existing SSE implementation;
* gameplay gets a proper realtime channel;
* frontend does not need polling for game events;
* transport choice is explicit and easy to evolve;
* future backend microservices can publish gameplay events behind the same API
  boundary.

Negative:

* the frontend has two realtime mechanisms to maintain;
* gameplay WebSocket infrastructure requires authentication, reconnect and
  subscription handling.

## Implementation direction

For the gameplay WebSocket implementation, Spring STOMP over WebSocket is the
preferred protocol unless a later ADR changes this decision. Spring provides
message destinations, subscriptions, authentication and authorization around
STOMP without requiring QuestEngine to invent a custom messaging protocol.

The exact endpoint, destinations, event envelope and subscription rules must
be defined in `docs/frontend/realtime.md` before implementation.

## Summary

```text
REST
 ├── CRUD
 ├── initial/current state
 └── normal mutations

SSE
 └── live statistics

WebSocket
 └── gameplay realtime events
```
