package com.alexleo.neonecho;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NeonDeckPage extends InteractiveCustomUIPage<NeonDeckPage.NeonDeckEventData> {
    private static final String TAB_NETRUN = "netrun";
    private static final String TAB_PERKS = "perks";
    private static final String TAB_ALERTS = "alerts";

    private final NeonEchoState state;
    private final PlayerRef playerRef;
    private final UUID playerId;

    private String activeTab = TAB_NETRUN;
    private String tierInput = "";
    private String riskInput = "";
    private String codeInput = "";

    private String netrunStatus;
    private String perkStatus;
    private String alertStatus;

    public NeonDeckPage(PlayerRef playerRef, NeonEchoState state) {
        super(playerRef, CustomPageLifetime.CanDismiss, NeonDeckEventData.CODEC);
        this.playerRef = playerRef;
        this.playerId = playerRef.getUuid();
        this.state = state;

        NeonEchoState.NetrunTier defaultTier = state.resolveNetrunTier(null);
        if (defaultTier != null && defaultTier.name() != null) {
            this.tierInput = defaultTier.name();
        }
        NeonEchoState.NetrunRisk defaultRisk = state.resolveNetrunRisk(null);
        if (defaultRisk != null && defaultRisk.name() != null) {
            this.riskInput = defaultRisk.name();
        }
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder events, Store<EntityStore> store) {
        builder.append("Pages/NeonEcho/NeonDeck.ui");
        bindStaticEvents(events);
        builder.set("#TierInput.Value", tierInput);
        builder.set("#RiskInput.Value", riskInput);
        builder.set("#CodeInput.Value", codeInput);
        refreshAll(ref, store, builder, events);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, NeonDeckEventData data) {
        boolean needsRefresh = false;

        if (data.tier != null) {
            tierInput = data.tier.trim();
        }
        if (data.risk != null) {
            riskInput = data.risk.trim();
        }
        if (data.code != null) {
            codeInput = data.code.trim();
        }

        if (data.tab != null) {
            activeTab = data.tab.trim().toLowerCase(Locale.ROOT);
            needsRefresh = true;
        }

        if (data.action != null) {
            handleAction(data.action.trim(), data.perkId);
            needsRefresh = true;
        }

        if (!needsRefresh) {
            return;
        }

        UICommandBuilder update = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        refreshAll(ref, store, update, eventBuilder);
        if (data.action != null && data.action.trim().equalsIgnoreCase("Submit")) {
            codeInput = "";
            update.set("#CodeInput.Value", "");
        }
        sendUpdate(update, eventBuilder, false);
    }

    private void bindStaticEvents(UIEventBuilder events) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabNetrun",
                EventData.of(NeonDeckEventData.KEY_TAB, TAB_NETRUN));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabPerks",
                EventData.of(NeonDeckEventData.KEY_TAB, TAB_PERKS));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabAlerts",
                EventData.of(NeonDeckEventData.KEY_TAB, TAB_ALERTS));

        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TierInput",
                EventData.of(NeonDeckEventData.KEY_TIER, "#TierInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#RiskInput",
                EventData.of(NeonDeckEventData.KEY_RISK, "#RiskInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CodeInput",
                EventData.of(NeonDeckEventData.KEY_CODE, "#CodeInput.Value"), false);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#StartRun",
                new EventData().append(NeonDeckEventData.KEY_ACTION, "Start"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SubmitCode",
                new EventData().append(NeonDeckEventData.KEY_ACTION, "Submit"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#RefreshNetrun",
                new EventData().append(NeonDeckEventData.KEY_ACTION, "RefreshNetrun"));

        events.addEventBinding(CustomUIEventBindingType.Activating, "#AlertClaim",
                new EventData().append(NeonDeckEventData.KEY_ACTION, "ClaimDrop"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#AlertRefresh",
                new EventData().append(NeonDeckEventData.KEY_ACTION, "RefreshAlerts"));
    }

    private void handleAction(String action, String perkId) {
        switch (action) {
            case "Start" -> handleNetrunStart();
            case "Submit" -> handleNetrunSubmit();
            case "RefreshNetrun" -> netrunStatus = null;
            case "PerkBuy" -> handlePerkPurchase(perkId);
            case "PerkToggle" -> handlePerkToggle(perkId);
            case "ClaimDrop" -> handleClaimDrop();
            case "RefreshAlerts" -> alertStatus = null;
            default -> {
            }
        }
    }

    private void refreshAll(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder builder, UIEventBuilder events) {
        refreshHeader(builder);
        refreshTabs(builder);
        refreshNetrun(builder);
        refreshPerks(builder, events);
        refreshAlerts(builder);
    }

    private void refreshHeader(UICommandBuilder builder) {
        int cred = state.getCred(playerId);
        String title = state.getTitle(playerId);
        builder.set("#CredValue.Text", Integer.toString(cred));
        builder.set("#TitleValue.Text", title != null ? title : "Rookie");
    }

    private void refreshTabs(UICommandBuilder builder) {
        if (!TAB_NETRUN.equals(activeTab) && !TAB_PERKS.equals(activeTab) && !TAB_ALERTS.equals(activeTab)) {
            activeTab = TAB_NETRUN;
        }
        boolean netrun = TAB_NETRUN.equals(activeTab);
        boolean perks = TAB_PERKS.equals(activeTab);
        boolean alerts = TAB_ALERTS.equals(activeTab);

        builder.set("#NetrunPanel.Visible", netrun);
        builder.set("#PerksPanel.Visible", perks);
        builder.set("#AlertsPanel.Visible", alerts);

        builder.set("#TabNetrunMarker.Visible", netrun);
        builder.set("#TabPerksMarker.Visible", perks);
        builder.set("#TabAlertsMarker.Visible", alerts);
    }

    private void refreshNetrun(UICommandBuilder builder) {
        String meta = buildNetrunMeta();
        builder.set("#NetrunMeta.Text", meta);
        builder.set("#NetrunStatus.Text", buildNetrunStatus());
    }

    private String buildNetrunMeta() {
        List<String> tiers = state.getNetrunTierNames();
        List<String> risks = state.getNetrunRiskNames();
        String tierLine = tiers.isEmpty() ? "" : "Tiers: " + String.join(", ", tiers);
        String riskLine = risks.isEmpty() ? "" : "Risks: " + String.join(", ", risks);
        if (tierLine.isEmpty()) {
            return riskLine;
        }
        if (riskLine.isEmpty()) {
            return tierLine;
        }
        return tierLine + " | " + riskLine;
    }

    private String buildNetrunStatus() {
        if (netrunStatus != null && !netrunStatus.isBlank()) {
            return netrunStatus;
        }
        NeonEchoState.NetrunSession session = state.getActiveSession(playerId);
        if (session != null) {
            long now = System.currentTimeMillis();
            long secondsLeft = Math.max(0L, (session.getExpiresAt() - now) / 1000L);
            return "Active stage " + session.getStage() + "/" + session.getStages()
                    + ". Code: " + session.getCode() + " | " + secondsLeft + "s left.";
        }
        long cooldown = state.getCooldownRemainingMillis(playerId);
        if (cooldown > 0) {
            return "Cooldown active. " + Math.max(1L, cooldown / 1000L) + "s remaining.";
        }
        return "Ready. Enter tier + risk, then hit Start.";
    }

    private void handleNetrunStart() {
        state.recordPlayerName(playerId, playerRef.getUsername());
        NeonEchoState.NetrunSession active = state.getActiveSession(playerId);
        if (active != null) {
            netrunStatus = "Session already running. Enter the code and hit Submit.";
            return;
        }
        long cooldown = state.getCooldownRemainingMillis(playerId);
        if (cooldown > 0) {
            Map<String, String> tokens = new HashMap<>();
            tokens.put("cooldown", Long.toString(Math.max(1L, cooldown / 1000L)));
            netrunStatus = formatLines(state.getNetrunCooldownLines(), tokens);
            return;
        }

        String tierName = tierInput.isBlank() ? null : tierInput;
        String riskName = riskInput.isBlank() ? null : riskInput;
        if (tierName != null && !isTierName(tierName)) {
            netrunStatus = "Unknown tier: " + tierName + ".";
            return;
        }
        if (riskName != null && !isRiskName(riskName)) {
            netrunStatus = "Unknown risk: " + riskName + ".";
            return;
        }

        NeonEchoState.NetrunTier tier = state.resolveNetrunTier(tierName);
        NeonEchoState.NetrunRisk risk = state.resolveNetrunRisk(riskName);
        long now = System.currentTimeMillis();
        NeonEchoState.NetrunSession session = state.createSession(playerId, tier, risk, 1, 0, now);
        state.startSession(playerId, session);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("code", session.getCode());
        tokens.put("seconds", Integer.toString(session.getStageTimeoutSeconds()));
        tokens.put("tier", tier.name());
        tokens.put("stage", Integer.toString(session.getStage()));
        tokens.put("stages", Integer.toString(session.getStages()));
        tokens.put("risk", risk.name());
        netrunStatus = formatLines(state.getNetrunStartLines(), tokens);
    }

    private void handleNetrunSubmit() {
        NeonEchoState.NetrunSession session = state.getActiveSession(playerId);
        if (session == null) {
            netrunStatus = "No active netrun. Hit Start to launch a run.";
            return;
        }
        if (codeInput == null || codeInput.isBlank()) {
            netrunStatus = "Enter the access code before submitting.";
            return;
        }

        String attempt = codeInput.trim().toUpperCase(Locale.ROOT);
        NeonEchoState.NetrunTier tier = session.getTier();
        NeonEchoState.NetrunRisk risk = session.getRisk();
        long now = System.currentTimeMillis();

        if (session.getCode().equalsIgnoreCase(attempt)) {
            if (session.getStage() < session.getStages()) {
                NeonEchoState.NetrunSession next = state.advanceSession(session, now);
                if (next == null) {
                    state.clearSession(playerId);
                    netrunStatus = "Session cleared.";
                    return;
                }
                state.startSession(playerId, next);
                netrunStatus = "Stage " + session.getStage() + "/" + session.getStages() + " cleared. Next code: "
                        + next.getCode() + ".";
                return;
            }

            state.clearSession(playerId);
            int cooldown = session.getTotalCooldownSeconds();
            state.startCooldown(playerId, cooldown);
            int streak = state.recordNetrunWin(playerId);
            int reward = session.getTotalReward();
            int bonus = state.consumeEventBonus(playerId);
            if (bonus > 0) {
                reward += bonus;
            }
            state.addCred(playerId, reward);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("cred", Integer.toString(reward));
            tokens.put("cooldown", Integer.toString(cooldown));
            tokens.put("tier", tier.name());
            tokens.put("streak", Integer.toString(streak));
            tokens.put("risk", risk.name());
            netrunStatus = formatLines(state.getNetrunSuccessLines(), tokens);
            if (bonus > 0) {
                netrunStatus += "\nEvent bonus: +" + bonus + " cred.";
            }
            return;
        }

        int attemptsLeft = session.consumeAttempt();
        if (attemptsLeft > 0) {
            netrunStatus = "Access denied. Attempts left: " + attemptsLeft + ".";
            return;
        }

        state.clearSession(playerId);
        int cooldown = session.getTotalCooldownSeconds();
        state.startCooldown(playerId, cooldown);
        state.recordNetrunFail(playerId);

        int penalty = session.getFailPenalty();
        if (penalty > 0) {
            state.addCred(playerId, -penalty);
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("cooldown", Integer.toString(cooldown));
        tokens.put("tier", tier.name());
        tokens.put("risk", risk.name());
        netrunStatus = formatLines(state.getNetrunFailLines(), tokens);
        if (penalty > 0) {
            netrunStatus += "\nStreet Cred lost: -" + penalty + ".";
        }
    }

    private void refreshPerks(UICommandBuilder builder, UIEventBuilder events) {
        List<NeonEchoConfig.PerkConfig> perks = state.getPerkConfigs();
        boolean perksEnabled = state.isPerksEnabled();
        int slots = Math.max(0, state.getPerkSlots());
        List<String> active = state.getActivePerks(playerId);

        builder.set("#PerkSlots.Text", "Slots: " + active.size() + "/" + slots);
        builder.set("#PerkActive.Text", "Active: " + (active.isEmpty() ? "none" : String.join(", ", active)));

        if (!perksEnabled) {
            builder.set("#PerkStatus.Text", "Perks are disabled in the config.");
        }
        else if (perkStatus != null) {
            builder.set("#PerkStatus.Text", perkStatus);
        }
        else {
            builder.set("#PerkStatus.Text", "Upgrade perks and tune your loadout.");
        }

        builder.clear("#PerkList");
        if (perks == null || perks.isEmpty()) {
            return;
        }

        int cred = state.getCred(playerId);
        int rowIndex = 0;
        for (NeonEchoConfig.PerkConfig perk : perks) {
            if (perk == null || perk.id == null || perk.id.isBlank()) {
                continue;
            }
            builder.append("#PerkList", "Pages/NeonEcho/PerkRow.ui");
            String row = "#PerkList[" + rowIndex + "]";
            rowIndex += 1;

            String name = perk.name != null && !perk.name.isBlank() ? perk.name : perk.id;
            int maxRank = perk.maxRank != null ? perk.maxRank : 1;
            int rank = state.getPerkRank(playerId, perk.id);
            int baseCost = perk.cost != null ? perk.cost : 0;
            int nextCost = Math.max(0, baseCost * (rank + 1));
            String detail = (perk.description != null ? perk.description : "")
                    + " (Rank " + rank + "/" + maxRank + ")";

            builder.set(row + " #PerkName.Text", name);
            builder.set(row + " #PerkDetail.Text", detail);

            boolean maxed = rank >= maxRank;
            boolean owned = rank > 0;
            boolean equipped = active.contains(perk.id.toLowerCase(Locale.ROOT));
            boolean canEquip = owned && (!equipped) && (slots == 0 || active.size() < slots);

            String buyText;
            boolean buyDisabled = false;
            if (!perksEnabled) {
                buyText = "Disabled";
                buyDisabled = true;
            }
            else if (maxed) {
                buyText = "Maxed";
                buyDisabled = true;
            }
            else if (rank == 0) {
                buyText = "Buy (" + nextCost + ")";
                buyDisabled = cred < nextCost;
            }
            else {
                buyText = "Upgrade (" + nextCost + ")";
                buyDisabled = cred < nextCost;
            }

            String toggleText = equipped ? "Unequip" : "Equip";
            boolean toggleDisabled = !perksEnabled || !owned || (!equipped && !canEquip);

            builder.set(row + " #PerkBuy.Text", buyText);
            builder.set(row + " #PerkBuy.Disabled", buyDisabled);
            builder.set(row + " #PerkToggle.Text", toggleText);
            builder.set(row + " #PerkToggle.Disabled", toggleDisabled);

            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #PerkBuy",
                    new EventData().append(NeonDeckEventData.KEY_ACTION, "PerkBuy")
                            .append(NeonDeckEventData.KEY_PERK_ID, perk.id));
            events.addEventBinding(CustomUIEventBindingType.Activating, row + " #PerkToggle",
                    new EventData().append(NeonDeckEventData.KEY_ACTION, "PerkToggle")
                            .append(NeonDeckEventData.KEY_PERK_ID, perk.id));
        }
    }

    private void handlePerkPurchase(String perkId) {
        if (perkId == null || perkId.isBlank()) {
            perkStatus = "Select a perk first.";
            return;
        }
        NeonEchoState.PerkPurchaseResult result = state.purchasePerk(playerId, perkId);
        switch (result.status()) {
            case DISABLED -> perkStatus = "Perks are disabled.";
            case NOT_FOUND -> perkStatus = "Perk not found.";
            case MAXED -> perkStatus = "Perk already maxed.";
            case INSUFFICIENT -> perkStatus = "Need " + result.cost() + " cred to upgrade.";
            case PURCHASED -> perkStatus = "Perk upgraded to rank " + result.rank() + ".";
            default -> perkStatus = "Perk updated.";
        }
    }

    private void handlePerkToggle(String perkId) {
        if (perkId == null || perkId.isBlank()) {
            perkStatus = "Select a perk first.";
            return;
        }
        boolean equipped = state.getActivePerks(playerId).contains(perkId.toLowerCase(Locale.ROOT));
        NeonEchoState.PerkEquipResult result = equipped
                ? state.unequipPerk(playerId, perkId)
                : state.equipPerk(playerId, perkId);

        switch (result.status()) {
            case DISABLED -> perkStatus = "Perks are disabled.";
            case NOT_FOUND -> perkStatus = "Perk not found.";
            case NOT_OWNED -> perkStatus = "Perk not owned yet.";
            case ALREADY_EQUIPPED -> perkStatus = "Perk already equipped.";
            case NO_SLOTS -> perkStatus = "No free perk slots (" + result.slots() + ").";
            case EQUIPPED -> perkStatus = "Perk equipped.";
            case UNEQUIPPED -> perkStatus = "Perk unequipped.";
            case NOT_EQUIPPED -> perkStatus = "Perk not equipped.";
            default -> perkStatus = "Perk state updated.";
        }
    }

    private void refreshAlerts(UICommandBuilder builder) {
        long now = System.currentTimeMillis();
        NeonEventState event = state.getActiveEvent(playerId);

        if (event == null) {
            builder.set("#AlertStatus.Text", "No active neon events.");
            builder.set("#AlertMeta.Text", "");
            builder.set("#AlertClaim.Disabled", true);
            builder.set("#AlertClaim.Visible", false);
            if (alertStatus != null) {
                builder.set("#AlertStatus.Text", alertStatus);
            }
            return;
        }

        long secondsLeft = event.expiresAt != null ? Math.max(0L, (event.expiresAt - now) / 1000L) : 0L;
        String headline = event.name != null ? event.name : "Neon Alert";
        String description = event.description != null ? event.description : "";
        builder.set("#AlertStatus.Text", headline + (description.isBlank() ? "" : " - " + description));

        List<String> metaLines = new ArrayList<>();
        metaLines.add("Time left: " + secondsLeft + "s");
        if (event.type != null) {
            metaLines.add("Type: " + event.type);
        }
        if (event.bonusCred != null && event.bonusCred > 0) {
            metaLines.add("Bonus: +" + event.bonusCred + " cred");
        }
        if (event.dropCred != null && event.dropCred > 0) {
            metaLines.add("Drop: +" + event.dropCred + " cred");
        }
        if (event.usesRemaining != null) {
            metaLines.add("Uses: " + event.usesRemaining);
        }
        if (alertStatus != null) {
            metaLines.add(alertStatus);
        }
        builder.set("#AlertMeta.Text", String.join(" | ", metaLines));

        boolean canClaim = "drop".equalsIgnoreCase(event.type) && event.dropCred != null && event.dropCred > 0;
        builder.set("#AlertClaim.Disabled", !canClaim);
        builder.set("#AlertClaim.Visible", canClaim);
    }

    private void handleClaimDrop() {
        NeonEchoState.EventClaimResult result = state.claimEventDrop(playerId);
        switch (result.status()) {
            case NO_EVENT -> alertStatus = "No active drop to claim.";
            case NOT_DROP -> alertStatus = "This alert has no drops.";
            case EXPIRED -> alertStatus = "Drop expired.";
            case CLAIMED -> alertStatus = "Claimed +" + result.reward() + " cred.";
            default -> alertStatus = "Drop updated.";
        }
    }

    private boolean isTierName(String input) {
        for (String tier : state.getNetrunTierNames()) {
            if (tier.equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRiskName(String input) {
        for (String risk : state.getNetrunRiskNames()) {
            if (risk.equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }

    private String formatLines(List<String> lines, Map<String, String> tokens) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        List<String> formatted = new ArrayList<>();
        for (String line : lines) {
            formatted.add(applyTokens(line, tokens));
        }
        return String.join("\n", formatted);
    }

    private String applyTokens(String line, Map<String, String> tokens) {
        String result = line;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    public static final class NeonDeckEventData {
        static final String KEY_TAB = "Tab";
        static final String KEY_ACTION = "Action";
        static final String KEY_TIER = "@Tier";
        static final String KEY_RISK = "@Risk";
        static final String KEY_CODE = "@Code";
        static final String KEY_PERK_ID = "PerkId";

        public static final BuilderCodec<NeonDeckEventData> CODEC = BuilderCodec
                .builder(NeonDeckEventData.class, NeonDeckEventData::new)
                .addField(new KeyedCodec<>(KEY_TAB, Codec.STRING),
                        (data, value) -> data.tab = value,
                        data -> data.tab)
                .addField(new KeyedCodec<>(KEY_ACTION, Codec.STRING),
                        (data, value) -> data.action = value,
                        data -> data.action)
                .addField(new KeyedCodec<>(KEY_TIER, Codec.STRING),
                        (data, value) -> data.tier = value,
                        data -> data.tier)
                .addField(new KeyedCodec<>(KEY_RISK, Codec.STRING),
                        (data, value) -> data.risk = value,
                        data -> data.risk)
                .addField(new KeyedCodec<>(KEY_CODE, Codec.STRING),
                        (data, value) -> data.code = value,
                        data -> data.code)
                .addField(new KeyedCodec<>(KEY_PERK_ID, Codec.STRING),
                        (data, value) -> data.perkId = value,
                        data -> data.perkId)
                .build();

        private String tab;
        private String action;
        private String tier;
        private String risk;
        private String code;
        private String perkId;

        public NeonDeckEventData() {
        }
    }
}
