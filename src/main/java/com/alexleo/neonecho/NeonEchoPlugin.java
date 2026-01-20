package com.alexleo.neonecho;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

/**
 * This class serves as the entrypoint for your plugin. Use the setup method to register into game registries or add
 * event listeners.
 */
public class NeonEchoPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final NeonEchoState state;

    public NeonEchoPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.state = new NeonEchoState(this.getName(), this.getManifest().getVersion().toString());
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new NetrunCommand(state));
        this.getCommandRegistry().registerCommand(new NeonHelpCommand());
        this.getCommandRegistry().registerCommand(new NeonStatusCommand(state));
        this.getCommandRegistry().registerCommand(new NeonMuteCommand(state));
        this.getEventRegistry().register(PlayerConnectEvent.class, event -> {
            if (!state.isMuted(event.getPlayerRef().getUuid())) {
                event.getPlayer().sendMessage(Message.raw("NeonEcho online. Welcome back, runner. Type /netrun to sync."));
            }
        });
    }
}
