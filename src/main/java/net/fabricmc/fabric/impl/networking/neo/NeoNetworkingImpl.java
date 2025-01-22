package net.fabricmc.fabric.impl.networking.neo;

import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.SharedConstants;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.server.command.DebugConfigCommand;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.fabricmc.fabric.impl.networking.server.neo.NeoServerPlayNetworking;

public class NeoNetworkingImpl {
    public static final NeoNetworkRegistrar CONFIGURATION_REGISTRY = new NeoNetworkRegistrar(NetworkPhase.CONFIGURATION);
    public static final NeoNetworkRegistrar PLAY_REGISTRY = new NeoNetworkRegistrar(NetworkPhase.PLAY);

    public static final int DEFAULT_CHANNEL_NAME_MAX_LENGTH = 128;

    public static void assertPayloadType(PayloadTypeRegistryImpl<?> payloadTypeRegistry, Identifier channelName, NetworkSide side, NetworkPhase phase) {
        if (payloadTypeRegistry == null) {
            return;
        }

        if (payloadTypeRegistry.get(channelName) == null) {
            throw new IllegalArgumentException(String.format("Cannot register handler as no payload type has been registered with name \"%s\" for %s %s", channelName, side, phase));
        }

        if (channelName.toString().length() > DEFAULT_CHANNEL_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(String.format("Cannot register handler for channel with name \"%s\" as it exceeds the maximum length of 128 characters", channelName));
        }
    }

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
