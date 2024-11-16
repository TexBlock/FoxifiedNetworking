/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.mixin.networking.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.sinytra.fabric.networking_api.client.NeoClientPlayNetworking;
import org.sinytra.fabric.networking_api.NeoListenableNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// We want to apply a bit earlier than other mods which may not use us in order to prevent refCount issues
@Mixin(value = ClientPlayNetworkHandler.class, priority = 999)
abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler implements NeoListenableNetworkHandler {
    protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initAddon(CallbackInfo ci) {
        NeoClientPlayNetworking.setTempPacketListener((ClientPlayNetworkHandler) (Object) this);
        ClientPlayConnectionEvents.INIT.invoker().onPlayInit((ClientPlayNetworkHandler) (Object) this, this.client);
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void handleServerPlayReady(GameJoinS2CPacket packet, CallbackInfo ci) {
        NeoClientPlayNetworking.onServerReady((ClientPlayNetworkHandler) (Object) this, this.client);
    }

    @Override
    public void handleDisconnect() {
        ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect((ClientPlayNetworkHandler) (Object) this, this.client);
    }
}
