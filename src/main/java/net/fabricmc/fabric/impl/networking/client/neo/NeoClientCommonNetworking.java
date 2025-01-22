package net.fabricmc.fabric.impl.networking.client.neo;

import net.fabricmc.fabric.api.client.networking.v1.C2SConfigurationChannelEvents;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.NetworkPhase;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;

import java.util.List;
import java.util.Set;

public class NeoClientCommonNetworking {
    public static void onRegisterPacket(ICommonPacketListener listener, Set<Identifier> ids) {
        NetworkPhase phase = listener.getPhase();
        if (phase == NetworkPhase.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> C2SConfigurationChannelEvents.REGISTER.invoker().onChannelRegister((ClientConfigurationNetworkHandler) listener, new NeoClientPacketSender(listener.getConnection()), MinecraftClient.getInstance(), List.copyOf(ids)));
        } else if (phase == NetworkPhase.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> C2SPlayChannelEvents.REGISTER.invoker().onChannelRegister((ClientPlayNetworkHandler) listener, new NeoClientPacketSender(listener.getConnection()), MinecraftClient.getInstance(), List.copyOf(ids)));
        }
    }

    public static void onUnregisterPacket(ICommonPacketListener listener, Set<Identifier> ids) {
        NetworkPhase phase = listener.getPhase();
        if (phase == NetworkPhase.CONFIGURATION) {
            listener.getMainThreadEventLoop().execute(() -> C2SConfigurationChannelEvents.UNREGISTER.invoker().onChannelUnregister((ClientConfigurationNetworkHandler) listener, new NeoClientPacketSender(listener.getConnection()), MinecraftClient.getInstance(), List.copyOf(ids)));
        } else if (phase == NetworkPhase.PLAY) {
            listener.getMainThreadEventLoop().execute(() -> C2SPlayChannelEvents.UNREGISTER.invoker().onChannelUnregister((ClientPlayNetworkHandler) listener, new NeoClientPacketSender(listener.getConnection()), MinecraftClient.getInstance(), List.copyOf(ids)));
        }
    }
}
