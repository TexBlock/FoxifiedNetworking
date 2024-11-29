package net.fabricmc.fabric.mixin.networking;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.EncoderHandler;
import net.minecraft.network.handler.HandlerNames;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;
import net.neoforged.neoforge.network.payload.SplitPacketPayload;
import org.sinytra.fabric.networking_api.NeoNetworkRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GenericPacketSplitter.class)
public class GenericPacketSplitterMixin {

    /*
     * Disable NeoForge packet splitting for Fabric packets
     */

    @Inject(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    public void encode(ChannelHandlerContext ctx, Packet<?> packet, List<Object> out, CallbackInfo ci) {
        if (packet instanceof CustomPayloadS2CPacket customPayloadS2CPacket) {
            if (ctx.pipeline().get(HandlerNames.ENCODER) instanceof EncoderHandler<?> encoder) {
                var registry = NeoNetworkRegistrar.getPayloadRegistry(encoder.getProtocolInfo().id(), NetworkSide.CLIENTBOUND);
                if (registry.get(customPayloadS2CPacket.payload().getId()) != null) {
                    out.add(packet);
                    ci.cancel();
                }
            }
        } else if (packet instanceof CustomPayloadC2SPacket customPayloadC2SPacket) {
            if (ctx.pipeline().get(HandlerNames.ENCODER) instanceof EncoderHandler<?> encoder) {
                var registry = NeoNetworkRegistrar.getPayloadRegistry(encoder.getProtocolInfo().id(), NetworkSide.SERVERBOUND);
                if (registry.get(customPayloadC2SPacket.payload().getId()) != null) {
                    out.add(packet);
                    ci.cancel();
                }
            }
        }
    }

}
