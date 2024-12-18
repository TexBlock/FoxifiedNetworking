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

package net.fabricmc.fabric.impl.networking.server;

import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.impl.networking.GlobalReceiverRegistry;
import net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions;
import net.minecraft.network.NetworkPhase;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import java.util.Objects;

public final class ServerNetworkingImpl {
	public static final GlobalReceiverRegistry<ServerLoginNetworking.LoginQueryResponseHandler> LOGIN = new GlobalReceiverRegistry<>(NetworkSide.SERVERBOUND, NetworkPhase.LOGIN, null);

	public static ServerLoginNetworkAddon getAddon(ServerLoginNetworkHandler handler) {
		return (ServerLoginNetworkAddon) ((NetworkHandlerExtensions) handler).getAddon();
	}

	public static Packet<ClientCommonPacketListener> createS2CPacket(CustomPayload payload) {
		Objects.requireNonNull(payload, "Payload cannot be null");
		Objects.requireNonNull(payload.getId(), "CustomPayload#getId() cannot return null for payload class: " + payload.getClass());

		return new CustomPayloadS2CPacket(payload);
	}
}
