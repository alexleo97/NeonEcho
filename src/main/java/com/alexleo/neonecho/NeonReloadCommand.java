package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class NeonReloadCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonReloadCommand(NeonEchoState state) {
        super("neonreload", "Reloads NeonEcho config from disk.");
        this.setPermissionGroup(GameMode.Creative);
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (state.reloadConfig()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Config reloaded. Theme: " + state.getTheme().getName() + ".")));
        }
        else {
            ctx.sendMessage(Message.raw(state.formatMessage("Config reload failed. Check logs.")));
        }
    }
}
