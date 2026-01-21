package com.alexleo.neonecho;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class NeonEchoState {
    private final String name;
    private final String version;
    private final long startMillis;
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();

    private final HytaleLogger logger;
    private final Gson gson;
    private final Path dataDir;
    private final Path configPath;
    private final Path dataPath;

    private final Object dataLock = new Object();
    private NeonEchoData data = new NeonEchoData();
    private volatile NeonEchoConfig config;

    private final Map<UUID, Long> netrunCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, NetrunSession> netrunSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> chatCredCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();
    private final AtomicLong lastOnlineCredAt = new AtomicLong();

    public NeonEchoState(String name, String version, HytaleLogger logger, Path dataDir) {
        this.name = name;
        this.version = version;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataDir = dataDir;
        this.configPath = dataDir.resolve("config.json");
        this.dataPath = dataDir.resolve("data.json");
        this.startMillis = System.currentTimeMillis();
        ensureDataDirectory();
        NeonEchoTheme theme = NeonEchoThemes.get("neon");
        this.config = NeonEchoConfig.defaultConfig(theme);
    }

    public void load() {
        loadConfig();
        loadData();
        lastOnlineCredAt.set(System.currentTimeMillis());
    }

    public void loadConfig() {
        NeonEchoConfig loaded = null;
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                loaded = gson.fromJson(reader, NeonEchoConfig.class);
            }
            catch (IOException ex) {
                logger.at(Level.WARNING).log("Failed to read NeonEcho config, recreating defaults.");
            }
        }
        NeonEchoTheme theme = NeonEchoThemes.get(loaded != null ? loaded.theme : null);
        if (loaded == null) {
            loaded = NeonEchoConfig.defaultConfig(theme);
            saveConfig(loaded);
        }
        else {
            loaded.applyDefaults(theme);
        }
        this.config = loaded;
    }

    public boolean reloadConfig() {
        try {
            loadConfig();
            return true;
        }
        catch (Exception ex) {
            logger.at(Level.WARNING).log("Failed to reload NeonEcho config: " + ex.getMessage());
            return false;
        }
    }

    public void saveConfig() {
        saveConfig(config);
    }

    public void saveConfig(NeonEchoConfig toSave) {
        if (toSave == null) {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            gson.toJson(toSave, writer);
        }
        catch (IOException ex) {
            logger.at(Level.WARNING).log("Failed to write NeonEcho config: " + ex.getMessage());
        }
    }

    public void loadData() {
        NeonEchoData loaded = null;
        if (Files.exists(dataPath)) {
            try (Reader reader = Files.newBufferedReader(dataPath, StandardCharsets.UTF_8)) {
                loaded = gson.fromJson(reader, NeonEchoData.class);
            }
            catch (IOException ex) {
                logger.at(Level.WARNING).log("Failed to read NeonEcho data, recreating defaults.");
            }
        }
        if (loaded == null) {
            loaded = new NeonEchoData();
            saveData(loaded);
        }
        else {
            loaded.normalize();
        }
        synchronized (dataLock) {
            this.data = loaded;
        }
    }

    public void saveData() {
        NeonEchoData snapshot;
        synchronized (dataLock) {
            snapshot = data.copy();
        }
        saveData(snapshot);
    }

    public void saveData(NeonEchoData toSave) {
        if (toSave == null) {
            return;
        }
        try (Writer writer = Files.newBufferedWriter(dataPath, StandardCharsets.UTF_8)) {
            gson.toJson(toSave, writer);
        }
        catch (IOException ex) {
            logger.at(Level.WARNING).log("Failed to write NeonEcho data: " + ex.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public NeonEchoConfig getConfig() {
        return config;
    }

    public NeonEchoTheme getTheme() {
        NeonEchoConfig cfg = config;
        return NeonEchoThemes.get(cfg != null ? cfg.theme : null);
    }

    public String formatMessage(String message) {
        String prefix = getPrefix();
        if (prefix.isBlank()) {
            return message;
        }
        return prefix + " " + message;
    }

    public String getPrefix() {
        NeonEchoConfig cfg = config;
        String prefix = cfg != null ? cfg.prefix : null;
        if (prefix == null || prefix.isBlank()) {
            prefix = getTheme().getPrefix();
        }
        return prefix == null ? "" : prefix;
    }

    public String getJoinMessage() {
        NeonEchoConfig cfg = config;
        String joinMessage = cfg != null ? cfg.joinMessage : null;
        if (joinMessage == null || joinMessage.isBlank()) {
            joinMessage = getTheme().getJoinMessage();
        }
        return joinMessage;
    }

    public List<String> getNetrunStartLines() {
        return resolveLines(config != null ? config.netrunStartLines : null, getTheme().getNetrunStartLines());
    }

    public List<String> getNetrunSuccessLines() {
        return resolveLines(config != null ? config.netrunSuccessLines : null, getTheme().getNetrunSuccessLines());
    }

    public List<String> getNetrunFailLines() {
        return resolveLines(config != null ? config.netrunFailLines : null, getTheme().getNetrunFailLines());
    }

    public List<String> getNetrunCooldownLines() {
        return resolveLines(config != null ? config.netrunCooldownLines : null, getTheme().getNetrunCooldownLines());
    }

    public List<String> getNetrunHintLines() {
        return resolveLines(config != null ? config.netrunHintLines : null, getTheme().getNetrunHintLines());
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

    public void markOnline(UUID playerId, boolean online) {
        if (online) {
            onlinePlayers.add(playerId);
        }
        else {
            onlinePlayers.remove(playerId);
        }
    }

    public void recordPlayerName(UUID playerId, String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        synchronized (dataLock) {
            data.names.put(playerId.toString(), username);
        }
    }

    public int getCred(UUID playerId) {
        synchronized (dataLock) {
            return data.cred.getOrDefault(playerId.toString(), 0);
        }
    }

    public int addCred(UUID playerId, int amount) {
        if (amount == 0) {
            return getCred(playerId);
        }
        synchronized (dataLock) {
            String key = playerId.toString();
            int current = data.cred.getOrDefault(key, 0);
            int updated = Math.max(0, current + amount);
            data.cred.put(key, updated);
            return updated;
        }
    }

    public void setCred(UUID playerId, int amount) {
        synchronized (dataLock) {
            data.cred.put(playerId.toString(), Math.max(0, amount));
        }
    }

    public NetrunStats getNetrunStats(UUID playerId) {
        synchronized (dataLock) {
            String key = playerId.toString();
            return new NetrunStats(
                    data.netrunWins.getOrDefault(key, 0),
                    data.netrunFails.getOrDefault(key, 0),
                    data.netrunStreak.getOrDefault(key, 0),
                    data.netrunBestStreak.getOrDefault(key, 0)
            );
        }
    }

    public int recordNetrunWin(UUID playerId) {
        if (isDailyEnabled()) {
            ensureDailyContract(playerId);
        }
        synchronized (dataLock) {
            String key = playerId.toString();
            data.netrunWins.put(key, data.netrunWins.getOrDefault(key, 0) + 1);
            int streak = data.netrunStreak.getOrDefault(key, 0) + 1;
            data.netrunStreak.put(key, streak);
            int best = Math.max(data.netrunBestStreak.getOrDefault(key, 0), streak);
            data.netrunBestStreak.put(key, best);
            data.dailyNetrunWins.put(key, data.dailyNetrunWins.getOrDefault(key, 0) + 1);
            return streak;
        }
    }

    public void recordNetrunFail(UUID playerId) {
        synchronized (dataLock) {
            String key = playerId.toString();
            data.netrunFails.put(key, data.netrunFails.getOrDefault(key, 0) + 1);
            data.netrunStreak.put(key, 0);
        }
    }

    public List<CredEntry> getTopCred(int limit) {
        List<CredEntry> results = new ArrayList<>();
        synchronized (dataLock) {
            for (Map.Entry<String, Integer> entry : data.cred.entrySet()) {
                String name = data.names.getOrDefault(entry.getKey(), "Runner-" + entry.getKey().substring(0, 6));
                results.add(new CredEntry(entry.getKey(), name, entry.getValue()));
            }
        }
        results.sort(Comparator.comparingInt(CredEntry::cred).reversed());
        if (limit > 0 && results.size() > limit) {
            return new ArrayList<>(results.subList(0, limit));
        }
        return results;
    }

    public String getTitle(UUID playerId) {
        int cred = getCred(playerId);
        return getTitleForCred(cred);
    }

    public String getTitleForCred(int cred) {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.titles == null || cfg.titles.isEmpty()) {
            return "Runner";
        }
        String title = "Runner";
        for (NeonEchoConfig.TitleRank rank : cfg.titles) {
            if (rank == null || rank.title == null || rank.minCred == null) {
                continue;
            }
            if (cred >= rank.minCred) {
                title = rank.title;
            }
        }
        return title;
    }

    public boolean tryAwardChatCred(UUID playerId, String content) {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.chatCred == null || cfg.chatCred <= 0) {
            return false;
        }
        int minChars = cfg.chatMinChars != null ? cfg.chatMinChars : 0;
        if (content == null || content.trim().length() < minChars) {
            return false;
        }
        int cooldownSeconds = cfg.chatCredCooldownSeconds != null ? cfg.chatCredCooldownSeconds : 0;
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(0L, cooldownSeconds) * 1000L;
        Long lastAward = chatCredCooldowns.get(playerId);
        if (lastAward != null && now - lastAward < cooldownMillis) {
            return false;
        }
        chatCredCooldowns.put(playerId, now);
        addCred(playerId, cfg.chatCred);
        return true;
    }

    public void recordChat(UUID playerId) {
        if (!isDailyEnabled()) {
            return;
        }
        ensureDailyContract(playerId);
        synchronized (dataLock) {
            String key = playerId.toString();
            data.dailyChatCount.put(key, data.dailyChatCount.getOrDefault(key, 0) + 1);
        }
    }

    public void tickOnline(int seconds) {
        if (seconds <= 0) {
            return;
        }
        NeonEchoConfig cfg = config;
        if (cfg != null && cfg.onlineCred != null && cfg.onlineCred > 0) {
            int intervalSeconds = cfg.onlineCredIntervalSeconds != null ? cfg.onlineCredIntervalSeconds : 0;
            if (intervalSeconds > 0) {
                long now = System.currentTimeMillis();
                long last = lastOnlineCredAt.get();
                if (now - last >= intervalSeconds * 1000L && lastOnlineCredAt.compareAndSet(last, now)) {
                    for (UUID playerId : onlinePlayers) {
                        addCred(playerId, cfg.onlineCred);
                    }
                }
            }
        }
        if (!isDailyEnabled()) {
            return;
        }
        for (UUID playerId : onlinePlayers) {
            ensureDailyContract(playerId);
            synchronized (dataLock) {
                String key = playerId.toString();
                data.dailyOnlineSeconds.put(key, data.dailyOnlineSeconds.getOrDefault(key, 0) + seconds);
            }
        }
    }

    public boolean isDailyEnabled() {
        NeonEchoConfig cfg = config;
        return cfg != null && Boolean.TRUE.equals(cfg.dailyEnabled);
    }

    public void prepareDaily(UUID playerId) {
        ensureDailyContract(playerId);
    }

    public DailyContractView getDailyContractView(UUID playerId) {
        if (!isDailyEnabled()) {
            return DailyContractView.disabled();
        }
        NeonDailyContract contract = ensureDailyContract(playerId);
        if (contract == null) {
            return DailyContractView.disabled();
        }
        List<DailyObjectiveView> objectives = new ArrayList<>();
        for (NeonDailyObjective objective : contract.objectives) {
            DailyObjectiveView view = buildObjectiveView(playerId, objective);
            objectives.add(view);
        }
        boolean complete = objectives.stream().allMatch(DailyObjectiveView::complete);
        boolean claimed = Boolean.TRUE.equals(contract.claimed);
        int reward = contract.reward != null ? contract.reward : 0;
        return new DailyContractView(true, claimed, complete, reward, objectives);
    }

    public ClaimResult claimDaily(UUID playerId) {
        if (!isDailyEnabled()) {
            return ClaimResult.disabled();
        }
        NeonDailyContract contract = ensureDailyContract(playerId);
        if (contract == null) {
            return ClaimResult.noContract();
        }
        synchronized (dataLock) {
            if (Boolean.TRUE.equals(contract.claimed)) {
                return ClaimResult.alreadyClaimed();
            }
            List<DailyObjectiveView> objectives = new ArrayList<>();
            for (NeonDailyObjective objective : contract.objectives) {
                objectives.add(buildObjectiveView(playerId, objective));
            }
            boolean complete = objectives.stream().allMatch(DailyObjectiveView::complete);
            if (!complete) {
                return ClaimResult.notComplete();
            }
            contract.claimed = true;
            int reward = contract.reward != null ? contract.reward : 0;
            addCred(playerId, reward);
            return ClaimResult.claimed(reward);
        }
    }

    private DailyObjectiveView buildObjectiveView(UUID playerId, NeonDailyObjective objective) {
        int target = objective.target != null ? objective.target : 0;
        int progress = getObjectiveProgress(playerId, objective.type);
        boolean complete = progress >= target && target > 0;
        String label = formatObjectiveLabel(objective, target);
        return new DailyObjectiveView(label, progress, target, complete);
    }

    private int getObjectiveProgress(UUID playerId, String type) {
        String safeType = type == null ? "" : type.toLowerCase(Locale.ROOT);
        synchronized (dataLock) {
            String key = playerId.toString();
            return switch (safeType) {
                case "chat" -> data.dailyChatCount.getOrDefault(key, 0);
                case "netrun" -> data.dailyNetrunWins.getOrDefault(key, 0);
                case "online" -> data.dailyOnlineSeconds.getOrDefault(key, 0) / 60;
                default -> 0;
            };
        }
    }

    private String formatObjectiveLabel(NeonDailyObjective objective, int target) {
        String label = objective.label;
        if (label == null || label.isBlank()) {
            String type = objective.type == null ? "" : objective.type.toLowerCase(Locale.ROOT);
            label = switch (type) {
                case "chat" -> "Send {target} chat messages";
                case "netrun" -> "Complete {target} netrun";
                case "online" -> "Stay online for {target} minutes";
                default -> "Complete {target} objective";
            };
        }
        return label.replace("{target}", Integer.toString(target));
    }

    private NeonDailyContract ensureDailyContract(UUID playerId) {
        if (!isDailyEnabled()) {
            return null;
        }
        String today = LocalDate.now().toString();
        String key = playerId.toString();
        synchronized (dataLock) {
            NeonDailyContract contract = data.dailyContracts.get(key);
            if (contract == null || contract.date == null || !contract.date.equals(today)) {
                contract = createDailyContract(today);
                data.dailyContracts.put(key, contract);
                data.dailyChatCount.put(key, 0);
                data.dailyNetrunWins.put(key, 0);
                data.dailyOnlineSeconds.put(key, 0);
            }
            else {
                if (contract.claimed == null) {
                    contract.claimed = false;
                }
                if (contract.objectives == null || contract.objectives.isEmpty()) {
                    contract.objectives = createDailyContract(today).objectives;
                }
                if (contract.reward == null) {
                    contract.reward = config.dailyReward != null ? config.dailyReward : 0;
                }
            }
            return contract;
        }
    }

    private NeonDailyContract createDailyContract(String date) {
        NeonEchoConfig cfg = config;
        int reward = cfg != null && cfg.dailyReward != null ? cfg.dailyReward : 0;
        int perDay = cfg != null && cfg.dailyObjectivesPerDay != null ? cfg.dailyObjectivesPerDay : 3;
        boolean randomize = cfg != null && Boolean.TRUE.equals(cfg.dailyRandomizeObjectives);
        List<NeonEchoConfig.DailyObjectiveConfig> pool = cfg != null ? cfg.dailyObjectivePool : Collections.emptyList();
        List<NeonEchoConfig.DailyObjectiveConfig> selected = new ArrayList<>(pool);
        if (randomize) {
            Collections.shuffle(selected, ThreadLocalRandom.current());
        }
        if (perDay > 0 && selected.size() > perDay) {
            selected = new ArrayList<>(selected.subList(0, perDay));
        }
        List<NeonDailyObjective> objectives = new ArrayList<>();
        for (NeonEchoConfig.DailyObjectiveConfig entry : selected) {
            if (entry == null) {
                continue;
            }
            int target = entry.target != null ? entry.target : 0;
            if (target <= 0) {
                continue;
            }
            objectives.add(new NeonDailyObjective(entry.type, target, entry.label));
        }
        return new NeonDailyContract(date, reward, objectives, false);
    }

    public NetrunTier resolveNetrunTier(String name) {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.netrunTiers == null || cfg.netrunTiers.isEmpty()) {
            return new NetrunTier("default", 4, 20, 2, 5, 1, 90);
        }
        String requested = name != null ? name.trim().toLowerCase(Locale.ROOT) : null;
        String fallbackName = cfg.netrunDefaultTier != null ? cfg.netrunDefaultTier.toLowerCase(Locale.ROOT) : null;
        NeonEchoConfig.NetrunTierConfig fallback = cfg.netrunTiers.get(0);
        for (NeonEchoConfig.NetrunTierConfig tier : cfg.netrunTiers) {
            if (tier != null && tier.name != null && tier.name.trim().equalsIgnoreCase(requested)) {
                return toTier(tier);
            }
            if (tier != null && tier.name != null && tier.name.trim().equalsIgnoreCase(fallbackName)) {
                fallback = tier;
            }
        }
        return toTier(fallback);
    }

    public List<String> getNetrunTierNames() {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.netrunTiers == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (NeonEchoConfig.NetrunTierConfig tier : cfg.netrunTiers) {
            if (tier != null && tier.name != null && !tier.name.isBlank()) {
                names.add(tier.name);
            }
        }
        return names;
    }

    private NetrunTier toTier(NeonEchoConfig.NetrunTierConfig tier) {
        String name = tier != null && tier.name != null ? tier.name : "default";
        int codeLength = tier != null && tier.codeLength != null ? tier.codeLength : 4;
        int timeout = tier != null && tier.timeoutSeconds != null ? tier.timeoutSeconds : 20;
        int attempts = tier != null && tier.attempts != null ? tier.attempts : 2;
        int reward = tier != null && tier.reward != null ? tier.reward : 5;
        int failPenalty = tier != null && tier.failPenalty != null ? tier.failPenalty : 1;
        int cooldown = tier != null && tier.cooldownSeconds != null ? tier.cooldownSeconds : 90;
        return new NetrunTier(name, codeLength, timeout, attempts, reward, failPenalty, cooldown);
    }

    public NetrunSession getActiveSession(UUID playerId) {
        NetrunSession session = netrunSessions.get(playerId);
        if (session == null) {
            return null;
        }
        if (session.isExpired(System.currentTimeMillis())) {
            netrunSessions.remove(playerId);
            return null;
        }
        return session;
    }

    public void startSession(UUID playerId, String code, long expiresAt, int attempts, NetrunTier tier) {
        netrunSessions.put(playerId, new NetrunSession(code, expiresAt, attempts, tier));
    }

    public void clearSession(UUID playerId) {
        netrunSessions.remove(playerId);
    }

    public long getCooldownRemainingMillis(UUID playerId) {
        long now = System.currentTimeMillis();
        Long until = netrunCooldowns.get(playerId);
        if (until == null) {
            return 0L;
        }
        long remaining = until - now;
        if (remaining <= 0L) {
            netrunCooldowns.remove(playerId);
            return 0L;
        }
        return remaining;
    }

    public void startCooldown(UUID playerId, int seconds) {
        long until = System.currentTimeMillis() + Math.max(0L, seconds) * 1000L;
        netrunCooldowns.put(playerId, until);
    }

    public String generateCode(int length) {
        int safeLength = Math.max(3, length);
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder(safeLength);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < safeLength; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private void ensureDataDirectory() {
        try {
            Files.createDirectories(dataDir);
        }
        catch (IOException ex) {
            logger.at(Level.WARNING).log("Failed to create NeonEcho data directory: " + ex.getMessage());
        }
    }

    private static List<String> resolveLines(List<String> preferred, List<String> fallback) {
        if (preferred == null || preferred.isEmpty()) {
            return fallback;
        }
        return preferred;
    }

    public record CredEntry(String playerKey, String displayName, int cred) {
    }

    public record NetrunStats(int wins, int fails, int streak, int bestStreak) {
    }

    public record DailyObjectiveView(String label, int progress, int target, boolean complete) {
    }

    public record DailyContractView(boolean enabled, boolean claimed, boolean complete, int reward,
                                    List<DailyObjectiveView> objectives) {
        public static DailyContractView disabled() {
            return new DailyContractView(false, false, false, 0, List.of());
        }
    }

    public record ClaimResult(ClaimStatus status, int reward) {
        public static ClaimResult disabled() {
            return new ClaimResult(ClaimStatus.DISABLED, 0);
        }

        public static ClaimResult noContract() {
            return new ClaimResult(ClaimStatus.NO_CONTRACT, 0);
        }

        public static ClaimResult alreadyClaimed() {
            return new ClaimResult(ClaimStatus.ALREADY_CLAIMED, 0);
        }

        public static ClaimResult notComplete() {
            return new ClaimResult(ClaimStatus.NOT_COMPLETE, 0);
        }

        public static ClaimResult claimed(int reward) {
            return new ClaimResult(ClaimStatus.CLAIMED, reward);
        }
    }

    public enum ClaimStatus {
        DISABLED,
        NO_CONTRACT,
        ALREADY_CLAIMED,
        NOT_COMPLETE,
        CLAIMED
    }

    public record NetrunTier(String name, int codeLength, int timeoutSeconds, int attempts, int reward,
                             int failPenalty, int cooldownSeconds) {
    }

    public static final class NetrunSession {
        private final String code;
        private final long expiresAt;
        private int attemptsRemaining;
        private final NetrunTier tier;

        public NetrunSession(String code, long expiresAt, int attemptsRemaining, NetrunTier tier) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.attemptsRemaining = attemptsRemaining;
            this.tier = tier;
        }

        public String getCode() {
            return code;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public int getAttemptsRemaining() {
            return attemptsRemaining;
        }

        public NetrunTier getTier() {
            return tier;
        }

        public int consumeAttempt() {
            if (attemptsRemaining > 0) {
                attemptsRemaining -= 1;
            }
            return attemptsRemaining;
        }

        public boolean isExpired(long now) {
            return now > expiresAt;
        }
    }
}
