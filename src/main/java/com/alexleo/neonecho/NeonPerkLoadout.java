package com.alexleo.neonecho;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NeonPerkLoadout {
    public Map<String, Integer> owned = new ConcurrentHashMap<>();
    public List<String> active = new ArrayList<>();

    public void normalize() {
        if (owned == null) {
            owned = new ConcurrentHashMap<>();
        }
        if (active == null) {
            active = new ArrayList<>();
        }
    }

    public NeonPerkLoadout copy() {
        NeonPerkLoadout copy = new NeonPerkLoadout();
        copy.owned.putAll(owned);
        copy.active.addAll(active);
        return copy;
    }
}
