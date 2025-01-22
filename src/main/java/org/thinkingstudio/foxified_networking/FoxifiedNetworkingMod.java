package org.thinkingstudio.foxified_networking;

import net.fabricmc.fabric.impl.networking.neo.NeoNetworkingImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FoxifiedNetworkingMod.MOD_ID)
public final class FoxifiedNetworkingMod {
    public static final String MOD_ID = "foxified_networking";

    public FoxifiedNetworkingMod(IEventBus modEventBus) {
        NeoNetworkingImpl.registerEvents(modEventBus);
    }
}
