package com.alexleo.neonecho;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NeonEchoState {
    private final String name;
    private final String version;
    private final long startMillis;
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();

    public NeonEchoState(String name, String version) {
        this.name = name;
        this.version = version;
        this.startMillis = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Duration getUptime() {
        long elapsed = System.currentTimeMillis() - startMillis;
        return Duration.ofMillis(Math.max(0L, elapsed));
    }

    public boolean isMuted(UUID playerId) {
        return mutedPlayers.contains(playerId);
    }

    public boolean toggleMuted(UUID playerId) {
        if (mutedPlayers.add(playerId)) {
            return true;
        }
        mutedPlayers.remove(playerId);
        return false;
    }
}
