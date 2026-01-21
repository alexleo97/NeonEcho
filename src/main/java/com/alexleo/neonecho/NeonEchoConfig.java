package com.alexleo.neonecho;

import java.util.ArrayList;
import java.util.List;

public final class NeonEchoConfig {
    public String theme;
    public String prefix;
    public String joinMessage;

    public Integer chatCred;
    public Integer chatCredCooldownSeconds;
    public Integer chatMinChars;

    public Integer onlineCred;
    public Integer onlineCredIntervalSeconds;

    public Integer netrunCooldownSeconds;
    public Integer netrunTimeoutSeconds;
    public Integer netrunCodeLength;
    public Integer netrunAttempts;
    public Integer netrunReward;
    public Integer netrunFailPenalty;

    public String netrunDefaultTier;
    public List<NetrunTierConfig> netrunTiers;

    public Integer credTopLimit;

    public List<TitleRank> titles;

    public Boolean dailyEnabled;
    public Integer dailyReward;
    public Integer dailyObjectivesPerDay;
    public Boolean dailyRandomizeObjectives;
    public List<DailyObjectiveConfig> dailyObjectivePool;

    public List<String> netrunStartLines;
    public List<String> netrunSuccessLines;
    public List<String> netrunFailLines;
    public List<String> netrunCooldownLines;
    public List<String> netrunHintLines;

    public static NeonEchoConfig defaultConfig(NeonEchoTheme theme) {
        NeonEchoConfig config = new NeonEchoConfig();
        config.theme = theme.getName();
        config.prefix = "";
        config.joinMessage = "";
        config.chatCred = 1;
        config.chatCredCooldownSeconds = 30;
        config.chatMinChars = 6;
        config.onlineCred = 1;
        config.onlineCredIntervalSeconds = 300;
        config.netrunCooldownSeconds = 90;
        config.netrunTimeoutSeconds = 20;
        config.netrunCodeLength = 4;
        config.netrunAttempts = 2;
        config.netrunReward = 5;
        config.netrunFailPenalty = 1;
        config.netrunDefaultTier = "easy";
        config.netrunTiers = defaultNetrunTiers(config);
        config.credTopLimit = 5;
        config.titles = defaultTitles();
        config.dailyEnabled = true;
        config.dailyReward = 8;
        config.dailyObjectivesPerDay = 3;
        config.dailyRandomizeObjectives = false;
        config.dailyObjectivePool = defaultDailyObjectives();
        config.netrunStartLines = new ArrayList<>(theme.getNetrunStartLines());
        config.netrunSuccessLines = new ArrayList<>(theme.getNetrunSuccessLines());
        config.netrunFailLines = new ArrayList<>(theme.getNetrunFailLines());
        config.netrunCooldownLines = new ArrayList<>(theme.getNetrunCooldownLines());
        config.netrunHintLines = new ArrayList<>(theme.getNetrunHintLines());
        return config;
    }

