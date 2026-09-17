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
| Level CRUD | `01-domain/level.md`, ADR-0005 | 🔵 | `requiredMainCodesCount` реализован; `codeIndex` относится к `Code`, а не к `Level` |
| Team, Captain, membership | `01-domain/team.md` | 🔵 | Реализовано |
| QuestRegistration | `01-domain/registration.md` | 🔵 | Регистрация, approve/reject и лимит команд реализованы; подтверждённая гонка закрыта |
| Автоматический старт Quest (Job 1) | `01-domain/progress.md`, ADR-002 | 🔵 | `QuestStartScheduler`, атомарный переход, конкурентный тест |
| Публикация Quest | `02-processes/quest-lifecycle.md` | 🔵 | `POST /api/quests/{id}/publish`, включая проверку конфигурации уровней |
| Завершение Quest автором | `02-processes/quest-lifecycle.md` | 🔵 | `POST /api/quests/{id}/finish`, незавершённые QuestProgress получают DNF |
| Автопереход уровня (Job 2) | `03-architecture/scheduling.md` | 🔵 | Атомарный переход, конкурентный тест Job 2 vs CodeSubmission |
| Оркестрация завершения уровня / QuestProgress | ADR-0009 | 🔵 | `advanceAfterLevelCompleted()` используется реальными точками входа |
| Hint CRUD и runtime | ADR-0020, ADR-0021 | 🔵 | REGULAR auto-reveal, BONUS/PENALTY take, три состояния видимости |
| Code CRUD | ADR-0004, ADR-0005 | 🔵 | Уникальность `code_value` в пределах Level и `codeIndex` реализованы |
| CodeSubmission | ADR-0004/0005/0006 | 🔵 | Аудит попыток, нормализация, атомарный порог, unit/controller/IT и конкурентные тесты |
| Bonus/Penalty | ADR-0007 | 🟡 | Все три источника и агрегация реализованы; есть unit-тесты агрегатора, но ещё нужна проверка полного runtime/API-контракта и одноразового эффекта BONUS/PENALTY-кода |
| Permissions / Security | `05-security/permissions.md` | 🔵 | Базовая ролевая модель реализована |
| HTTP error semantics / pagination | ADR-0011, ADR-0012 | 🟡 | Реализованы `403/404/409` и `PageResponse<T>`; отдельное контрактное покрытие ещё стоит усилить |
| DNF для команды | `01-domain/registration.md`, `progress.md` | 🟡 | `PUT /api/quests/progress/{questId}/{teamId}/dnf` есть; бизнес-семантика момента вызова ещё требует решения |
| Rate limiting | ADR-0016 | 🔵 | `LoginRateLimitFilter`, 5/мин на IP; CodeSubmission намеренно не ограничивается |
| CI | `.github/workflows/build.yml` | 🔵 | Spotless, тесты, Docker image, GHCR, JaCoCo/test artifacts |
| JaCoCo + k6 smoke (Сценарий 6) | ADR-0017, `07-quality/testing-strategy.md` | 🔵 | ADR-0017 amended: жёсткий coverage threshold/fail-build отменены; JaCoCo отчёт и k6 smoke добавлены как проверочные инструменты, k6 не является CI-gate |

## Текущие задачи

### 1. Качество и тестирование

1. 🔵 **Расширить контрактные тесты API**
   - 🔵 отдельно проверить семантику `403/404/409`;
   - 🔵 проверить `PageResponse<T>` для listing endpoints;
   - 🔵 не дублировать уже существующие domain/integration tests.

2. ⚪ **Проверить runtime-семантику Bonus/Penalty**
   - покрыть API/integration путь ручной корректировки;
   - проверить одноразовое применение BONUS/PENALTY-кода;
   - проверить агрегат кода + подсказки + ручной корректировки вместе.

3. ⚪ **Проверить повторную отправку CodeSubmission после потери соединения**
   - отдельно зафиксировать контракт для повтора после успешного завершения уровня и перехода на следующий;
   - не возвращаться автоматически к `CodeSubmissionOperation`/HTTP-idempotency ledger: сначала проверить, достаточно ли текущей бизнес-модели и какого минимального контракта не хватает.

