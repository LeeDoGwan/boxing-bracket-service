package com.boxing.bracket.auth.domain;

/** Holds the authenticated request principal for service-layer authorization checks. */
public final class AuthSessionContext {

    private static final ThreadLocal<AuthSession> SESSION = new ThreadLocal<>();

    private AuthSessionContext() {
    }

    public static void set(AuthSession session) {
        SESSION.set(session);
    }

    public static AuthSession get() {
        return SESSION.get();
    }

    public static void clear() {
        SESSION.remove();
    }
}
