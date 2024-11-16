package org.thinkingstudio.foxified_networking;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.sinytra.fabric.networking_api.NetworkingEventHooks;

@Mod(FoxifiedNetworkingMod.MOD_ID)
public final class FoxifiedNetworkingMod {
    public static final String MOD_ID = "foxified_networking";

    public FoxifiedNetworkingMod(IEventBus modEventBus) {
        NetworkingEventHooks.registerEvents(modEventBus);
    }
}
