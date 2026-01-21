package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;
import java.time.Duration;

public class NeonStatusCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonStatusCommand(NeonEchoState state) {
        super("neonstatus", "Shows NeonEcho status.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        Duration uptime = state.getUptime();
        ctx.sendMessage(Message.raw(state.formatMessage("Core v" + state.getVersion() + " online.")));
        ctx.sendMessage(Message.raw(state.formatMessage("Theme " + state.getTheme().getName() + ".")));
        ctx.sendMessage(Message.raw(state.formatMessage("Uptime " + formatDuration(uptime) + ".")));
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        }
        return String.format("%ds", secs);
    }
}
