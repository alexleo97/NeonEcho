package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;

public class NeonClaimCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonClaimCommand(NeonEchoState state) {
        super("claim", "Claims your daily contract reward.");
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
        NeonEchoState.ClaimResult result = state.claimDaily(player.getPlayerRef().getUuid());
        switch (result.status()) {
            case DISABLED -> ctx.sendMessage(Message.raw(state.formatMessage("Daily contracts are disabled.")));
            case NO_CONTRACT -> ctx.sendMessage(Message.raw(state.formatMessage("No daily contract available.")));
            case ALREADY_CLAIMED -> ctx.sendMessage(Message.raw(state.formatMessage("Reward already claimed.")));
            case NOT_COMPLETE -> ctx.sendMessage(Message.raw(state.formatMessage("Complete all objectives before claiming.")));
            case CLAIMED -> ctx.sendMessage(Message.raw(state.formatMessage("Cred awarded: +" + result.reward() + ".")));
            default -> ctx.sendMessage(Message.raw(state.formatMessage("Unable to claim reward.")));
        }
    }
}
