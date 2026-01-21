package com.alexleo.neonecho;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

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
    private final Map<UUID, PlayerRef> onlineRefs = new ConcurrentHashMap<>();
    private final AtomicLong lastOnlineCredAt = new AtomicLong();
    private final Map<UUID, Long> eventRolls = new ConcurrentHashMap<>();

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

    public void markOnline(PlayerRef ref, boolean online) {
        if (ref == null) {
            return;
        }
        UUID playerId = ref.getUuid();
        if (online) {
            onlinePlayers.add(playerId);
            onlineRefs.put(playerId, ref);
        }
        else {
            onlinePlayers.remove(playerId);
            onlineRefs.remove(playerId);
            eventRolls.remove(playerId);
        }
    }

    public void markOnline(UUID playerId, boolean online) {
        if (playerId == null) {
            return;
        }
        if (online) {
            onlinePlayers.add(playerId);
        }
        else {
            onlinePlayers.remove(playerId);
            onlineRefs.remove(playerId);
            eventRolls.remove(playerId);
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

    public boolean isPerksEnabled() {
        NeonEchoConfig cfg = config;
        return cfg != null && Boolean.TRUE.equals(cfg.perksEnabled);
    }

    public int getPerkSlots() {
        NeonEchoConfig cfg = config;
        return cfg != null && cfg.perkSlots != null ? cfg.perkSlots : 0;
    }

    public List<NeonEchoConfig.PerkConfig> getPerkConfigs() {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.perks == null) {
            return List.of();
        }
        return cfg.perks;
    }

    public NeonEchoConfig.PerkConfig getPerkConfig(String perkId) {
        if (perkId == null) {
            return null;
        }
        String target = perkId.trim().toLowerCase(Locale.ROOT);
        for (NeonEchoConfig.PerkConfig perk : getPerkConfigs()) {
            if (perk != null && perk.id != null && perk.id.trim().equalsIgnoreCase(target)) {
                return perk;
            }
        }
        return null;
    }

    public int getPerkRank(UUID playerId, String perkId) {
        if (playerId == null || perkId == null) {
            return 0;
        }
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            return loadout.owned.getOrDefault(perkId.toLowerCase(Locale.ROOT), 0);
        }
    }

    public List<String> getActivePerks(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        int slots = Math.max(0, getPerkSlots());
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            List<String> active = new ArrayList<>(loadout.active);
            if (slots > 0 && active.size() > slots) {
                active = new ArrayList<>(active.subList(0, slots));
                loadout.active = new ArrayList<>(active);
            }
            return active;
        }
    }

    public PerkEffects getPerkEffects(UUID playerId) {
        if (!isPerksEnabled()) {
            return PerkEffects.defaults();
        }
        List<String> active = getActivePerks(playerId);
        double rewardMultiplier = 1.0;
        double cooldownMultiplier = 1.0;
        double timeoutMultiplier = 1.0;
        int attemptBonus = 0;
        int failPenaltyReduction = 0;
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            for (String perkId : active) {
                NeonEchoConfig.PerkConfig perk = getPerkConfig(perkId);
                if (perk == null) {
                    continue;
                }
                int rank = loadout.owned.getOrDefault(perk.id.toLowerCase(Locale.ROOT), 0);
                if (rank <= 0) {
                    continue;
                }
                rewardMultiplier = applyMultiplier(rewardMultiplier, perk.rewardMultiplier, rank);
                cooldownMultiplier = applyMultiplier(cooldownMultiplier, perk.cooldownMultiplier, rank);
                timeoutMultiplier = applyMultiplier(timeoutMultiplier, perk.timeoutMultiplier, rank);
                attemptBonus += (perk.attemptBonus != null ? perk.attemptBonus : 0) * rank;
                failPenaltyReduction += (perk.failPenaltyReduction != null ? perk.failPenaltyReduction : 0) * rank;
            }
        }
        return new PerkEffects(rewardMultiplier, cooldownMultiplier, timeoutMultiplier, attemptBonus, failPenaltyReduction);
    }

    public PerkPurchaseResult purchasePerk(UUID playerId, String perkId) {
        if (!isPerksEnabled()) {
            return PerkPurchaseResult.disabled();
        }
        NeonEchoConfig.PerkConfig perk = getPerkConfig(perkId);
        if (perk == null) {
            return PerkPurchaseResult.notFound();
        }
        String normalized = perk.id.toLowerCase(Locale.ROOT);
        int maxRank = perk.maxRank != null ? perk.maxRank : 1;
        int baseCost = perk.cost != null ? perk.cost : 0;
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            int currentRank = loadout.owned.getOrDefault(normalized, 0);
            if (currentRank >= maxRank) {
                return PerkPurchaseResult.maxed(currentRank, baseCost);
            }
            int cost = Math.max(0, baseCost * (currentRank + 1));
            int currentCred = data.cred.getOrDefault(playerId.toString(), 0);
            if (currentCred < cost) {
                return PerkPurchaseResult.insufficient(currentRank, cost);
            }
            data.cred.put(playerId.toString(), currentCred - cost);
            int newRank = currentRank + 1;
            loadout.owned.put(normalized, newRank);
            if (!loadout.active.contains(normalized) && loadout.active.size() < Math.max(0, getPerkSlots())) {
                loadout.active.add(normalized);
            }
            return PerkPurchaseResult.purchased(newRank, cost);
        }
    }

    public PerkEquipResult equipPerk(UUID playerId, String perkId) {
        if (!isPerksEnabled()) {
            return PerkEquipResult.disabled();
        }
        NeonEchoConfig.PerkConfig perk = getPerkConfig(perkId);
        if (perk == null) {
            return PerkEquipResult.notFound();
        }
        String normalized = perk.id.toLowerCase(Locale.ROOT);
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            int rank = loadout.owned.getOrDefault(normalized, 0);
            if (rank <= 0) {
                return PerkEquipResult.notOwned();
            }
            if (loadout.active.contains(normalized)) {
                return PerkEquipResult.alreadyEquipped();
            }
            int slots = Math.max(0, getPerkSlots());
            if (slots > 0 && loadout.active.size() >= slots) {
                return PerkEquipResult.noSlots(slots);
            }
            loadout.active.add(normalized);
            return PerkEquipResult.equipped(slots);
        }
    }

    public PerkEquipResult unequipPerk(UUID playerId, String perkId) {
        if (!isPerksEnabled()) {
            return PerkEquipResult.disabled();
        }
        NeonEchoConfig.PerkConfig perk = getPerkConfig(perkId);
        if (perk == null) {
            return PerkEquipResult.notFound();
        }
        String normalized = perk.id.toLowerCase(Locale.ROOT);
        synchronized (dataLock) {
            NeonPerkLoadout loadout = ensurePerkLoadout(playerId);
            if (!loadout.active.contains(normalized)) {
                return PerkEquipResult.notEquipped();
            }
            loadout.active.remove(normalized);
            return PerkEquipResult.unequipped(getPerkSlots());
        }
    }

    private NeonPerkLoadout ensurePerkLoadout(UUID playerId) {
        String key = playerId.toString();
        NeonPerkLoadout loadout = data.perkLoadouts.get(key);
        if (loadout == null) {
            loadout = new NeonPerkLoadout();
            loadout.normalize();
            data.perkLoadouts.put(key, loadout);
        }
        else {
            loadout.normalize();
        }
        return loadout;
    }

    private double applyMultiplier(double current, Double perkMultiplier, int rank) {
        if (perkMultiplier == null) {
            return current;
        }
        double multiplier = perkMultiplier;
        double result = current;
        for (int i = 0; i < rank; i++) {
            result *= multiplier;
        }
        return result;
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

    public void tickEvents() {
        NeonEchoConfig cfg = config;
        if (cfg == null || !Boolean.TRUE.equals(cfg.eventsEnabled)) {
            return;
        }
        int intervalSeconds = cfg.eventIntervalSeconds != null ? cfg.eventIntervalSeconds : 0;
        if (intervalSeconds <= 0) {
            return;
        }
        List<NeonEchoConfig.EventConfig> eventConfigs = cfg.events != null ? cfg.events : List.of();
        if (eventConfigs.isEmpty()) {
            return;
        }
        double chance = cfg.eventChance != null ? cfg.eventChance : 0.0;
        long now = System.currentTimeMillis();
        for (UUID playerId : onlinePlayers) {
            if (getActiveEvent(playerId) != null) {
                continue;
            }
            long lastRoll = eventRolls.getOrDefault(playerId, 0L);
            if (now - lastRoll < intervalSeconds * 1000L) {
                continue;
            }
            eventRolls.put(playerId, now);
            if (ThreadLocalRandom.current().nextDouble() > chance) {
                continue;
            }
            NeonEchoConfig.EventConfig chosen = eventConfigs.get(ThreadLocalRandom.current().nextInt(eventConfigs.size()));
            NeonEventState event = startEvent(playerId, chosen, now);
            PlayerRef ref = onlineRefs.get(playerId);
            if (ref != null && event != null) {
                ref.sendMessage(Message.raw(formatMessage("Neon alert: " + event.name + ".")));
                if (event.description != null && !event.description.isBlank()) {
                    ref.sendMessage(Message.raw(formatMessage(event.description)));
                }
                ref.sendMessage(Message.raw(formatMessage("Use /neonalert for details.")));
            }
        }
    }

    public NeonEventState getActiveEvent(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        synchronized (dataLock) {
            String key = playerId.toString();
            NeonEventState event = data.eventStates.get(key);
            if (event == null) {
                return null;
            }
            if (event.isExpired(now) || (event.usesRemaining != null && event.usesRemaining <= 0)) {
                data.eventStates.remove(key);
                return null;
            }
            return event;
        }
    }

    public EventClaimResult claimEventDrop(UUID playerId) {
        NeonEventState event = getActiveEvent(playerId);
        if (event == null) {
            return EventClaimResult.noEvent();
        }
        if (!"drop".equalsIgnoreCase(event.type)) {
            return EventClaimResult.notDrop();
        }
        int dropCred = event.dropCred != null ? event.dropCred : 0;
        if (dropCred <= 0) {
            return EventClaimResult.notDrop();
        }
        synchronized (dataLock) {
            NeonEventState active = data.eventStates.get(playerId.toString());
            if (active == null) {
                return EventClaimResult.noEvent();
            }
            int uses = active.usesRemaining != null ? active.usesRemaining : 1;
            if (uses <= 0) {
                data.eventStates.remove(playerId.toString());
                return EventClaimResult.expired();
            }
            uses -= 1;
            active.usesRemaining = uses;
            addCred(playerId, dropCred);
            if (uses <= 0) {
                data.eventStates.remove(playerId.toString());
            }
            return EventClaimResult.claimed(dropCred, uses);
        }
    }

    public int consumeEventBonus(UUID playerId) {
        NeonEventState event = getActiveEvent(playerId);
        if (event == null || !"netrun_bonus".equalsIgnoreCase(event.type)) {
            return 0;
        }
        int bonus = event.bonusCred != null ? event.bonusCred : 0;
        if (bonus <= 0) {
            return 0;
        }
        synchronized (dataLock) {
            NeonEventState active = data.eventStates.get(playerId.toString());
            if (active == null) {
                return 0;
            }
            int uses = active.usesRemaining != null ? active.usesRemaining : 1;
            if (uses <= 0) {
                data.eventStates.remove(playerId.toString());
                return 0;
            }
            uses -= 1;
            active.usesRemaining = uses;
            if (uses <= 0) {
                data.eventStates.remove(playerId.toString());
            }
            return bonus;
        }
    }

    private NeonEventState startEvent(UUID playerId, NeonEchoConfig.EventConfig config, long now) {
        if (config == null || playerId == null) {
            return null;
        }
        NeonEventState state = new NeonEventState();
        state.id = config.id;
        state.name = config.name != null ? config.name : "Neon Event";
        state.description = config.description != null ? config.description : "";
        state.type = config.type != null ? config.type : "netrun_bonus";
        int duration = config.durationSeconds != null ? config.durationSeconds : 300;
        state.expiresAt = now + Math.max(30, duration) * 1000L;
        state.bonusCred = config.bonusCred != null ? config.bonusCred : 0;
        state.dropCred = config.dropCred != null ? config.dropCred : 0;
        state.usesRemaining = config.maxTriggers != null ? config.maxTriggers : 1;
        synchronized (dataLock) {
            data.eventStates.put(playerId.toString(), state);
        }
        return state;
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

    public List<String> getNetrunRiskNames() {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.netrunRisks == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (NeonEchoConfig.RiskProfileConfig risk : cfg.netrunRisks) {
            if (risk != null && risk.name != null && !risk.name.isBlank()) {
                names.add(risk.name);
            }
        }
        return names;
    }

    public NetrunRisk resolveNetrunRisk(String name) {
        NeonEchoConfig cfg = config;
        if (cfg == null || cfg.netrunRisks == null || cfg.netrunRisks.isEmpty()) {
            return new NetrunRisk("normal", 1.0, 1.0, 1.0, 1.0, 1.0);
        }
        String requested = name != null ? name.trim().toLowerCase(Locale.ROOT) : null;
        NeonEchoConfig.RiskProfileConfig fallback = cfg.netrunRisks.get(0);
        for (NeonEchoConfig.RiskProfileConfig risk : cfg.netrunRisks) {
            if (risk != null && risk.name != null && risk.name.trim().equalsIgnoreCase(requested)) {
                return toRisk(risk);
            }
            if (risk != null && risk.name != null && risk.name.trim().equalsIgnoreCase("normal")) {
                fallback = risk;
            }
        }
        return toRisk(fallback);
    }

    public NetrunSession createSession(UUID playerId, NetrunTier tier, NetrunRisk risk, int stage, int stages, long now) {
        NeonEchoConfig cfg = config;
        int stageCount = stages > 0 ? stages : (cfg != null && cfg.netrunStages != null ? cfg.netrunStages : 1);
        stageCount = Math.max(1, stageCount);
        int stageIndex = Math.min(Math.max(1, stage), stageCount);

        PerkEffects perks = getPerkEffects(playerId);
        double rewardMultiplier = risk.rewardMultiplier() * perks.rewardMultiplier();
        double cooldownMultiplier = risk.cooldownMultiplier() * perks.cooldownMultiplier();
        double timeoutMultiplier = risk.timeoutMultiplier() * perks.timeoutMultiplier();
        double attemptsMultiplier = risk.attemptsMultiplier();
        double failPenaltyMultiplier = risk.failPenaltyMultiplier();

        int baseCodeLength = Math.max(3, tier.codeLength());
        int baseTimeout = applyIntMultiplier(tier.timeoutSeconds(), timeoutMultiplier, 6);
        int baseAttempts = applyIntMultiplier(tier.attempts(), attemptsMultiplier, 1) + perks.attemptBonus();
        int baseReward = applyIntMultiplier(tier.reward(), rewardMultiplier, 0);
        int baseCooldown = applyIntMultiplier(tier.cooldownSeconds(), cooldownMultiplier, 0);
        int baseFailPenalty = applyIntMultiplier(tier.failPenalty(), failPenaltyMultiplier, 0) - perks.failPenaltyReduction();
        if (baseFailPenalty < 0) {
            baseFailPenalty = 0;
        }

        int stageLengthDelta = cfg != null && cfg.netrunStageLengthDelta != null ? cfg.netrunStageLengthDelta : 0;
        int stageTimeoutDelta = cfg != null && cfg.netrunStageTimeoutDelta != null ? cfg.netrunStageTimeoutDelta : 0;
        int stageRewardBonus = cfg != null && cfg.netrunStageRewardBonus != null ? cfg.netrunStageRewardBonus : 0;
        int stageCooldownBonus = cfg != null && cfg.netrunStageCooldownBonus != null ? cfg.netrunStageCooldownBonus : 0;

        int stageCodeLength = Math.max(3, baseCodeLength + stageLengthDelta * (stageIndex - 1));
        int stageTimeout = Math.max(6, baseTimeout + stageTimeoutDelta * (stageIndex - 1));
        int stageAttempts = Math.max(1, baseAttempts);
        String code = generateCode(stageCodeLength);
        long expiresAt = now + stageTimeout * 1000L;

        return new NetrunSession(code, expiresAt, stageAttempts, tier, risk, stageIndex, stageCount,
                baseCodeLength, baseTimeout, baseAttempts, baseReward, baseCooldown, baseFailPenalty,
                stageLengthDelta, stageTimeoutDelta, stageRewardBonus, stageCooldownBonus);
    }

    public NetrunSession advanceSession(NetrunSession session, long now) {
        if (session == null || session.getStage() >= session.getStages()) {
            return null;
        }
        int nextStage = session.getStage() + 1;
        int stageCodeLength = Math.max(3, session.getBaseCodeLength()
                + session.getStageLengthDelta() * (nextStage - 1));
        int stageTimeout = Math.max(6, session.getBaseTimeoutSeconds()
                + session.getStageTimeoutDelta() * (nextStage - 1));
        int stageAttempts = Math.max(1, session.getBaseAttempts());
        String code = generateCode(stageCodeLength);
        long expiresAt = now + stageTimeout * 1000L;
        return new NetrunSession(code, expiresAt, stageAttempts, session.getTier(), session.getRisk(),
                nextStage, session.getStages(), session.getBaseCodeLength(), session.getBaseTimeoutSeconds(),
                session.getBaseAttempts(), session.getBaseReward(), session.getBaseCooldownSeconds(),
                session.getBaseFailPenalty(), session.getStageLengthDelta(), session.getStageTimeoutDelta(),
                session.getStageRewardBonus(), session.getStageCooldownBonus());
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

    private NetrunRisk toRisk(NeonEchoConfig.RiskProfileConfig risk) {
        String name = risk != null && risk.name != null ? risk.name : "normal";
        double reward = risk != null && risk.rewardMultiplier != null ? risk.rewardMultiplier : 1.0;
        double cooldown = risk != null && risk.cooldownMultiplier != null ? risk.cooldownMultiplier : 1.0;
        double timeout = risk != null && risk.timeoutMultiplier != null ? risk.timeoutMultiplier : 1.0;
        double penalty = risk != null && risk.failPenaltyMultiplier != null ? risk.failPenaltyMultiplier : 1.0;
        double attempts = risk != null && risk.attemptsMultiplier != null ? risk.attemptsMultiplier : 1.0;
        return new NetrunRisk(name, reward, cooldown, timeout, penalty, attempts);
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

    public void startSession(UUID playerId, NetrunSession session) {
        netrunSessions.put(playerId, session);
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

    private int applyIntMultiplier(int value, double multiplier, int minimum) {
        int result = (int) Math.round(value * multiplier);
        return Math.max(minimum, result);
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

    public record PerkEffects(double rewardMultiplier, double cooldownMultiplier, double timeoutMultiplier,
                              int attemptBonus, int failPenaltyReduction) {
        public static PerkEffects defaults() {
            return new PerkEffects(1.0, 1.0, 1.0, 0, 0);
        }
    }

    public record PerkPurchaseResult(PerkPurchaseStatus status, int rank, int cost) {
        public static PerkPurchaseResult disabled() {
            return new PerkPurchaseResult(PerkPurchaseStatus.DISABLED, 0, 0);
        }

        public static PerkPurchaseResult notFound() {
            return new PerkPurchaseResult(PerkPurchaseStatus.NOT_FOUND, 0, 0);
        }

        public static PerkPurchaseResult maxed(int rank, int cost) {
            return new PerkPurchaseResult(PerkPurchaseStatus.MAXED, rank, cost);
        }

        public static PerkPurchaseResult insufficient(int rank, int cost) {
            return new PerkPurchaseResult(PerkPurchaseStatus.INSUFFICIENT, rank, cost);
        }

        public static PerkPurchaseResult purchased(int rank, int cost) {
            return new PerkPurchaseResult(PerkPurchaseStatus.PURCHASED, rank, cost);
        }
    }

    public enum PerkPurchaseStatus {
        DISABLED,
        NOT_FOUND,
        MAXED,
        INSUFFICIENT,
        PURCHASED
    }

    public record PerkEquipResult(PerkEquipStatus status, int slots) {
        public static PerkEquipResult disabled() {
            return new PerkEquipResult(PerkEquipStatus.DISABLED, 0);
        }

        public static PerkEquipResult notFound() {
            return new PerkEquipResult(PerkEquipStatus.NOT_FOUND, 0);
        }

        public static PerkEquipResult notOwned() {
            return new PerkEquipResult(PerkEquipStatus.NOT_OWNED, 0);
        }

        public static PerkEquipResult alreadyEquipped() {
            return new PerkEquipResult(PerkEquipStatus.ALREADY_EQUIPPED, 0);
        }

        public static PerkEquipResult noSlots(int slots) {
            return new PerkEquipResult(PerkEquipStatus.NO_SLOTS, slots);
        }

        public static PerkEquipResult equipped(int slots) {
            return new PerkEquipResult(PerkEquipStatus.EQUIPPED, slots);
        }

        public static PerkEquipResult notEquipped() {
            return new PerkEquipResult(PerkEquipStatus.NOT_EQUIPPED, 0);
        }

        public static PerkEquipResult unequipped(int slots) {
            return new PerkEquipResult(PerkEquipStatus.UNEQUIPPED, slots);
        }
    }

    public enum PerkEquipStatus {
        DISABLED,
        NOT_FOUND,
        NOT_OWNED,
        ALREADY_EQUIPPED,
        NO_SLOTS,
        EQUIPPED,
        NOT_EQUIPPED,
        UNEQUIPPED
    }

    public record EventClaimResult(EventClaimStatus status, int reward, int usesRemaining) {
        public static EventClaimResult noEvent() {
            return new EventClaimResult(EventClaimStatus.NO_EVENT, 0, 0);
        }

        public static EventClaimResult notDrop() {
            return new EventClaimResult(EventClaimStatus.NOT_DROP, 0, 0);
        }

        public static EventClaimResult expired() {
            return new EventClaimResult(EventClaimStatus.EXPIRED, 0, 0);
        }

        public static EventClaimResult claimed(int reward, int usesRemaining) {
            return new EventClaimResult(EventClaimStatus.CLAIMED, reward, usesRemaining);
        }
    }

    public enum EventClaimStatus {
        NO_EVENT,
        NOT_DROP,
        EXPIRED,
        CLAIMED
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

    public record NetrunRisk(String name, double rewardMultiplier, double cooldownMultiplier, double timeoutMultiplier,
                             double failPenaltyMultiplier, double attemptsMultiplier) {
    }

    public static final class NetrunSession {
        private final String code;
        private final long expiresAt;
        private int attemptsRemaining;
        private final NetrunTier tier;
        private final NetrunRisk risk;
        private final int stage;
        private final int stages;
        private final int baseCodeLength;
        private final int baseTimeoutSeconds;
        private final int baseAttempts;
        private final int baseReward;
        private final int baseCooldownSeconds;
        private final int baseFailPenalty;
        private final int stageLengthDelta;
        private final int stageTimeoutDelta;
        private final int stageRewardBonus;
        private final int stageCooldownBonus;

        public NetrunSession(
                String code,
                long expiresAt,
                int attemptsRemaining,
                NetrunTier tier,
                NetrunRisk risk,
                int stage,
                int stages,
                int baseCodeLength,
                int baseTimeoutSeconds,
                int baseAttempts,
                int baseReward,
                int baseCooldownSeconds,
                int baseFailPenalty,
                int stageLengthDelta,
                int stageTimeoutDelta,
                int stageRewardBonus,
                int stageCooldownBonus) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.attemptsRemaining = attemptsRemaining;
            this.tier = tier;
            this.risk = risk;
            this.stage = stage;
            this.stages = stages;
            this.baseCodeLength = baseCodeLength;
            this.baseTimeoutSeconds = baseTimeoutSeconds;
            this.baseAttempts = baseAttempts;
            this.baseReward = baseReward;
            this.baseCooldownSeconds = baseCooldownSeconds;
            this.baseFailPenalty = baseFailPenalty;
            this.stageLengthDelta = stageLengthDelta;
            this.stageTimeoutDelta = stageTimeoutDelta;
            this.stageRewardBonus = stageRewardBonus;
            this.stageCooldownBonus = stageCooldownBonus;
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

        public NetrunRisk getRisk() {
            return risk;
        }

        public int getStage() {
            return stage;
        }

        public int getStages() {
            return stages;
        }

        public int getStageCodeLength() {
            return Math.max(3, baseCodeLength + stageLengthDelta * (stage - 1));
        }

        public int getStageTimeoutSeconds() {
            return Math.max(6, baseTimeoutSeconds + stageTimeoutDelta * (stage - 1));
        }

        public int getStageAttempts() {
            return Math.max(1, baseAttempts);
        }

        public int getBaseCodeLength() {
            return baseCodeLength;
        }

        public int getBaseTimeoutSeconds() {
            return baseTimeoutSeconds;
        }

        public int getBaseAttempts() {
            return baseAttempts;
        }

        public int getBaseReward() {
            return baseReward;
        }

        public int getBaseCooldownSeconds() {
            return baseCooldownSeconds;
        }

        public int getBaseFailPenalty() {
            return baseFailPenalty;
        }

        public int getStageLengthDelta() {
            return stageLengthDelta;
        }

        public int getStageTimeoutDelta() {
            return stageTimeoutDelta;
        }

        public int getStageRewardBonus() {
            return stageRewardBonus;
        }

        public int getStageCooldownBonus() {
            return stageCooldownBonus;
        }

        public int getTotalReward() {
            return Math.max(0, baseReward + stageRewardBonus * (stages - 1));
        }

        public int getTotalCooldownSeconds() {
            return Math.max(0, baseCooldownSeconds + stageCooldownBonus * (stages - 1));
        }

        public int getFailPenalty() {
            return Math.max(0, baseFailPenalty);
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
