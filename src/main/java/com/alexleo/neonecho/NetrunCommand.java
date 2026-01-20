package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;

/**
 * This is an example command that will simply print the name of the plugin in chat when used.
 */
public class NetrunCommand extends CommandBase {

    private final String pluginName;
    private final String pluginVersion;

    public NetrunCommand(String pluginName, String pluginVersion) {
        super("netrun", "Runs a quick system scan from the " + pluginName + " plugin.");
        this.setPermissionGroup(GameMode.Adventure); // Allows the command to be used by anyone, not just OP
        this.pluginName = pluginName;
        this.pluginVersion = pluginVersion;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        String operator = "runner";
        if (ctx.isPlayer()) {
            Player player = ctx.senderAs(Player.class);
            operator = player.getDisplayName();
        }
        ctx.sendMessage(Message.raw("[NeonEcho] Netrun link established, " + operator + "."));
        ctx.sendMessage(Message.raw("[NeonEcho] Street cred synced. Core v" + pluginVersion + " online."));
    }
}
