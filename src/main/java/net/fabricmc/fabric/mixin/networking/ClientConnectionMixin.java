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

import io.netty.channel.ChannelHandlerContext;
import org.sinytra.fabric.networking_api.NeoListenableNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions;
import net.fabricmc.fabric.impl.networking.PacketCallbackListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;

@Mixin(ClientConnection.class)
abstract class ClientConnectionMixin {
	@Shadow
	private PacketListener packetListener;

	@Inject(method = "sendImmediately", at = @At(value = "FIELD", target = "Lnet/minecraft/network/ClientConnection;packetsSentCounter:I"))
	private void checkPacket(Packet<?> packet, PacketCallbacks callback, boolean flush, CallbackInfo ci) {
		if (this.packetListener instanceof PacketCallbackListener) {
			((PacketCallbackListener) this.packetListener).sent(packet);
		}
	}

	@Inject(method = "setPacketListener", at = @At("HEAD"))
	private void unwatchAddon(NetworkState<?> state, PacketListener listener, CallbackInfo ci) {
		if (this.packetListener instanceof NetworkHandlerExtensions oldListener) {
			oldListener.getAddon().endSession();
		}
	}

	@Inject(method = "channelInactive", at = @At("HEAD"))
	private void disconnectAddon(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
		if (packetListener instanceof NetworkHandlerExtensions extension) {
			extension.getAddon().handleDisconnect();
		}
		if (packetListener instanceof NeoListenableNetworkHandler handler) {
			handler.handleDisconnect();
		}
	}

	@Inject(method = "handleDisconnection", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/listener/PacketListener;onDisconnected(Lnet/minecraft/network/DisconnectionInfo;)V"))
	private void disconnectAddon(CallbackInfo ci) {
		if (packetListener instanceof NetworkHandlerExtensions extension) {
			extension.getAddon().handleDisconnect();
		}
		if (packetListener instanceof NeoListenableNetworkHandler handler) {
			handler.handleDisconnect();
		}
	}
}
