package com.boxing.bracket.audit.service;

public final class AuditActorContext {

    private static final ThreadLocal<AuditActor> ACTOR = new ThreadLocal<>();

    private AuditActorContext() {
    }

    public static void set(AuditActor actor) {
        ACTOR.set(actor);
    }

    public static AuditActor get() {
        return ACTOR.get();
    }

    public static void clear() {
        ACTOR.remove();
    }
}
