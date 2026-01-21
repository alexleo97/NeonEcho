package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.util.List;

public class NeonCredTopCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<Integer> limitArg;

    public NeonCredTopCommand(NeonEchoState state) {
        super("credtop", "Shows the highest Street Cred runners.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.limitArg = this.withOptionalArg("limit", "How many to list", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        int limit = state.getConfig().credTopLimit != null ? state.getConfig().credTopLimit : 5;
        if (ctx.provided(limitArg)) {
            limit = Math.max(1, ctx.get(limitArg));
        }
        List<NeonEchoState.CredEntry> top = state.getTopCred(limit);
        if (top.isEmpty()) {
            ctx.sendMessage(Message.raw(state.formatMessage("No Street Cred recorded yet.")));
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Top Street Cred:")));
        int rank = 1;
        for (NeonEchoState.CredEntry entry : top) {
            ctx.sendMessage(Message.raw(state.formatMessage(rank + ") " + entry.displayName() + " - " + entry.cred())));
            rank++;
        }
    }
}
