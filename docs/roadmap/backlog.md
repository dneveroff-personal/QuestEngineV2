# Roadmap / Backlog

Статусы: 🔵 Done · 🟡 In Progress · ⚪ Planned · 🟣 Decision · 💤 Deferred · 🔴 Bug

## Состояние

| Механика | Статус | Комментарий |
|---|---|---|
| Quest CRUD / lifecycle / archive / maximumTeams | 🔵 | |
| Registration + late reg / DNF | 🔵 | |
| Game Mode APIs (current-level, team quests) | 🔵 | Backend готов; frontend его не использует — см. п. 4 ниже |
| Statistics ranking snapshot | 🔵 | `GET /api/quests/{id}/statistics` |
| Live statistics SSE (ADR-0014) | 🔵 | `GET .../statistics/stream` — **merge PR #22** |
| Auth refresh / rate limit / CI | 🔵 | |
| Удалён unsafe `autoTransitionLevel` | 🔵 | только `tryAutoTransition` (Job 2) |
| `progress.md` / `runtime.md` | 🔵 | runtime = обзор, progress = детали |

## Текущие задачи

### 🔴 Баги (найдены при сверке 0.7.15 с документацией — приоритет выше "качества")

1. 🔴 **Гонка Сценария 3 не закрыта** (`02-processes/concurrency-scenarios.md`, Сценарий 3; `bonus-penalty.md`). `CodeSubmissionServiceImpl.submitCode` проверяет одноразовость BONUS/PENALTY-кода через `existsByQuestProgressIdAndMatchedCodeIdAndResult` (read-then-write) **без** частичного уникального индекса на `code_submissions(level_progress_id, matched_code_id) WHERE result IN ('CORRECT_BONUS','CORRECT_PENALTY')`, который сам документ называет обязательным решением. Два параллельных ввода одного и того же bonus-кода с разных устройств команды могут оба пройти проверку и оба начислить эффект дважды — тот же класс бага, что и Сценарии 1/2, но не исправлен. Тест `submitCode_bonusCodeCanOnlyBeAppliedOnce` — последовательный (два запроса подряд), не конкурентный, гонку не ловит. **Нужна миграция + тест по образцу `CodeSubmissionControllerIT`/Сценарий 6 (потоки + `CountDownLatch`).**
2. 🔴 **Frontend: `getShownHints` бьёт по несуществующему URL.** `frontend/src/api/hints.ts` вызывает `GET /api/quests/{questId}/teams/{teamId}/hints`, а реальный маршрут (`Routes.QUEST_PROGRESS_HINTS`) — `GET /api/quests/progress/{questId}/{teamId}/hints`. `ShownHintsList` (используется в `GamePage`) в рантайме получит 404 на каждый опрос — список подсказок в Game Mode никогда не загрузится.
3. 🔴 **Frontend: нет UI для взятия BONUS/PENALTY-подсказки.** Backend полностью реализует ADR-0021 (`POST .../hints/{hintId}/take`, три состояния видимости в `GET .../hints`), но во frontend нет ни одного вызова `/take` (проверено по всему `frontend/src`) и `ShownHintsList` не различает "доступна, не взята" от "показана" — рисует только уже показанные подсказки. Команда физически не может воспользоваться бонусными/штрафными подсказками через UI.
4. 🔴 **Frontend не использует уже готовый `GET .../current-level`.** `api/gameplay.ts` содержит рабочую функцию `getCurrentLevel()` (title/content/autoTransitionAt/hints одним запросом), но `GamePage.tsx` её не вызывает — вместо этого там комментарий, что "backend не отдаёт название/содержимое уровня", который устарел (эндпоинт уже есть и реализован). Команда во время игры не видит ни текста задания текущего уровня, ни таймера автоперехода.

### Качество (не блокеры)

5. 🟡 Runtime Bonus/Penalty — полный integration-путь трёх источников (частично перекрывается п. 1 выше — атомарность одного из трёх источников).
6. ⚪ Повтор CodeSubmission после потери соединения (контракт).
7. ⚪ Тесты ranking/SSE / 14–16 — по мере необходимости (Odissey). **Уточнение: `StatisticsServiceImpl`/`StatisticsController`/`StatisticsSseHub` не имеют вообще ни одного теста (0 файлов), как и `CurrentLevelServiceImpl`/`CurrentLevelService` (Game Mode агрегирующий эндпоинт) — оба сейчас непроверены совсем, не просто "недостаточно".**
8. ⚪ Нет `TeamController`-эндпоинта переименования команды. `01-domain/team.md` явно описывает это как полномочие капитана ("Переименование", отдельный раздел с примером) — в коде ни `TeamService`, ни `TeamController` такого действия не содержат.
9. ⚪ Статистика попыток ввода кода не реализована как API. `statistics-ranking.md` и `code-submission.md` явно описывают видимость: автору — полная статистика попыток всех команд, команде — только своя на активном уровне. В коде нет ни одного эндпоинта, отдающего список/сводку `CodeSubmission` (кроме самого `POST .../codes`) — фича описана в доменной модели, но не существует как API.
10. ⚪ `Level.requiredMainCodesCount` не валидируется относительно фактического числа MAIN-кодов уровня — ни при создании/редактировании уровня, ни при `validateQuestPublishable`. Можно опубликовать уровень с порогом, который физически недостижим (например, требуется 5 кодов при 3 настроенных) — по духу это тот же случай, что и уже отклоняемый "аномальный уровень" (ADR-0005), но не покрыт проверкой.
11. ⚪ `GlobalExceptionHandler.timestamp` — `LocalDateTime.now()` вместо `Instant`, конвенция сама фиксирует это как известное расхождение "исправить при следующей правке файла" (`04-api/conventions.md`) — правка ещё не сделана.

### Отложено

- 💤 WebSocket игрового процесса (ADR-022).
- 💤 Unarchive / персональные подсказки.
- 💤 Детальная статистика попыток кодов автору (см. также п. 9 — теперь это не только "детальная", а вообще отсутствующая базовая версия).

## Итог

**Доменный backlog MVP закрыт по фичам, но сверка 0.7.15 с документацией нашла 4 реальных дефекта (п. 1–4) и 7 задокументированных, но не реализованных или не покрытых тестами мест (п. 5–11).** Ни один из них не был известен до этой сверки — раньше не встречались в backlog.
Приоритет: сначала п. 1 (гонка, повреждение игровых данных) и п. 2–4 (Game Mode на frontend фактически недоступен: нет текста уровня, нет таймера, нет подсказок, нет взятия бонус/штраф-подсказок) — это основной пользовательский сценарий игры.
Открытый PR: **#22 SSE** — смержить, если ещё не в main.
