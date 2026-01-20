package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class NetrunCommand extends CommandBase {

    private final NeonEchoState state;

    public NetrunCommand(NeonEchoState state) {
        super("netrun", "Runs a quick system scan from the NeonEcho plugin.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String operator = "runner";
        if (ctx.isPlayer()) {
            Player player = ctx.senderAs(Player.class);
            operator = player.getDisplayName();
        }
        int latency = ThreadLocalRandom.current().nextInt(12, 77);
        int trace = ThreadLocalRandom.current().nextInt(1, 100);
        ctx.sendMessage(Message.raw("[NeonEcho] Netrun link established, " + operator + "."));
        ctx.sendMessage(Message.raw("[NeonEcho] Trace level " + trace + "%. Latency " + latency + "ms."));
        ctx.sendMessage(Message.raw("[NeonEcho] Core v" + state.getVersion() + " online."));
    }
}
