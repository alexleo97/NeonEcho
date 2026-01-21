package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class NeonCredSetCommand extends CommandBase {

    private final NeonEchoState state;
    private final RequiredArg<PlayerRef> targetArg;
    private final RequiredArg<Integer> amountArg;

    public NeonCredSetCommand(NeonEchoState state) {
        super("credset", "Sets Street Cred for a runner.");
        this.setPermissionGroup(GameMode.Creative);
        this.state = state;
        this.targetArg = this.withRequiredArg("player", "Player to set", ArgTypes.PLAYER_REF);
        this.amountArg = this.withRequiredArg("amount", "Street Cred amount", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        PlayerRef target = ctx.get(targetArg);
        int amount = Math.max(0, ctx.get(amountArg));
        state.recordPlayerName(target.getUuid(), target.getUsername());
        state.setCred(target.getUuid(), amount);
        ctx.sendMessage(Message.raw(state.formatMessage("Street Cred for " + target.getUsername() + " set to " + amount + ".")));
    }
}
