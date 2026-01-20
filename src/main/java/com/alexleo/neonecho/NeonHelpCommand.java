package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class NeonHelpCommand extends CommandBase {

    public NeonHelpCommand() {
        super("neonhelp", "Shows NeonEcho command help.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw("[NeonEcho] Commands:"));
        ctx.sendMessage(Message.raw("[NeonEcho] /netrun - quick status readout"));
        ctx.sendMessage(Message.raw("[NeonEcho] /neonstatus - version + uptime"));
        ctx.sendMessage(Message.raw("[NeonEcho] /neonmute - toggle join message"));
    }
}
