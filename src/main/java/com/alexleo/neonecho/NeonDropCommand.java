package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.util.UUID;

public class NeonDropCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonDropCommand(NeonEchoState state) {
        super("neondrop", "Claim active NeonEcho drops.");
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
        PlayerRef ref = player.getPlayerRef();
        UUID playerId = ref.getUuid();
        state.recordPlayerName(playerId, ref.getUsername());

        NeonEchoState.EventClaimResult result = state.claimEventDrop(playerId);
        switch (result.status()) {
            case NO_EVENT -> ctx.sendMessage(Message.raw(state.formatMessage("No active drop to claim.")));
            case NOT_DROP -> ctx.sendMessage(Message.raw(state.formatMessage("Active alert has no drops.")));
            case EXPIRED -> ctx.sendMessage(Message.raw(state.formatMessage("Drop window expired.")));
            case CLAIMED -> ctx.sendMessage(Message.raw(state.formatMessage("Drop claimed: +"
                    + result.reward() + " cred.")));
        }
    }
}
