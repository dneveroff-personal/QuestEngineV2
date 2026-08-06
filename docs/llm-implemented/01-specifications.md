Проанализированы все сущности и их репозитории. Определены места, где Specifications дают наибольшую пользу — это динамические фильтры для поиска по комбинации полей.

## Реализованные Specifications
Созданы Specifications для всех сущностей проекта:

Сущность	    Specification	        Ключевые фильтры
Team	        TeamSpecification	    имя (частичное совпадение), капитан, дата создания
User	        UserSpecification	    username, email, роль, дата создания
TeamMember	    TeamMemberSpecification	команда, пользователь, роль, дата вступления
Hint	        HintSpecification	    уровень, порядковый индекс, задержка, контент, дата создания
Level	        LevelSpecification	    квест, название, порядковый индекс, таймаут, дата создания


## Интеграция в репозитории
Все репозитории расширены интерфейсом JpaSpecificationExecutor:
- TeamRepository
- UserRepository
- TeamMemberRepository
- HintRepository
- LevelRepository


## Новые REST endpoints
Добавлены endpoints для поиска с динамическими фильтрами:

- GET /api/quests/search — поиск квестов по статусу, типу, времени, названию, описанию
- GET /api/teams/search — поиск команд по имени, капитану, дате создания
- GET /api/users/search — поиск пользователей по username, email, роли, дате создания


## DTO для фильтров
Созданы DTO с валидацией:
- TeamFilterRequest
- UserFilterRequest

Результат
Все тесты проходят успешно (./gradlew test — BUILD SUCCESSFUL). Код отформатирован через Spotless. Specifications готовы к использованию и позволяют динамически комбинировать любые фильтры без написания новых методов в репозиториях.