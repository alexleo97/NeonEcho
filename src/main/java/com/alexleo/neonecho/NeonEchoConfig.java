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
    public Integer netrunStages;
    public Integer netrunStageLengthDelta;
    public Integer netrunStageTimeoutDelta;
    public Integer netrunStageRewardBonus;
    public Integer netrunStageCooldownBonus;

    public String netrunDefaultTier;
    public List<NetrunTierConfig> netrunTiers;
    public List<RiskProfileConfig> netrunRisks;

    public Integer credTopLimit;

    public List<TitleRank> titles;

    public Boolean perksEnabled;
    public Integer perkSlots;
    public List<PerkConfig> perks;

    public Boolean eventsEnabled;
    public Integer eventIntervalSeconds;
    public Double eventChance;
    public List<EventConfig> events;

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
        config.netrunStages = 3;
        config.netrunStageLengthDelta = 1;
        config.netrunStageTimeoutDelta = -2;
        config.netrunStageRewardBonus = 2;
        config.netrunStageCooldownBonus = 10;
        config.netrunDefaultTier = "easy";
        config.netrunTiers = defaultNetrunTiers(config);
        config.netrunRisks = defaultRiskProfiles();
        config.credTopLimit = 5;
        config.titles = defaultTitles();
        config.perksEnabled = true;
        config.perkSlots = 2;
        config.perks = defaultPerks();
        config.eventsEnabled = true;
        config.eventIntervalSeconds = 300;
        config.eventChance = 0.35;
        config.events = defaultEvents();
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
        if (this.netrunStages == null) {
            this.netrunStages = 3;
        }
        if (this.netrunStageLengthDelta == null) {
            this.netrunStageLengthDelta = 1;
        }
        if (this.netrunStageTimeoutDelta == null) {
            this.netrunStageTimeoutDelta = -2;
        }
        if (this.netrunStageRewardBonus == null) {
            this.netrunStageRewardBonus = 2;
        }
        if (this.netrunStageCooldownBonus == null) {
            this.netrunStageCooldownBonus = 10;
        }
        if (this.netrunDefaultTier == null || this.netrunDefaultTier.isBlank()) {
            this.netrunDefaultTier = "easy";
        }
        if (this.netrunTiers == null || this.netrunTiers.isEmpty()) {
            this.netrunTiers = defaultNetrunTiers(this);
        }
        if (this.netrunRisks == null || this.netrunRisks.isEmpty()) {
            this.netrunRisks = defaultRiskProfiles();
        }
        if (this.credTopLimit == null) {
            this.credTopLimit = 5;
        }
        if (this.titles == null || this.titles.isEmpty()) {
            this.titles = defaultTitles();
        }
        if (this.perksEnabled == null) {
            this.perksEnabled = true;
        }
        if (this.perkSlots == null) {
            this.perkSlots = 2;
        }
        if (this.perks == null || this.perks.isEmpty()) {
            this.perks = defaultPerks();
        }
        if (this.eventsEnabled == null) {
            this.eventsEnabled = true;
        }
        if (this.eventIntervalSeconds == null) {
            this.eventIntervalSeconds = 300;
        }
        if (this.eventChance == null) {
            this.eventChance = 0.35;
        }
        if (this.events == null || this.events.isEmpty()) {
            this.events = defaultEvents();
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

    private static List<RiskProfileConfig> defaultRiskProfiles() {
        List<RiskProfileConfig> profiles = new ArrayList<>();
        profiles.add(new RiskProfileConfig("safe", 0.8, 0.75, 1.2, 0.5, 1.2));
        profiles.add(new RiskProfileConfig("normal", 1.0, 1.0, 1.0, 1.0, 1.0));
        profiles.add(new RiskProfileConfig("risky", 1.4, 1.25, 0.85, 1.5, 0.9));
        return profiles;
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

    private static List<PerkConfig> defaultPerks() {
        List<PerkConfig> perks = new ArrayList<>();
        perks.add(new PerkConfig("signal_booster", "Signal Booster",
                "Reduce netrun cooldowns by 15%.", 25, 2,
                0.85, null, null, null, null));
        perks.add(new PerkConfig("ghost_protocol", "Ghost Protocol",
                "Gain +1 attempt per netrun stage.", 30, 2,
                null, null, null, 1, null));
        perks.add(new PerkConfig("overclock", "Overclock",
                "Increase netrun payout by 20%, but reduce time by 10%.", 35, 2,
                null, 1.2, 0.9, null, null));
        perks.add(new PerkConfig("fail_safe", "Fail Safe",
                "Reduce fail penalty by 1.", 20, 1,
                null, null, null, null, 1));
        perks.add(new PerkConfig("stacked_deck", "Stacked Deck",
                "Increase netrun payout by 10%.", 15, 3,
                null, 1.1, null, null, null));
        return perks;
    }

    private static List<EventConfig> defaultEvents() {
        List<EventConfig> events = new ArrayList<>();
        events.add(new EventConfig("district_alarm", "District Alarm",
                "Netrun payouts spike while alarms blare.", "netrun_bonus",
                300, 4, 0, 3));
        events.add(new EventConfig("signal_cache", "Signal Cache",
                "A cache drops. Claim it before it fades.", "drop",
                600, 0, 10, 1));
        events.add(new EventConfig("skyline_chase", "Skyline Chase",
                "Fast runs pay extra cred for a short window.", "netrun_bonus",
                240, 6, 0, 2));
        return events;
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

    public static final class RiskProfileConfig {
        public String name;
        public Double rewardMultiplier;
        public Double cooldownMultiplier;
        public Double timeoutMultiplier;
        public Double failPenaltyMultiplier;
        public Double attemptsMultiplier;

        public RiskProfileConfig() {
        }

        public RiskProfileConfig(
                String name,
                Double rewardMultiplier,
                Double cooldownMultiplier,
                Double timeoutMultiplier,
                Double failPenaltyMultiplier,
                Double attemptsMultiplier) {
            this.name = name;
            this.rewardMultiplier = rewardMultiplier;
            this.cooldownMultiplier = cooldownMultiplier;
            this.timeoutMultiplier = timeoutMultiplier;
            this.failPenaltyMultiplier = failPenaltyMultiplier;
            this.attemptsMultiplier = attemptsMultiplier;
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

    public static final class PerkConfig {
        public String id;
        public String name;
        public String description;
        public Integer cost;
        public Integer maxRank;
        public Double cooldownMultiplier;
        public Double rewardMultiplier;
        public Double timeoutMultiplier;
        public Integer attemptBonus;
        public Integer failPenaltyReduction;

        public PerkConfig() {
        }

        public PerkConfig(
                String id,
                String name,
                String description,
                Integer cost,
                Integer maxRank,
                Double cooldownMultiplier,
                Double rewardMultiplier,
                Double timeoutMultiplier,
                Integer attemptBonus,
                Integer failPenaltyReduction) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.cost = cost;
            this.maxRank = maxRank;
            this.cooldownMultiplier = cooldownMultiplier;
            this.rewardMultiplier = rewardMultiplier;
            this.timeoutMultiplier = timeoutMultiplier;
            this.attemptBonus = attemptBonus;
            this.failPenaltyReduction = failPenaltyReduction;
        }
    }

    public static final class EventConfig {
        public String id;
        public String name;
        public String description;
        public String type;
        public Integer durationSeconds;
        public Integer bonusCred;
        public Integer dropCred;
        public Integer maxTriggers;

        public EventConfig() {
        }

        public EventConfig(
                String id,
                String name,
                String description,
                String type,
                Integer durationSeconds,
                Integer bonusCred,
                Integer dropCred,
                Integer maxTriggers) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.durationSeconds = durationSeconds;
            this.bonusCred = bonusCred;
            this.dropCred = dropCred;
            this.maxTriggers = maxTriggers;
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
