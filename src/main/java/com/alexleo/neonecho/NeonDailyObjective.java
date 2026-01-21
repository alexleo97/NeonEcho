package com.alexleo.neonecho;

public final class NeonDailyObjective {
    public String type;
    public Integer target;
    public String label;

    public NeonDailyObjective() {
    }

    public NeonDailyObjective(String type, Integer target, String label) {
        this.type = type;
        this.target = target;
        this.label = label;
    }
}
