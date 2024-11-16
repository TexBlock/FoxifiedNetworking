package org.sinytra.fabric.networking_api.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record NeoServerPacketSender(ClientConnection connection) implements PacketSender {
    @Override
    public Packet<?> createPacket(CustomPayload packet) {
        return ServerPlayNetworking.createS2CPacket(packet);
    }

    @Override
    public void sendPacket(Packet<?> packet, @Nullable PacketCallbacks callback) {
        Objects.requireNonNull(packet, "Packet cannot be null");

        this.connection.send(packet, callback);
    }

    @Override
    public void disconnect(Text disconnectReason) {
        Objects.requireNonNull(disconnectReason, "Disconnect reason cannot be null");

        this.connection.disconnect(disconnectReason);
    }
}