    public void applyDefaults(NeonEchoTheme theme) {
        if (this.theme == null || this.theme.isBlank()) {
            this.theme = theme.getName();
        }
        if (this.prefix == null) {
            this.prefix = "";
        }
        if (this.joinMessage == null) {
            this.joinMessage = "";
        }
        if (this.chatCred == null) {
            this.chatCred = 1;
        }
        if (this.chatCredCooldownSeconds == null) {
            this.chatCredCooldownSeconds = 30;
        }
        if (this.chatMinChars == null) {
            this.chatMinChars = 6;
        }
        if (this.onlineCred == null) {
            this.onlineCred = 1;
        }
        if (this.onlineCredIntervalSeconds == null) {
            this.onlineCredIntervalSeconds = 300;
        }
        if (this.netrunCooldownSeconds == null) {
            this.netrunCooldownSeconds = 90;
        }
        if (this.netrunTimeoutSeconds == null) {
            this.netrunTimeoutSeconds = 20;
        }
        if (this.netrunCodeLength == null) {
            this.netrunCodeLength = 4;
        }
        if (this.netrunAttempts == null) {
            this.netrunAttempts = 2;
        }
        if (this.netrunReward == null) {
            this.netrunReward = 5;
        }
        if (this.netrunFailPenalty == null) {
            this.netrunFailPenalty = 1;
        }
        if (this.netrunDefaultTier == null || this.netrunDefaultTier.isBlank()) {
            this.netrunDefaultTier = "easy";
        }
        if (this.netrunTiers == null || this.netrunTiers.isEmpty()) {
            this.netrunTiers = defaultNetrunTiers(this);
        }
        if (this.credTopLimit == null) {
            this.credTopLimit = 5;
        }
        if (this.titles == null || this.titles.isEmpty()) {
            this.titles = defaultTitles();
        }
        if (this.dailyEnabled == null) {
            this.dailyEnabled = true;
        }
        if (this.dailyReward == null) {
            this.dailyReward = 8;
        }
        if (this.dailyObjectivesPerDay == null) {
            this.dailyObjectivesPerDay = 3;
        }
        if (this.dailyRandomizeObjectives == null) {
            this.dailyRandomizeObjectives = false;
        }
        if (this.dailyObjectivePool == null || this.dailyObjectivePool.isEmpty()) {
            this.dailyObjectivePool = defaultDailyObjectives();
        }
        if (this.netrunStartLines == null) {
            this.netrunStartLines = new ArrayList<>(theme.getNetrunStartLines());
        }
        if (this.netrunSuccessLines == null) {
            this.netrunSuccessLines = new ArrayList<>(theme.getNetrunSuccessLines());
        }
        if (this.netrunFailLines == null) {
            this.netrunFailLines = new ArrayList<>(theme.getNetrunFailLines());
        }
        if (this.netrunCooldownLines == null) {
            this.netrunCooldownLines = new ArrayList<>(theme.getNetrunCooldownLines());
        }
        if (this.netrunHintLines == null) {
            this.netrunHintLines = new ArrayList<>(theme.getNetrunHintLines());
        }
    }

    private static List<NetrunTierConfig> defaultNetrunTiers(NeonEchoConfig config) {
        List<NetrunTierConfig> tiers = new ArrayList<>();
        tiers.add(new NetrunTierConfig("easy", config.netrunCodeLength, 25, 3, 4, 0, 60));
        tiers.add(new NetrunTierConfig("medium", 5, 20, 2, 8, 1, 90));
        tiers.add(new NetrunTierConfig("hard", 6, 18, 2, 12, 2, 120));
        return tiers;
    }

    private static List<TitleRank> defaultTitles() {
        List<TitleRank> titles = new ArrayList<>();
        titles.add(new TitleRank("Rookie", 0));
        titles.add(new TitleRank("Runner", 25));
        titles.add(new TitleRank("Wire Ghost", 50));
        titles.add(new TitleRank("Neon Phantom", 100));
        titles.add(new TitleRank("Legend", 200));
        return titles;
    }

    private static List<DailyObjectiveConfig> defaultDailyObjectives() {
        List<DailyObjectiveConfig> objectives = new ArrayList<>();
        objectives.add(new DailyObjectiveConfig("chat", 5, "Send {target} chat messages"));
        objectives.add(new DailyObjectiveConfig("netrun", 1, "Complete {target} netrun"));
        objectives.add(new DailyObjectiveConfig("online", 10, "Stay online for {target} minutes"));
        return objectives;
    }

    public static final class NetrunTierConfig {
        public String name;
        public Integer codeLength;
        public Integer timeoutSeconds;
        public Integer attempts;
        public Integer reward;
        public Integer failPenalty;
        public Integer cooldownSeconds;

        public NetrunTierConfig() {
        }

        public NetrunTierConfig(
                String name,
                Integer codeLength,
                Integer timeoutSeconds,
                Integer attempts,
                Integer reward,
                Integer failPenalty,
                Integer cooldownSeconds) {
            this.name = name;
            this.codeLength = codeLength;
            this.timeoutSeconds = timeoutSeconds;
            this.attempts = attempts;
            this.reward = reward;
            this.failPenalty = failPenalty;
            this.cooldownSeconds = cooldownSeconds;
        }
    }

    public static final class TitleRank {
        public String title;
        public Integer minCred;

        public TitleRank() {
        }

        public TitleRank(String title, Integer minCred) {
            this.title = title;
            this.minCred = minCred;
        }
    }

    public static final class DailyObjectiveConfig {
        public String type;
        public Integer target;
        public String label;

        public DailyObjectiveConfig() {
        }

        public DailyObjectiveConfig(String type, Integer target, String label) {
            this.type = type;
            this.target = target;
            this.label = label;
        }
    }
}
