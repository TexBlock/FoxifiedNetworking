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

package net.fabricmc.fabric.mixin.networking;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.util.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import org.sinytra.fabric.networking_api.server.NeoServerCommonNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(ServerCommonNetworkHandler.class)
public abstract class ServerCommonNetworkHandlerMixin {

    @WrapOperation(method = "onCustomPayload", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/registration/NetworkRegistry;onMinecraftRegister(Lnet/minecraft/network/ClientConnection;Ljava/util/Set;)V"))
    public void onCustomPayloadRegisterPacket(ClientConnection connection, Set<Identifier> channels, Operation<Void> original) {
        original.call(connection, channels);
		NeoServerCommonNetworking.onRegisterPacket((ICommonPacketListener) this, channels);
    }

    @WrapOperation(method = "onCustomPayload", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/network/registration/NetworkRegistry;onMinecraftUnregister(Lnet/minecraft/network/ClientConnection;Ljava/util/Set;)V"))
    public void onCustomPayloadUnregisterPacket(ClientConnection connection, Set<Identifier> channels, Operation<Void> original) {
        original.call(connection, channels);
        NeoServerCommonNetworking.onUnregisterPacket((ICommonPacketListener) this, channels);
    }
}
