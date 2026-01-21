package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

public class NeonHelpCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonHelpCommand(NeonEchoState state) {
        super("neonhelp", "Shows NeonEcho command help.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        ctx.sendMessage(Message.raw(state.formatMessage("Commands:")));
        ctx.sendMessage(Message.raw(state.formatMessage("/netrun [tier] [risk] - start or complete a netrun")));
        ctx.sendMessage(Message.raw(state.formatMessage("/netrunstats - netrun stats")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonstatus - version + uptime")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonmute - toggle join message")));
        ctx.sendMessage(Message.raw(state.formatMessage("/cred - check Street Cred")));
        ctx.sendMessage(Message.raw(state.formatMessage("/credtop - top Street Cred")));
        ctx.sendMessage(Message.raw(state.formatMessage("/credset - set Street Cred (admin)")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonprofile - runner profile")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonperks - perk loadout + vendor")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonalert - active neon events")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neondrop - claim event drops")));
        ctx.sendMessage(Message.raw(state.formatMessage("/contracts - daily objectives")));
        ctx.sendMessage(Message.raw(state.formatMessage("/claim - claim daily reward")));
        ctx.sendMessage(Message.raw(state.formatMessage("/neonreload - reload config (admin)")));
    }
}
