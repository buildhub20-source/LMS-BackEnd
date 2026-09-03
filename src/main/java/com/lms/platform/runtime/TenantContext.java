package com.lms.platform.runtime;

import java.util.Optional;

/** Thread-local request context used by the routing datasource and JWT guard. */
public final class TenantContext {
    private static final ThreadLocal<TenantConnection> CURRENT = new ThreadLocal<>();
    private TenantContext() {}
    public static void set(TenantConnection tenant) { CURRENT.set(tenant); }
    public static Optional<TenantConnection> current() { return Optional.ofNullable(CURRENT.get()); }
    public static void clear() { CURRENT.remove(); }
}
