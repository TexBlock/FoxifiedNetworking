package net.fabricmc.fabric.impl.networking.neo;

import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.fabricmc.fabric.mixin.networking.accessor.neo.NetworkRegistryAccessor;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NeoNetworkRegistrar {
    // Not our actual codec, see NetworkRegistryMixin
    public static final PacketCodec<?, ?> DUMMY_CODEC = PacketCodec.ofStatic((a, b) -> {
        throw new UnsupportedOperationException();
    }, a -> {
        throw new UnsupportedOperationException();
    });

    private final NetworkPhase phase;

    private final Map<Identifier, NeoPayloadHandler<?>> registeredPayloads = new HashMap<>();

    public NeoNetworkRegistrar(NetworkPhase phase) {
        this.phase = phase;
    }

    public static boolean hasCodecFor(NetworkPhase phase, NetworkSide side, Identifier id) {
        PayloadTypeRegistryImpl<? extends PacketByteBuf> registry = getPayloadRegistry(phase, side);
        return registry.get(id) != null;
    }

    public static PayloadTypeRegistryImpl<? extends PacketByteBuf> getPayloadRegistry(NetworkPhase phase, NetworkSide side) {
        if (phase == NetworkPhase.PLAY) {
            return side == NetworkSide.SERVERBOUND ? PayloadTypeRegistryImpl.PLAY_C2S : PayloadTypeRegistryImpl.PLAY_S2C;
        } else if (phase == NetworkPhase.CONFIGURATION) {
            return side == NetworkSide.SERVERBOUND ? PayloadTypeRegistryImpl.CONFIGURATION_C2S : PayloadTypeRegistryImpl.CONFIGURATION_S2C;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public <P extends CustomPayload, C, H> boolean registerGlobalReceiver(CustomPayload.Id<P> type, NetworkSide side, H handler, Function<IPayloadContext, C> ctxFactory, TriConsumer<H, P, C> consumer) {
        NeoPayloadHandler<P> neoHandler = getOrRegisterNativeHandler(type);
        return neoHandler.registerGlobalHandler(side, handler, ctxFactory, consumer);
    }

    public <H> H unregisterGlobalReceiver(Identifier id, NetworkSide side) {
        NeoPayloadHandler<?> neoHandler = registeredPayloads.get(id);
        return neoHandler != null ? neoHandler.unregisterGlobalHandler(side) : null;
    }

    public Set<Identifier> getGlobalReceivers(NetworkSide side) {
        return registeredPayloads.entrySet().stream()
            .filter(e -> e.getValue().hasGlobalHandler(side))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public <P extends CustomPayload, C, H> boolean registerLocalReceiver(CustomPayload.Id<P> type, ICommonPacketListener listener, H handler, Function<IPayloadContext, C> ctxFactory, TriConsumer<H, P, C> consumer) {
        NeoPayloadHandler<P> neoHandler = getOrRegisterNativeHandler(type);
        return neoHandler.registerLocalReceiver(listener, handler, ctxFactory, consumer);
    }

    public <H> H unregisterLocalReceiver(Identifier id, ICommonPacketListener listener) {
        NeoPayloadHandler<?> neoHandler = registeredPayloads.get(id);
        return neoHandler != null ? neoHandler.unregisterLocalHandler(listener) : null;
    }

    public Set<Identifier> getLocalReceivers(ICommonPacketListener listener) {
        return registeredPayloads.entrySet().stream()
            .filter(e -> e.getValue().hasLocalHandler(listener))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public Set<Identifier> getLocalSendable(ICommonPacketListener listener) {
        NetworkPayloadSetup payloadSetup = ChannelAttributes.getPayloadSetup(listener.getConnection());
        if (payloadSetup == null) {
            return Set.of();
        }
        return payloadSetup.channels().get(this.phase).keySet();
    }

    @SuppressWarnings("unchecked")
    private <P extends CustomPayload> NeoPayloadHandler<P> getOrRegisterNativeHandler(CustomPayload.Id<P> type) {
        return (NeoPayloadHandler<P>) registeredPayloads.computeIfAbsent(type.id(), k -> {
            NeoPayloadHandler<P> handler = new NeoPayloadHandler<>();
            boolean setup = NetworkRegistryAccessor.getSetup();

            NetworkRegistryAccessor.setSetup(false);
            NetworkRegistry.register(type, (PacketCodec<? super PacketByteBuf, P>) DUMMY_CODEC, handler, List.of(phase), Optional.empty(), "1.0", true);
            NetworkRegistryAccessor.setSetup(setup);

            // TODO Send registration message when registering late
            return handler;
        });
    }

    public static class NeoPayloadHandler<P extends CustomPayload> implements IPayloadHandler<P> {
        private final Map<NetworkSide, NeoSubHandler<P, ?, ?>> globalReceivers = new HashMap<>();
        private final Map<ICommonPacketListener, NeoSubHandler<P, ?, ?>> localReceivers = new HashMap<>();

        @Override
        public void handle(P arg, IPayloadContext context) {
            NeoSubHandler globalHandler = globalReceivers.get(context.flow());
            if (globalHandler != null) {
                context.enqueueWork(() -> globalHandler.consumer().accept(globalHandler.handler(), arg, globalHandler.ctxFactory().apply(context)));
            }
            NeoSubHandler localHandler = localReceivers.get(context.listener());
            if (localHandler != null) {
                context.enqueueWork(() -> localHandler.consumer().accept(localHandler.handler(), arg, localHandler.ctxFactory().apply(context)));
            }
        }

        public boolean hasGlobalHandler(NetworkSide flow) {
            return globalReceivers.containsKey(flow);
        }

        public <C, H> boolean registerGlobalHandler(NetworkSide side, H original, Function<IPayloadContext, C> ctxFactory, TriConsumer<H, P, C> consumer) {
            if (!hasGlobalHandler(side)) {
                globalReceivers.put(side, new NeoSubHandler<>(original, ctxFactory, consumer));
                return true;
            }
            return false;
        }

        public boolean hasLocalHandler(ICommonPacketListener listener) {
            return localReceivers.containsKey(listener);
        }

        public <C, H> boolean registerLocalReceiver(ICommonPacketListener listener, H original, Function<IPayloadContext, C> ctxFactory, TriConsumer<H, P, C> consumer) {
            if (!hasLocalHandler(listener)) {
                localReceivers.put(listener, new NeoSubHandler<>(original, ctxFactory, consumer));
                return true;
            }
            return false;
        }

        @Nullable
        public <H> H unregisterGlobalHandler(NetworkSide side) {
            NeoSubHandler subHandler = globalReceivers.remove(side);
            return subHandler != null ? (H) subHandler.handler() : null;
        }

        @Nullable
        public <H> H unregisterLocalHandler(ICommonPacketListener listener) {
            NeoSubHandler subHandler = localReceivers.remove(listener);
            return subHandler != null ? (H) subHandler.handler() : null;
        }
    }

    record NeoSubHandler<P extends CustomPayload, C, H>(H handler, Function<IPayloadContext, C> ctxFactory, TriConsumer<H, P, C> consumer) { }
}