### 2. Auth / Security

4. ⚪ **Перейти на access + refresh tokens (ADR-0015)**
   - access token 15 минут;
   - refresh token в БД;
   - rotation;
   - `/api/auth/refresh` и `/api/auth/logout`;
   - обновить frontend и документацию.

5. ⚪ **Разобраться с диагностическим `GET /api/test/secure`**
   - удалить перед первым публичным релизом либо явно оставить и документировать назначение.

6. ⚪ **Проверить безопасность `/api/users/search`**
   - решить, кто имеет право искать пользователей;
   - определить, какие поля (`email`, `role`, и т.п.) доступны не-ADMIN;
   - зафиксировать решение в threat model и тестах.

7. ⚪ **Убрать публикацию PostgreSQL `5432` из production compose**
   - БД должна быть доступна приложению через внутреннюю Docker-сеть;
   - публичный вход в production остаётся через frontend/nginx.

### 3. API для frontend / Game Mode

8. ⚪ **Добавить `GET /api/users/me`**
   - вернуть профиль текущего пользователя;
   - frontend должен получать `id`, `publicName`, `email`, `role` без обходных запросов.

9. ⚪ **Добавить получение квестов/регистраций текущей команды**
   - убрать N+1 запросы frontend через `upcoming`;
   - вернуть также прошедшие (`FINISHED`) квесты.

10. ⚪ **Определить отображаемое имя команды**
    - решить `username` vs `publicName` для `TeamMemberDto.name` и `TeamResponse.captainName`;
    - зафиксировать единое правило и привести API/frontend к нему.

11. ⚪ **Добавить `username` в `UserResponse` для поиска пользователей**
    - frontend должен однозначно идентифицировать результат поиска при передаче капитанства.

12. ⚪ **Добавить `authorId` / `authorName` в `QuestResponse`**
    - frontend должен понимать авторство без обходного поиска через `/users/search`.

13. ⚪ **Добавить API для текущего уровня команды**
    - упростить Game Mode: текущий `LevelProgress`, содержимое уровня, `autoTransitionAt` и доступные подсказки должны быть получаемы одним понятным контрактом.

### 4. Бизнес-правила, требующие решения

14. 🟣 **DNF: определить прекондицию**
    - вариант A: ручной `setDnf()` разрешён только после `Quest.status = FINISHED`;
    - вариант B: `finishQuest()` автоматически переводит все незавершённые команды в DNF, а ручной endpoint не нужен;
    - после решения обновить `progress.md`, `registration.md`, API и тесты.

15. 🟣 **Регистрация команды: разрешённые статусы Quest**
    - сейчас backend разрешает регистрацию во всех статусах кроме `FINISHED`, а frontend показывает её только для `REGISTRATION`;
    - определить, должен ли backend принимать только `REGISTRATION` или поддерживать позднюю регистрацию как отдельный сценарий.

16. 🟣 **Удаление Quest**
    - определить, разрешено ли удаление `RUNNING`/`FINISHED` квестов;
    - если нет — запретить на backend или ввести soft-delete/архивирование.

### 5. Домен и функциональность

17. ⚪ **Прокинуть `Quest.maximumTeams` в DTO**
    - добавить поле в create/update/response;
    - дать автору возможность задавать лимит, а клиенту — видеть его.

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

- 💤 Персональные подсказки.
- 💤 Ручное досрочное открытие подсказки автором.
- 💤 Подробный список отличий QuestEngine от Encounter — можно уточнять по мере развития продукта, это не блокирует текущую разработку.

## Отдельно: production CD

CD намеренно не включён до выхода в production. Поэтому исправление `deploy.yml` сейчас **не является задачей текущего backlog**. Когда появится реальный production deployment, отдельным deployment-заходом нужно проверить trigger/`if`, рабочую директорию и весь production compose.

## Итог

**Немедленных блокеров сейчас нет.**

Следующий рабочий фокус — API/Game Mode и security. После них — статистика/SSE и технический долг документации. JaCoCo/k6 остаются уже готовым инструментарием качества, а не отдельным блокером.
