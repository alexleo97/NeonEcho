package com.alexleo.neonecho;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class NeonEchoPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final NeonEchoState state;
    private final ScheduledExecutorService scheduler;

    public NeonEchoPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.state = new NeonEchoState(
                this.getName(),
                this.getManifest().getVersion().toString(),
                LOGGER,
                this.getDataDirectory()
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "neonecho-tasks");
            thread.setDaemon(true);
            return thread;
        });
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        state.load();
        this.getCommandRegistry().registerCommand(new NetrunCommand(state));
        this.getCommandRegistry().registerCommand(new NeonHelpCommand(state));
        this.getCommandRegistry().registerCommand(new NeonStatusCommand(state));
        this.getCommandRegistry().registerCommand(new NeonMuteCommand(state));
        this.getCommandRegistry().registerCommand(new NeonCredCommand(state));
        this.getCommandRegistry().registerCommand(new NeonCredTopCommand(state));
        this.getCommandRegistry().registerCommand(new NeonCredSetCommand(state));
        this.getCommandRegistry().registerCommand(new NeonNetrunStatsCommand(state));
        this.getCommandRegistry().registerCommand(new NeonProfileCommand(state));
        this.getCommandRegistry().registerCommand(new NeonContractsCommand(state));
        this.getCommandRegistry().registerCommand(new NeonClaimCommand(state));
        this.getCommandRegistry().registerCommand(new NeonPerksCommand(state));
        this.getCommandRegistry().registerCommand(new NeonAlertCommand(state));
        this.getCommandRegistry().registerCommand(new NeonDropCommand(state));
        this.getCommandRegistry().registerCommand(new NeonDeckCommand(state));
        this.getCommandRegistry().registerCommand(new NeonReloadCommand(state));
        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            state.markOnline(event.getPlayerRef(), true);
            state.recordPlayerName(event.getPlayerRef().getUuid(), event.getPlayerRef().getUsername());
            state.prepareDaily(event.getPlayerRef().getUuid());
            if (!state.isMuted(event.getPlayerRef().getUuid())) {
                String joinMessage = state.getJoinMessage();
                if (joinMessage != null && !joinMessage.isBlank()) {
                    event.getPlayer().sendMessage(Message.raw(state.formatMessage(joinMessage)));
                }
                String title = state.getTitle(event.getPlayerRef().getUuid());
                int cred = state.getCred(event.getPlayerRef().getUuid());
                event.getPlayer().sendMessage(Message.raw(state.formatMessage("Runner tag: " + title + " | Cred " + cred + ".")));
            }
        });
        this.getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            state.markOnline(event.getPlayerRef(), false);
        });
        this.getEventRegistry().registerAsyncGlobal(PlayerChatEvent.class, future -> future.thenApply(event -> {
            if (event == null || event.getSender() == null) {
                return event;
            }
            if (event.isCancelled()) {
                return event;
            }
            state.recordPlayerName(event.getSender().getUuid(), event.getSender().getUsername());
            state.tryAwardChatCred(event.getSender().getUuid(), event.getContent());
            state.recordChat(event.getSender().getUuid());
            return event;
        }));

        ScheduledFuture<?> credFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                state.tickOnline(30);
            }
            catch (Exception ex) {
                LOGGER.at(Level.WARNING).log("NeonEcho cred tick failed: " + ex.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        registerTask(credFuture);

        ScheduledFuture<?> saveFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                state.saveData();
            }
            catch (Exception ex) {
                LOGGER.at(Level.WARNING).log("NeonEcho save failed: " + ex.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
        registerTask(saveFuture);

        ScheduledFuture<?> eventFuture = scheduler.scheduleAtFixedRate(() -> {
            try {
                state.tickEvents();
            }
            catch (Exception ex) {
                LOGGER.at(Level.WARNING).log("NeonEcho events tick failed: " + ex.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
        registerTask(eventFuture);
    }

    @SuppressWarnings("unchecked")
    private void registerTask(ScheduledFuture<?> future) {
        this.getTaskRegistry().registerTask((ScheduledFuture<Void>) future);
    }
}
