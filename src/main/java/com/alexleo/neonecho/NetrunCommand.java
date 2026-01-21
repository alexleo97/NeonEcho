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
    private final OptionalArg<String> inputArg;

    public NetrunCommand(NeonEchoState state) {
        super("netrun", "Runs a NeonEcho netrun sequence.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.inputArg = this.withOptionalArg("codeOrTier", "Tier name or access code", ArgTypes.STRING);
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

        String input = ctx.provided(inputArg) ? ctx.get(inputArg) : null;
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

        if (input != null && isTierName(input)) {
            NeonEchoState.NetrunTier tier = state.resolveNetrunTier(input);
            startSession(ctx, playerId, tier, now);
            return;
        }

        if (input != null) {
            ctx.sendMessage(Message.raw(state.formatMessage("No active netrun. Start with /netrun or /netrun <tier>.")));
            sendTierList(ctx);
            return;
        }

        if (active != null) {
            long secondsLeft = Math.max(0L, (active.getExpiresAt() - now) / 1000L);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("code", active.getCode());
            tokens.put("seconds", Long.toString(secondsLeft));
            tokens.put("tier", active.getTier().name());
            sendLines(ctx, state.getNetrunHintLines(), tokens);
            return;
        }

        NeonEchoState.NetrunTier tier = state.resolveNetrunTier(null);
        startSession(ctx, playerId, tier, now);
    }

    private void startSession(CommandContext ctx, UUID playerId, NeonEchoState.NetrunTier tier, long now) {
        String code = state.generateCode(tier.codeLength());
        long expiresAt = now + tier.timeoutSeconds() * 1000L;
        state.startSession(playerId, code, expiresAt, tier.attempts(), tier);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("code", code);
        tokens.put("seconds", Integer.toString(tier.timeoutSeconds()));
        tokens.put("tier", tier.name());
        sendLines(ctx, state.getNetrunStartLines(), tokens);

        int latency = ThreadLocalRandom.current().nextInt(12, 77);
        int trace = ThreadLocalRandom.current().nextInt(1, 100);
        ctx.sendMessage(Message.raw(state.formatMessage("Trace level " + trace + "%. Latency " + latency + "ms.")));
    }

    private void handleAttempt(CommandContext ctx, UUID playerId, String input, NeonEchoState.NetrunSession session) {
        String attempt = input.toUpperCase(Locale.ROOT);
        NeonEchoState.NetrunTier tier = session.getTier();

        if (session.getCode().equalsIgnoreCase(attempt)) {
            state.clearSession(playerId);
            state.startCooldown(playerId, tier.cooldownSeconds());
            int streak = state.recordNetrunWin(playerId);
            state.addCred(playerId, tier.reward());

            Map<String, String> tokens = new HashMap<>();
            tokens.put("cred", Integer.toString(tier.reward()));
            tokens.put("cooldown", Integer.toString(tier.cooldownSeconds()));
            tokens.put("tier", tier.name());
            tokens.put("streak", Integer.toString(streak));
            sendLines(ctx, state.getNetrunSuccessLines(), tokens);
            return;
        }

        int attemptsLeft = session.consumeAttempt();
        if (attemptsLeft > 0) {
            ctx.sendMessage(Message.raw(state.formatMessage("Access denied. Attempts left: " + attemptsLeft + ".")));
            return;
        }

        state.clearSession(playerId);
        state.startCooldown(playerId, tier.cooldownSeconds());
        state.recordNetrunFail(playerId);

        if (tier.failPenalty() > 0) {
            state.addCred(playerId, -tier.failPenalty());
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("cooldown", Integer.toString(tier.cooldownSeconds()));
        tokens.put("tier", tier.name());
        sendLines(ctx, state.getNetrunFailLines(), tokens);
        if (tier.failPenalty() > 0) {
            ctx.sendMessage(Message.raw(state.formatMessage("Street Cred lost: -" + tier.failPenalty() + ".")));
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

    private boolean isTierName(String input) {
        for (String tier : state.getNetrunTierNames()) {
            if (tier.equalsIgnoreCase(input)) {
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
