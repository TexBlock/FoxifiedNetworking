package net.fabricmc.fabric.impl.networking.neo;

import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.minecraft.SharedConstants;
import net.minecraft.server.command.DebugConfigCommand;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.fabricmc.fabric.impl.networking.server.neo.NeoServerPlayNetworking;

public class NetworkingEventHooks {

    public static void registerEvents(IEventBus bus) {
        bus.addListener(RegisterConfigurationTasksEvent.class, event -> {
            ServerConfigurationNetworkHandler listener = (ServerConfigurationNetworkHandler) event.getListener();
            ServerConfigurationConnectionEvents.CONFIGURE.invoker().onSendConfiguration(listener, listener.server);
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
            if (SharedConstants.isDevelopment) {
                // Command is registered when isDevelopment is set.
                return;
            }

            if (FMLLoader.isProduction()) {
                // Only register this command in a dev env
                return;
            }

            DebugConfigCommand.register(event.getDispatcher());
        });
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> {
            if (event.getPlayer() != null) {
                NeoServerPlayNetworking.onClientReady(event.getPlayer());
            }
        });
    }
}
