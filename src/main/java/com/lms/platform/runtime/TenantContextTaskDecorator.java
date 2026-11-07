package com.lms.platform.runtime;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * Carries the current institute into {@code @Async} work.
 *
 * <p>{@link TenantContext} is thread-local, so a task handed to the shared
 * executor (learning mails, reminder mails, webhooks) would otherwise run with
 * no institute: links in those mails would point at the platform address, and
 * anything reading the institute's database there would fail. Spring Boot
 * applies a single {@link TaskDecorator} bean to its task executor.
 */
@Component
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        TenantConnection tenant = TenantContext.current().orElse(null);
        return () -> {
            if (tenant == null) {
                task.run();
                return;
            }
            TenantContext.set(tenant);
            try {
                task.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
