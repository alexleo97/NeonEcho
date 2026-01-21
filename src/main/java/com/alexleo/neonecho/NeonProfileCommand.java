package com.alexleo.neonecho;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public class NeonProfileCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<PlayerRef> targetArg;

    public NeonProfileCommand(NeonEchoState state) {
        super("neonprofile", "Shows NeonEcho runner profile.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.targetArg = this.withOptionalArg("player", "Player to inspect", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        PlayerRef target = null;
        if (ctx.provided(targetArg)) {
            target = ctx.get(targetArg);
        }
        else if (ctx.isPlayer()) {
            Player player = ctx.senderAs(Player.class);
            target = player.getPlayerRef();
        }
        if (target == null) {
            ctx.sendMessage(Message.raw(state.formatMessage("Target player required.")));
            return;
        }
        state.recordPlayerName(target.getUuid(), target.getUsername());
        int cred = state.getCred(target.getUuid());
        String title = state.getTitleForCred(cred);
        NeonEchoState.NetrunStats stats = state.getNetrunStats(target.getUuid());
        NeonEchoState.DailyContractView daily = state.getDailyContractView(target.getUuid());

        ctx.sendMessage(Message.raw(state.formatMessage("Profile for " + target.getUsername() + ":")));
        ctx.sendMessage(Message.raw(state.formatMessage("Street Cred " + cred + ". Title: " + title + ".")));
        ctx.sendMessage(Message.raw(state.formatMessage("Netrun wins " + stats.wins() + ", fails " + stats.fails()
                + ", streak " + stats.streak() + " (best " + stats.bestStreak() + ").")));
        if (!daily.enabled()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Daily contracts disabled.")));
        }
        else if (daily.complete() && !daily.claimed()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Daily contract complete. Claim " + daily.reward()
                    + " cred with /claim.")));
        }
        else if (daily.claimed()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Daily contract claimed.")));
        }
        else {
            ctx.sendMessage(Message.raw(state.formatMessage("Daily contract in progress.")));
        }
    }
}
