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

public class NeonCredCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<PlayerRef> targetArg;

    public NeonCredCommand(NeonEchoState state) {
        super("cred", "Shows Street Cred for you or a target.");
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
        int cred = state.getCred(target.getUuid());
        ctx.sendMessage(Message.raw(state.formatMessage("Street Cred for " + target.getUsername() + ": " + cred + ".")));
    }
}
