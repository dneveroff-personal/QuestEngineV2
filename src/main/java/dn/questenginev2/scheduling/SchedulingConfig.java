package dn.questenginev2.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Включает поддержку {@code @Scheduled} для Job 1 (старт Quest) и Job 2 (автопереход уровня).
 * См. docs/03-architecture/scheduling.md.
 *
 * <p>Single-instance MVP (docs/06-nfr/requirements.md, "Доступность") — распределённая блокировка
 * (ShedLock) намеренно не подключена, отложена до перехода на несколько инстансов приложения.
 *
 * <p>Отключается в тестах (см. {@code scheduling.enabled=false} в тестовых application.yml) —
 * иначе таймер (раз в 1 секунду) фоном работал бы во время ВСЕХ {@code @SpringBootTest}, включая
 * не связанные с планировщиком, создавая риск непредсказуемого вмешательства в данные других
 * тестов. Тесты самого планировщика вызывают методы Job 1/Job 2 напрямую, не полагаясь на таймер.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    prefix = "scheduling",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
