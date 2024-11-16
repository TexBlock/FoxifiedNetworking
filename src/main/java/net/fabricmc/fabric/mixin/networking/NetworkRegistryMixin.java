package net.fabricmc.fabric.mixin.networking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.listener.ServerConfigurationPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.negotiation.NegotiatedNetworkComponent;
import net.neoforged.neoforge.network.negotiation.NegotiationResult;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.sinytra.fabric.networking_api.NeoNetworkRegistrar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin {

    @Inject(method = "getCodec", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 0), cancellable = true)
    private static void getCodec(Identifier id, NetworkPhase protocol, NetworkSide flow, CallbackInfoReturnable<PacketCodec<? super PacketByteBuf, ? extends CustomPayload>> cir) {
        PayloadTypeRegistryImpl<? extends PacketByteBuf> registry = NeoNetworkRegistrar.getPayloadRegistry(protocol, flow);
        CustomPayload.Type<? extends PacketByteBuf, ? extends CustomPayload> fabricCodec = registry.get(id);
        if (fabricCodec != null) {
            cir.setReturnValue((PacketCodec) fabricCodec.codec());
        }
    }

    @ModifyReturnValue(method = "getCodec", at = @At(value = "RETURN", ordinal = 3))
    private static PacketCodec<? super PacketByteBuf, ? extends CustomPayload> getFabricDynamicCodec(PacketCodec<? super PacketByteBuf, ? extends CustomPayload> codec, Identifier id, NetworkPhase protocol, NetworkSide flow) {
        if (codec == NeoNetworkRegistrar.DUMMY_CODEC) {
            PayloadTypeRegistryImpl<? extends PacketByteBuf> registry = NeoNetworkRegistrar.getPayloadRegistry(protocol, flow);
            CustomPayload.Type<? extends PacketByteBuf, ? extends CustomPayload> fabricCodec = registry.get(id);
            if (fabricCodec != null) {
                return (PacketCodec) fabricCodec.codec();
            }
        }
        return codec;
    }

    @Inject(method = "handleModdedPayload(Lnet/minecraft/network/listener/ClientCommonPacketListener;Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;)V", ordinal = 1), cancellable = true)
    private static void preventDisconnectOnUnknownFabricPacketClient(ClientCommonPacketListener listener, CustomPayloadS2CPacket packet, CallbackInfo info) {
        if (NeoNetworkRegistrar.hasCodecFor(listener.getPhase(), packet.getPacketId().side(), packet.payload().getId().id())) {
            info.cancel();
        }
    }

    @Inject(method = "handleModdedPayload(Lnet/minecraft/network/listener/ServerCommonPacketListener;Lnet/minecraft/network/packet/c2s/common/CustomPayloadC2SPacket;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"), cancellable = true)
    private static void preventDisconnectOnUnknownFabricPacketServer(ServerCommonPacketListener listener, CustomPayloadC2SPacket packet, CallbackInfo info) {
        if (NeoNetworkRegistrar.hasCodecFor(listener.getPhase(), packet.getPacketId().side(), packet.payload().getId().id())) {
            info.cancel();
        }
    }

    @WrapOperation(
        method = {
            "checkPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/ClientCommonPacketListener;)V",
            "checkPacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/ServerCommonPacketListener;)V"
        },
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/registration/NetworkRegistry;hasChannel(Lnet/neoforged/neoforge/common/extensions/ICommonPacketListener;Lnet/minecraft/util/Identifier;)Z")
    )
    private static boolean includeFabricChannels(ICommonPacketListener listener, Identifier location, Operation<Boolean> original) {
        // TODO Use original args that include the packet
        return original.call(listener, location) || NeoNetworkRegistrar.hasCodecFor(listener.getPhase(), listener.getSide() == NetworkSide.SERVERBOUND ? NetworkSide.CLIENTBOUND : NetworkSide.SERVERBOUND, location);
    }

    @ModifyVariable(method = "initializeNeoForgeConnection", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/registration/NetworkPayloadSetup;from(Ljava/util/Map;)Lnet/neoforged/neoforge/network/registration/NetworkPayloadSetup;"), ordinal = 1)
    private static Map<NetworkPhase, NegotiationResult> preserveSendableChannels(Map<NetworkPhase, NegotiationResult> results, ServerConfigurationPacketListener listener, Map<NetworkPhase, Set<ModdedNetworkQueryComponent>> clientChannels) {
        Set<ModdedNetworkQueryComponent> channels = clientChannels.get(NetworkPhase.PLAY);
        if (channels != null && !channels.isEmpty()) {
            NegotiationResult negotiation = results.get(NetworkPhase.PLAY);
            List<NegotiatedNetworkComponent> components = new ArrayList<>(negotiation.components());
            channels.stream()
                .filter(c -> components.stream().noneMatch(d -> c.id().equals(d.id())) && PayloadTypeRegistryImpl.PLAY_S2C.get(c.id()) != null)
                .forEach(c -> components.add(new NegotiatedNetworkComponent(c.id(), c.version())));
            results.put(NetworkPhase.PLAY, new NegotiationResult(components, negotiation.success(), negotiation.failureReasons()));
        }
        return results;
    }
}
