package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;

public class NeonContractsCommand extends CommandBase {

    private final NeonEchoState state;

    public NeonContractsCommand(NeonEchoState state) {
        super("contracts", "Shows your daily NeonEcho contract.");
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
        NeonEchoState.DailyContractView daily = state.getDailyContractView(player.getPlayerRef().getUuid());
        if (!daily.enabled()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Daily contracts are disabled.")));
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Daily contract (reward " + daily.reward() + " cred):")));
        for (NeonEchoState.DailyObjectiveView objective : daily.objectives()) {
            String progress = objective.progress() + "/" + objective.target();
            ctx.sendMessage(Message.raw(state.formatMessage("- " + objective.label() + " (" + progress + ")")));
        }
        if (daily.complete() && !daily.claimed()) {
            ctx.sendMessage(Message.raw(state.formatMessage("All objectives complete. Claim with /claim.")));
        }
        else if (daily.claimed()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Contract already claimed.")));
        }
    }
}
