package org.sinytra.fabric.networking_api.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;

import java.util.Set;

public class NeoServerPlayNetworking {
    public static <T extends CustomPayload> boolean registerGlobalReceiver(CustomPayload.Id<T> type, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.PLAY_C2S, type.id(), NetworkSide.SERVERBOUND, NetworkPhase.PLAY);
        return NeoCommonNetworking.PLAY_REGISTRY.registerGlobalReceiver(type, NetworkSide.SERVERBOUND, handler, ServerNeoContextWrapper::new, ServerPlayNetworking.PlayPayloadHandler::receive);
    }

    public static ServerPlayNetworking.PlayPayloadHandler<?> unregisterGlobalReceiver(Identifier id) {
        return NeoCommonNetworking.PLAY_REGISTRY.unregisterGlobalReceiver(id, NetworkSide.SERVERBOUND);
    }

    public static Set<Identifier> getGlobalReceivers() {
        return NeoCommonNetworking.PLAY_REGISTRY.getGlobalReceivers(NetworkSide.SERVERBOUND);
    }

    public static <T extends CustomPayload> boolean registerReceiver(ServerPlayNetworkHandler networkHandler, CustomPayload.Id<T> type, ServerPlayNetworking.PlayPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.PLAY_C2S, type.id(), NetworkSide.SERVERBOUND, NetworkPhase.PLAY);
        return NeoCommonNetworking.PLAY_REGISTRY.registerLocalReceiver(type, networkHandler, handler, ServerNeoContextWrapper::new, ServerPlayNetworking.PlayPayloadHandler::receive);
    }

    public static ServerPlayNetworking.PlayPayloadHandler<?> unregisterReceiver(ServerPlayNetworkHandler networkHandler, Identifier id) {
        return NeoCommonNetworking.PLAY_REGISTRY.unregisterLocalReceiver(id, networkHandler);
    }

    public static Set<Identifier> getReceived(ServerPlayNetworkHandler handler) throws IllegalStateException {
        return NeoCommonNetworking.PLAY_REGISTRY.getLocalReceivers(handler);
    }

    public static Set<Identifier> getSendable(ServerPlayNetworkHandler handler) throws IllegalStateException {
        return NeoCommonNetworking.PLAY_REGISTRY.getLocalSendable(handler);
    }

    public static boolean canSend(ServerPlayNetworkHandler handler, Identifier channelName) throws IllegalArgumentException {
        return NetworkRegistry.hasChannel(handler, channelName);
    }

    public static PacketSender getSender(ServerPlayNetworkHandler handler) {
        return new NeoServerPacketSender(handler.getConnection());
    }

    public static void onClientReady(ServerPlayerEntity player) {
        ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(player.networkHandler, new NeoServerPacketSender(player.networkHandler.getConnection()), player.server);
    }

    private record ServerNeoContextWrapper(IPayloadContext context) implements ServerPlayNetworking.Context {
        @Override
        public MinecraftServer server() {
            return player().getServer();
        }

        @Override
        public ServerPlayerEntity player() {
            return (ServerPlayerEntity) context.player();
        }

        @Override
        public PacketSender responseSender() {
            return new NeoServerPacketSender(context.connection());
        }
    }
}
