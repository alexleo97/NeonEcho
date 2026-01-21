package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.UUID;

public class NeonAlertCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonAlertCommand(NeonEchoState state) {
        super("neonalert", "Shows active NeonEcho events.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
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

        NeonEventState event = state.getActiveEvent(playerId);
        if (event == null) {
            ctx.sendMessage(Message.raw(state.formatMessage("No active neon alerts. Keep running.")));
            return;
        }
        long now = System.currentTimeMillis();
        long secondsLeft = event.expiresAt != null ? Math.max(0L, (event.expiresAt - now) / 1000L) : 0L;

        ctx.sendMessage(Message.raw(state.formatMessage("Neon alert: " + event.name + " (" + secondsLeft + "s).")));
        if (event.description != null && !event.description.isBlank()) {
            ctx.sendMessage(Message.raw(state.formatMessage(event.description)));
        }
        if ("netrun_bonus".equalsIgnoreCase(event.type)) {
            int bonus = event.bonusCred != null ? event.bonusCred : 0;
            int uses = event.usesRemaining != null ? event.usesRemaining : 0;
            ctx.sendMessage(Message.raw(state.formatMessage("Netrun bonus: +" + bonus
                    + " cred. Uses left: " + uses + ".")));
        }
        else if ("drop".equalsIgnoreCase(event.type)) {
            int drop = event.dropCred != null ? event.dropCred : 0;
            ctx.sendMessage(Message.raw(state.formatMessage("Signal drop: " + drop
                    + " cred. Claim with /neondrop.")));
        }
    }
}
