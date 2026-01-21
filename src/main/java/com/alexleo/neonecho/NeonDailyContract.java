package com.alexleo.neonecho;

import java.util.ArrayList;
import java.util.List;

public final class NeonDailyContract {
    public String date;
    public Integer reward;
    public List<NeonDailyObjective> objectives = new ArrayList<>();
    public Boolean claimed;

    public NeonDailyContract() {
    }

    public NeonDailyContract(String date, Integer reward, List<NeonDailyObjective> objectives, Boolean claimed) {
        this.date = date;
        this.reward = reward;
        this.objectives = objectives;
        this.claimed = claimed;
    }
}
