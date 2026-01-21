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

public class NeonNetrunStatsCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<PlayerRef> targetArg;

    public NeonNetrunStatsCommand(NeonEchoState state) {
        super("netrunstats", "Shows your netrun stats.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.targetArg = this.withOptionalArg("player", "Player to inspect", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        PlayerRef target = null;
        if (ctx.provided(targetArg)) {
            target = ctx.get(targetArg);
        }
        else if (ctx.isPlayer()) {
            Player player = ctx.senderAs(Player.class);
            target = player.getPlayerRef();
        }
        if (target == null) {
            ctx.sendMessage(Message.raw(state.formatMessage("Target player required.")));
            return;
        }
        state.recordPlayerName(target.getUuid(), target.getUsername());
        NeonEchoState.NetrunStats stats = state.getNetrunStats(target.getUuid());
        ctx.sendMessage(Message.raw(state.formatMessage("Netrun stats for " + target.getUsername() + ":")));
        ctx.sendMessage(Message.raw(state.formatMessage("Wins " + stats.wins() + ", Fails " + stats.fails()
                + ", Streak " + stats.streak() + " (best " + stats.bestStreak() + ").")));
    }
}
