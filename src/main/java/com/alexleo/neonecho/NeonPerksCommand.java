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
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class NeonPerksCommand extends CommandBase {

    private final NeonEchoState state;
    private final OptionalArg<String> actionArg;
    private final OptionalArg<String> perkArg;

    public NeonPerksCommand(NeonEchoState state) {
        super("neonperks", "Manage NeonEcho perk loadouts.");
        this.setPermissionGroup(GameMode.Adventure);
        this.state = state;
        this.actionArg = this.withOptionalArg("action", "list, buy, equip, unequip", ArgTypes.STRING);
        this.perkArg = this.withOptionalArg("perk", "Perk id", ArgTypes.STRING);
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

        if (!state.isPerksEnabled()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Perks are disabled.")));
            return;
        }

        String action = ctx.provided(actionArg) ? ctx.get(actionArg) : null;
        String perkId = ctx.provided(perkArg) ? ctx.get(perkArg) : null;
        if (action == null) {
            showPerkList(ctx, playerId);
            return;
        }

        switch (action.trim().toLowerCase(Locale.ROOT)) {
            case "list" -> showPerkList(ctx, playerId);
            case "active" -> showActivePerks(ctx, playerId);
            case "buy" -> handlePurchase(ctx, playerId, perkId);
            case "equip" -> handleEquip(ctx, playerId, perkId);
            case "unequip" -> handleUnequip(ctx, playerId, perkId);
            default -> ctx.sendMessage(Message.raw(state.formatMessage("Usage: /neonperks [list|active|buy|equip|unequip] <perk>")));
        }
    }

    private void showPerkList(CommandContext ctx, UUID playerId) {
        List<NeonEchoConfig.PerkConfig> perks = state.getPerkConfigs();
        if (perks.isEmpty()) {
            ctx.sendMessage(Message.raw(state.formatMessage("No perks available.")));
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Neon vendor inventory:")));
        for (NeonEchoConfig.PerkConfig perk : perks) {
            if (perk == null || perk.id == null) {
                continue;
            }
            int rank = state.getPerkRank(playerId, perk.id);
            int maxRank = perk.maxRank != null ? perk.maxRank : 1;
            int cost = perk.cost != null ? perk.cost : 0;
            int nextCost = cost * Math.max(1, rank + 1);
            String label = perk.name != null ? perk.name : perk.id;
            String desc = perk.description != null ? perk.description : "";
            ctx.sendMessage(Message.raw(state.formatMessage("- " + perk.id + " | " + label
                    + " [" + rank + "/" + maxRank + "] cost " + nextCost)));
            if (!desc.isBlank()) {
                ctx.sendMessage(Message.raw(state.formatMessage("  " + desc)));
            }
        }
        showActivePerks(ctx, playerId);
    }

    private void showActivePerks(CommandContext ctx, UUID playerId) {
        List<String> active = state.getActivePerks(playerId);
        int slots = state.getPerkSlots();
        if (active.isEmpty()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Active perks: none (" + slots + " slots).")));
            return;
        }
        ctx.sendMessage(Message.raw(state.formatMessage("Active perks (" + active.size() + "/" + slots + "): "
                + String.join(", ", active) + ".")));
    }

    private void handlePurchase(CommandContext ctx, UUID playerId, String perkId) {
        if (perkId == null || perkId.isBlank()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Usage: /neonperks buy <perk>.")));
            return;
        }
        NeonEchoState.PerkPurchaseResult result = state.purchasePerk(playerId, perkId);
        switch (result.status()) {
            case DISABLED -> ctx.sendMessage(Message.raw(state.formatMessage("Perks are disabled.")));
            case NOT_FOUND -> ctx.sendMessage(Message.raw(state.formatMessage("Unknown perk: " + perkId + ".")));
            case MAXED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk already maxed.")));
            case INSUFFICIENT -> ctx.sendMessage(Message.raw(state.formatMessage("Not enough cred. Cost: "
                    + result.cost() + ".")));
            case PURCHASED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk upgraded to rank "
                    + result.rank() + ". Cost: " + result.cost() + ".")));
        }
    }

    private void handleEquip(CommandContext ctx, UUID playerId, String perkId) {
        if (perkId == null || perkId.isBlank()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Usage: /neonperks equip <perk>.")));
            return;
        }
        NeonEchoState.PerkEquipResult result = state.equipPerk(playerId, perkId);
        switch (result.status()) {
            case DISABLED -> ctx.sendMessage(Message.raw(state.formatMessage("Perks are disabled.")));
            case NOT_FOUND -> ctx.sendMessage(Message.raw(state.formatMessage("Unknown perk: " + perkId + ".")));
            case NOT_OWNED -> ctx.sendMessage(Message.raw(state.formatMessage("You do not own that perk.")));
            case ALREADY_EQUIPPED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk already equipped.")));
            case NO_SLOTS -> ctx.sendMessage(Message.raw(state.formatMessage("No free perk slots ("
                    + result.slots() + ").")));
            case EQUIPPED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk equipped.")));
            case NOT_EQUIPPED, UNEQUIPPED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk state updated.")));
        }
    }

    private void handleUnequip(CommandContext ctx, UUID playerId, String perkId) {
        if (perkId == null || perkId.isBlank()) {
            ctx.sendMessage(Message.raw(state.formatMessage("Usage: /neonperks unequip <perk>.")));
            return;
        }
        NeonEchoState.PerkEquipResult result = state.unequipPerk(playerId, perkId);
        switch (result.status()) {
            case DISABLED -> ctx.sendMessage(Message.raw(state.formatMessage("Perks are disabled.")));
            case NOT_FOUND -> ctx.sendMessage(Message.raw(state.formatMessage("Unknown perk: " + perkId + ".")));
            case NOT_EQUIPPED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk is not equipped.")));
            case UNEQUIPPED -> ctx.sendMessage(Message.raw(state.formatMessage("Perk unequipped.")));
            default -> ctx.sendMessage(Message.raw(state.formatMessage("Perk state updated.")));
        }
    }
}
