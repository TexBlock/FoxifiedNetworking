package org.sinytra.fabric.networking_api.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.sinytra.fabric.networking_api.NeoCommonNetworking;

import java.util.Objects;
import java.util.Set;

public class NeoClientConfigurationNetworking {
    private static ICommonPacketListener configurationPacketListener;

    public static <T extends CustomPayload> boolean registerGlobalReceiver(CustomPayload.Id<T> type, ClientConfigurationNetworking.ConfigurationPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_S2C, type.id(), NetworkSide.CLIENTBOUND, NetworkPhase.CONFIGURATION);
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.registerGlobalReceiver(type, NetworkSide.CLIENTBOUND, handler, ClientConfigNeoContextWrapper::new, ClientConfigurationNetworking.ConfigurationPayloadHandler::receive);
    }

    public static ClientConfigurationNetworking.ConfigurationPayloadHandler<?> unregisterGlobalReceiver(Identifier id) {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.unregisterGlobalReceiver(id, NetworkSide.CLIENTBOUND);
    }

    public static Set<Identifier> getGlobalReceivers() {
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getGlobalReceivers(NetworkSide.CLIENTBOUND);
    }

    public static <T extends CustomPayload> boolean registerReceiver(CustomPayload.Id<T> type, ClientConfigurationNetworking.ConfigurationPayloadHandler<T> handler) {
        NeoCommonNetworking.assertPayloadType(PayloadTypeRegistryImpl.CONFIGURATION_S2C, type.id(), NetworkSide.CLIENTBOUND, NetworkPhase.CONFIGURATION);
        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot register receiver while not configuring!");
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.registerLocalReceiver(type, listener, handler, ClientConfigNeoContextWrapper::new, ClientConfigurationNetworking.ConfigurationPayloadHandler::receive);
    }

    public static ClientConfigurationNetworking.ConfigurationPayloadHandler<?> unregisterReceiver(Identifier id) {
        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot unregister receiver while not configuring!");
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.unregisterLocalReceiver(id, listener);
    }

    public static Set<Identifier> getReceived() throws IllegalStateException {
        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot get a list of channels the client can receive packets on while not configuring!");
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getLocalReceivers(listener);
    }

    public static Set<Identifier> getSendable() throws IllegalStateException {
        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot get a list of channels the server can receive packets on while not configuring!");
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getLocalSendable(listener);
    }

    public static boolean canSend(Identifier channelName) throws IllegalArgumentException {
//        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot get a list of channels the server can receive packets on while not configuring!");
        return NeoCommonNetworking.CONFIGURATION_REGISTRY.getGlobalReceivers(NetworkSide.SERVERBOUND).contains(channelName);
    }

    public static PacketSender getSender() {
        ICommonPacketListener listener = Objects.requireNonNull(configurationPacketListener, "Cannot get PacketSender while not configuring!");
        return new NeoClientPacketSender(listener.getConnection());
    }

    public static void send(CustomPayload payload) {
        Objects.requireNonNull(payload, "Payload cannot be null");
        Objects.requireNonNull(payload.getId(), "CustomPayload#getId() cannot return null for payload class: " + payload.getClass());

        if (configurationPacketListener != null) {
            new NeoClientPacketSender(configurationPacketListener.getConnection()).sendPacket(payload);
            return;
        }

        throw new IllegalStateException("Cannot send packet while not configuring!");
    }

    public static void setClientConfigurationAddon(ICommonPacketListener listener) {
        configurationPacketListener = listener;
    }

    private record ClientConfigNeoContextWrapper(IPayloadContext context) implements ClientConfigurationNetworking.Context {
        @Override
        public MinecraftClient client() {
            return MinecraftClient.getInstance();
        }

        @Override
        public PacketSender responseSender() {
            return new NeoClientPacketSender(context.connection());
        }
    }
}
