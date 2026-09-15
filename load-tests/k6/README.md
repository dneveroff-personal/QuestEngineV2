# k6 smoke tests (optional)

**Not a CI gate.** Concurrent correctness of Scenario 6 is already covered by the Java IT test with many threads. These scripts exercise the same path over real HTTP and are meant for manual runs before a release.

## Prerequisites

1. Install [k6](https://k6.io/docs/get-started/installation/).
2. Running API (local compose or staging).
3. A quest in `RUNNING` with a team that has an active `LevelProgress`, and a valid code value for that level.
4. A JWT for a team member (current auth model).

## Scenario 6 — concurrent code threshold

```bash
export BASE_URL=http://localhost:8080
export TOKEN='<jwt>'
export QUEST_ID=1
export TEAM_ID=1
export CODE_VALUE='YOUR_CODE'
export VUS=20
export DURATION=15s

k6 run load-tests/k6/scenario6-code-threshold.js
```

What to look at:

- HTTP success/error mix (duplicates and already-used codes are expected under concurrency).
- Application logs / DB: level should complete **once**; accepted unique main codes should not be lost (same invariant as the IT race test).

## Why not required in CI?

- Needs seeded game state and credentials.
- Overlaps with existing concurrent IT.
- Useful as a pre-release check, not as a merge blocker.
