package org.sinytra.fabric.networking_api.server;

import net.fabricmc.fabric.api.networking.v1.S2CConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.minecraft.network.NetworkPhase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

import java.util.List;
import java.util.Set;

public class NeoServerCommonNetworking {
    public static void onRegisterPacket(ICommonPacketListener listener, Set<Identifier> ids) {
        NetworkPhase protocol = listener.getPhase();
        MinecraftServer server = ((ServerCommonNetworkHandler) listener).server;
        NeoServerPacketSender packetSender = new NeoServerPacketSender(listener.getConnection());

        if (protocol == NetworkPhase.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> S2CConfigurationChannelEvents.REGISTER.invoker().onChannelRegister((ServerConfigurationNetworkHandler) listener, packetSender, server, List.copyOf(ids)));
        } else if (protocol == NetworkPhase.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> S2CPlayChannelEvents.REGISTER.invoker().onChannelRegister((ServerPlayNetworkHandler) listener, packetSender, server, List.copyOf(ids)));
        }
    }

    public static void onUnregisterPacket(ICommonPacketListener listener, Set<Identifier> ids) {
        NetworkPhase protocol = listener.getPhase();
        MinecraftServer server = ((ServerCommonNetworkHandler) listener).server;
        NeoServerPacketSender packetSender = new NeoServerPacketSender(listener.getConnection());

        if (protocol == NetworkPhase.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> S2CConfigurationChannelEvents.UNREGISTER.invoker().onChannelUnregister((ServerConfigurationNetworkHandler) listener, packetSender, server, List.copyOf(ids)));
        } else if (protocol == NetworkPhase.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> S2CPlayChannelEvents.UNREGISTER.invoker().onChannelUnregister((ServerPlayNetworkHandler) listener, packetSender, server, List.copyOf(ids)));
        }
    }
}
