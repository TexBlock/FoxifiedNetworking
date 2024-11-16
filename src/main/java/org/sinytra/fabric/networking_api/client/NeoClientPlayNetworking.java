package org.sinytra.fabric.networking_api.client;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.jetbrains.annotations.Nullable;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

public class NeoClientPlayNetworking {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static ICommonPacketListener tempPacketListener;

    public static <T extends CustomPayload> boolean registerGlobalReceiver(CustomPayload.Id<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.PLAY_S2C, type.id(), NetworkSide.CLIENTBOUND, NetworkPhase.PLAY);
        return NeoCommonNetworking.PLAY_REGISTRY.registerGlobalReceiver(type, NetworkSide.CLIENTBOUND, handler, ClientNeoContextWrapper::new, ClientPlayNetworking.PlayPayloadHandler::receive);
    }

    public static ClientPlayNetworking.PlayPayloadHandler<?> unregisterGlobalReceiver(Identifier id) {
        return NeoCommonNetworking.PLAY_REGISTRY.unregisterGlobalReceiver(id, NetworkSide.CLIENTBOUND);
    }

    public static Set<Identifier> getGlobalReceivers() {
        return NeoCommonNetworking.PLAY_REGISTRY.getGlobalReceivers(NetworkSide.CLIENTBOUND);
    }

    public static <T extends CustomPayload> boolean registerReceiver(CustomPayload.Id<T> type, ClientPlayNetworking.PlayPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.PLAY_S2C, type.id(), NetworkSide.CLIENTBOUND, NetworkPhase.PLAY);
        ICommonPacketListener listener = Objects.requireNonNull(getClientListener(), "Cannot register receiver while not in game!");
        return NeoCommonNetworking.PLAY_REGISTRY.registerLocalReceiver(type, listener, handler, ClientNeoContextWrapper::new, ClientPlayNetworking.PlayPayloadHandler::receive);
    }

    public static ClientPlayNetworking.PlayPayloadHandler<?> unregisterReceiver(Identifier id) {
        ICommonPacketListener listener = Objects.requireNonNull(getClientListener(), "Cannot unregister receiver while not in game!");
        return NeoCommonNetworking.PLAY_REGISTRY.unregisterLocalReceiver(id, listener);
    }

    public static Set<Identifier> getReceived() throws IllegalStateException {
        ICommonPacketListener listener = Objects.requireNonNull(getClientListener(), "Cannot get a list of channels the client can receive packets on while not in game!");
        return NeoCommonNetworking.PLAY_REGISTRY.getLocalReceivers(listener);
    }

    public static Set<Identifier> getSendable() throws IllegalStateException {
        ICommonPacketListener listener = Objects.requireNonNull(getClientListener(), "Cannot get a list of channels the server can receive packets on while not in game!");
        return NeoCommonNetworking.PLAY_REGISTRY.getLocalSendable(listener);
    }

    public static boolean canSend(Identifier channelName) throws IllegalArgumentException {
        return NetworkRegistry.hasChannel(MinecraftClient.getInstance().getNetworkHandler(), channelName);
    }

    public static PacketSender getSender() {
        return new NeoClientPacketSender(MinecraftClient.getInstance().getNetworkHandler().getConnection());
    }

    public static void onServerReady(ClientPlayNetworkHandler handler, MinecraftClient client) {
        try {
            ClientPlayConnectionEvents.JOIN.invoker().onPlayReady(handler, new NeoClientPacketSender(handler.getConnection()), client);
        } catch (RuntimeException e) {
            LOGGER.error("Exception thrown while invoking ClientPlayConnectionEvents.JOIN", e);
        }
    }

    @Nullable
    private static ICommonPacketListener getClientListener() {
        // Since Minecraft can be a bit weird, we need to check for the play addon in a few ways:
        // If the client's player is set this will work
        if (MinecraftClient.getInstance().getNetworkHandler() != null) {
            tempPacketListener = null; // Shouldn't need this anymore
            return MinecraftClient.getInstance().getNetworkHandler();
        }

        // We haven't hit the end of onGameJoin yet, use our backing field here to access the network handler
        if (tempPacketListener != null) {
            return tempPacketListener;
        }

        // We are not in play stage
        return null;
    }
    
    public static void setTempPacketListener(ICommonPacketListener listener) {
        tempPacketListener = listener;
    }

    private record ClientNeoContextWrapper(IPayloadContext context) implements ClientPlayNetworking.Context {
        @Override
        public MinecraftClient client() {
            return MinecraftClient.getInstance();
        }

        @Override
        public ClientPlayerEntity player() {
            return (ClientPlayerEntity) context.player();
        }

        @Override
        public PacketSender responseSender() {
            return new NeoClientPacketSender(context.connection());
        }
    }
}
