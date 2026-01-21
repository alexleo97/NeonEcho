package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NetrunCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<String> tierArg;
    private final OptionalArg<String> riskArg;

    public NetrunCommand(NeonEchoState state) {
        super("netrun", "Runs a NeonEcho netrun sequence.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.tierArg = this.withOptionalArg("tierOrCode", "Tier name or access code", ArgTypes.STRING);
        this.riskArg = this.withOptionalArg("risk", "Risk profile", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(state.formatMessage("This command can only be used in-game.")));
            return;
        }
        Player player = ctx.senderAs(Player.class);
        PlayerRef ref = player.getPlayerRef();
        UUID playerId = ref.getUuid();
        state.recordPlayerName(playerId, ref.getUsername());

        String input = ctx.provided(tierArg) ? ctx.get(tierArg) : null;
        String riskInput = ctx.provided(riskArg) ? ctx.get(riskArg) : null;
        NeonEchoState.NetrunSession active = state.getActiveSession(playerId);
        long now = System.currentTimeMillis();

        if (input != null && active != null) {
            handleAttempt(ctx, playerId, input.trim(), active);
            return;
        }

        long remainingCooldown = state.getCooldownRemainingMillis(playerId);
        if (remainingCooldown > 0) {
            sendLines(ctx, state.getNetrunCooldownLines(), tokensForCooldown(remainingCooldown));
            return;
        }

        if (active != null) {
            long secondsLeft = Math.max(0L, (active.getExpiresAt() - now) / 1000L);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("code", active.getCode());
            tokens.put("seconds", Long.toString(secondsLeft));
            tokens.put("tier", active.getTier().name());
            tokens.put("stage", Integer.toString(active.getStage()));
            tokens.put("stages", Integer.toString(active.getStages()));
            tokens.put("risk", active.getRisk().name());
            sendLines(ctx, state.getNetrunHintLines(), tokens);
            return;
        }

        String tierName = null;
        String riskName = riskInput;
        if (input != null) {
            if (isTierName(input)) {
                tierName = input;
            }
            else if (isRiskName(input)) {
                riskName = input;
            }
            else {
                ctx.sendMessage(Message.raw(state.formatMessage("Unknown tier or risk: " + input + ".")));
                sendTierList(ctx);
                sendRiskList(ctx);
                return;
            }
        }
        if (riskName != null && !riskName.isBlank() && !isRiskName(riskName)) {
            ctx.sendMessage(Message.raw(state.formatMessage("Unknown risk profile: " + riskName + ".")));
            sendRiskList(ctx);
            return;
        }

        NeonEchoState.NetrunTier tier = state.resolveNetrunTier(tierName);
        NeonEchoState.NetrunRisk risk = state.resolveNetrunRisk(riskName);
        startSession(ctx, playerId, tier, risk, now);
    }

    private void startSession(CommandContext ctx, UUID playerId, NeonEchoState.NetrunTier tier,
                              NeonEchoState.NetrunRisk risk, long now) {
        NeonEchoState.NetrunSession session = state.createSession(playerId, tier, risk, 1, 0, now);
        state.startSession(playerId, session);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("code", session.getCode());
        tokens.put("seconds", Integer.toString(session.getStageTimeoutSeconds()));
        tokens.put("tier", tier.name());
        tokens.put("stage", Integer.toString(session.getStage()));
        tokens.put("stages", Integer.toString(session.getStages()));
        tokens.put("risk", risk.name());
        sendLines(ctx, state.getNetrunStartLines(), tokens);

        int latency = ThreadLocalRandom.current().nextInt(12, 77);
        int trace = ThreadLocalRandom.current().nextInt(1, 100);
        ctx.sendMessage(Message.raw(state.formatMessage("Trace level " + trace + "%. Latency " + latency + "ms.")));
        ctx.sendMessage(Message.raw(state.formatMessage("Risk profile: " + risk.name() + ".")));
    }

    private void handleAttempt(CommandContext ctx, UUID playerId, String input, NeonEchoState.NetrunSession session) {
        String attempt = input.toUpperCase(Locale.ROOT);
        NeonEchoState.NetrunTier tier = session.getTier();
        NeonEchoState.NetrunRisk risk = session.getRisk();
        long now = System.currentTimeMillis();

        if (session.getCode().equalsIgnoreCase(attempt)) {
            if (session.getStage() < session.getStages()) {
                NeonEchoState.NetrunSession next = state.advanceSession(session, now);
                if (next == null) {
                    state.clearSession(playerId);
                }
                else {
                    state.startSession(playerId, next);
                    ctx.sendMessage(Message.raw(state.formatMessage("Stage " + session.getStage() + "/"
                            + session.getStages() + " cleared. Next code: " + next.getCode() + ".")));
                    ctx.sendMessage(Message.raw(state.formatMessage("Stage " + next.getStage() + " timer: "
                            + next.getStageTimeoutSeconds() + "s.")));
                }
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
            sendLines(ctx, state.getNetrunSuccessLines(), tokens);
            if (bonus > 0) {
                ctx.sendMessage(Message.raw(state.formatMessage("Event bonus: +" + bonus + " cred.")));
            }
            return;
        }

        int attemptsLeft = session.consumeAttempt();
        if (attemptsLeft > 0) {
            ctx.sendMessage(Message.raw(state.formatMessage("Access denied. Attempts left: " + attemptsLeft + ".")));
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
        sendLines(ctx, state.getNetrunFailLines(), tokens);
        if (penalty > 0) {
            ctx.sendMessage(Message.raw(state.formatMessage("Street Cred lost: -" + penalty + ".")));
        }
    }

    private void sendLines(CommandContext ctx, List<String> lines, Map<String, String> tokens) {
        for (String line : lines) {
            ctx.sendMessage(Message.raw(state.formatMessage(applyTokens(line, tokens))));
        }
    }

    private void sendTierList(CommandContext ctx) {
        List<String> tiers = state.getNetrunTierNames();
        if (tiers.isEmpty()) {
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Available tiers: " + String.join(", ", tiers) + ".")));
    }

    private void sendRiskList(CommandContext ctx) {
        List<String> risks = state.getNetrunRiskNames();
        if (risks.isEmpty()) {
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Risk profiles: " + String.join(", ", risks) + ".")));
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

    private Map<String, String> tokensForCooldown(long remainingMillis) {
        long seconds = Math.max(1L, remainingMillis / 1000L);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("cooldown", Long.toString(seconds));
        return tokens;
    }

    private String applyTokens(String line, Map<String, String> tokens) {
        String result = line;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
