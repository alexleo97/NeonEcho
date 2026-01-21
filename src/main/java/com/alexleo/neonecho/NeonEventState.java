package com.alexleo.neonecho;

public final class NeonEventState {
    public String id;
    public String name;
    public String description;
    public String type;
    public Long expiresAt;
    public Integer bonusCred;
    public Integer dropCred;
    public Integer usesRemaining;

    public NeonEventState() {
    }

    public boolean isExpired(long now) {
        return expiresAt != null && now >= expiresAt;
    }

    public NeonEventState copy() {
        NeonEventState copy = new NeonEventState();
        copy.id = id;
        copy.name = name;
        copy.description = description;
        copy.type = type;
        copy.expiresAt = expiresAt;
        copy.bonusCred = bonusCred;
        copy.dropCred = dropCred;
        copy.usesRemaining = usesRemaining;
        return copy;
    }
}
