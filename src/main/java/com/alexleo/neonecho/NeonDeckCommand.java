package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;

public class NeonDeckCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonDeckCommand(NeonEchoState state) {
        super("neondeck", "Opens the NeonEcho deck interface.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw(state.formatMessage("This command can only be used in-game.")));
            return;
        }

        Player player = ctx.senderAs(Player.class);
        player.getPageManager().openCustomPage(
                player.getReference(),
                player.getWorld().getEntityStore().getStore(),
                new NeonDeckPage(player.getPlayerRef(), state)
        );
    }
}
