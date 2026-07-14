package com.boxing.bracket.audit.service;

public class AuditSnapshot {

    private final Long tournamentId;
    private final Long ringId;
    private final Long boutId;
    private final Long targetId;
    private final String beforeData;

    public AuditSnapshot(Long tournamentId, Long ringId, Long boutId, Long targetId, String beforeData) {
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutId = boutId;
        this.targetId = targetId;
        this.beforeData = beforeData;
    }

    public static AuditSnapshot empty() {
        return new AuditSnapshot(null, null, null, null, null);
    }

    public Long getTournamentId() { return tournamentId; }
    public Long getRingId() { return ringId; }
    public Long getBoutId() { return boutId; }
    public Long getTargetId() { return targetId; }
    public String getBeforeData() { return beforeData; }
}
