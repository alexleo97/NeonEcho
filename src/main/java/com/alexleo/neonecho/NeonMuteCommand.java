package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;

public class NeonMuteCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonMuteCommand(NeonEchoState state) {
        super("neonmute", "Toggles NeonEcho welcome messages.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(state.formatMessage("This command can only be used in-game.")));
            return;
        }
        Player player = ctx.senderAs(Player.class);
        boolean muted = state.toggleMuted(player.getPlayerRef().getUuid());
        if (muted) {
            ctx.sendMessage(Message.raw(state.formatMessage("Welcome messages muted.")));
        }
        else {
            ctx.sendMessage(Message.raw(state.formatMessage("Welcome messages enabled.")));
        }
    }
}
