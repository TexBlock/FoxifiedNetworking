package org.sinytra.fabric.networking_api.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;

import java.util.Set;

public class NeoServerConfigurationNetworking {

    public static <T extends CustomPayload> boolean registerGlobalReceiver(CustomPayload.Id<T> type, ServerConfigurationNetworking.ConfigurationPacketHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_C2S, type.id(), NetworkSide.SERVERBOUND, NetworkPhase.CONFIGURATION);
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.registerGlobalReceiver(type, NetworkSide.SERVERBOUND, handler, ServerConfigNeoContextWrapper::new, ServerConfigurationNetworking.ConfigurationPacketHandler::receive);
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?> unregisterGlobalReceiver(Identifier id) {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.unregisterGlobalReceiver(id, NetworkSide.SERVERBOUND);
    }

    public static Set<Identifier> getGlobalReceivers() {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getGlobalReceivers(NetworkSide.SERVERBOUND);
    }

    public static <T extends CustomPayload> boolean registerReceiver(ServerConfigurationNetworkHandler networkHandler, CustomPayload.Id<T> type, ServerConfigurationNetworking.ConfigurationPacketHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_C2S, type.id(), NetworkSide.SERVERBOUND, NetworkPhase.CONFIGURATION);
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.registerLocalReceiver(type, networkHandler, handler, ServerConfigNeoContextWrapper::new, ServerConfigurationNetworking.ConfigurationPacketHandler::receive);
    }

    public static ServerConfigurationNetworking.ConfigurationPacketHandler<?> unregisterReceiver(ServerConfigurationNetworkHandler networkHandler, Identifier id) {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.unregisterLocalReceiver(id, networkHandler);
    }

    public static Set<Identifier> getReceived(ServerConfigurationNetworkHandler handler) throws IllegalStateException {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getLocalReceivers(handler);
    }

    public static Set<Identifier> getSendable(ServerConfigurationNetworkHandler handler) throws IllegalStateException {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getLocalSendable(handler);
    }

    public static boolean canSend(ServerConfigurationNetworkHandler handler, Identifier channelName) throws IllegalArgumentException {
        return NetworkRegistry.hasChannel(handler, channelName);
    }

    public static PacketSender getSender(ServerConfigurationNetworkHandler handler) {
        return new NeoServerPacketSender(handler.getConnection());
    }

    private record ServerConfigNeoContextWrapper(IPayloadContext context) implements ServerConfigurationNetworking.Context {
        @Override
        public MinecraftServer server() {
            return networkHandler().server;
        }

        @Override
        public ServerConfigurationNetworkHandler networkHandler() {
            return (ServerConfigurationNetworkHandler) context.listener();
        }

        @Override
        public PacketSender responseSender() {
            return new NeoServerPacketSender(context.connection());
        }
    }
}
