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

package net.fabricmc.fabric.test.networking.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.test.networking.NetworkingTestmods;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;

public class NetworkingCommonTest {
	private boolean firstLoad = true;
	private List<String> receivedPlay = new ArrayList<>();
	private List<String> receivedConfig = new ArrayList<>();

	public void onInitialize() {
		// Register the payload on both sides for play and configuration
		PayloadTypeRegistry.playS2C().register(CommonPayload.ID, CommonPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(CommonPayload.ID, CommonPayload.CODEC);
		PayloadTypeRegistry.configurationS2C().register(CommonPayload.ID, CommonPayload.CODEC);
		PayloadTypeRegistry.configurationC2S().register(CommonPayload.ID, CommonPayload.CODEC);

		// When the client joins, send a packet expecting it to be echoed back
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(new CommonPayload("play")));
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> ServerConfigurationNetworking.send(handler, new CommonPayload("configuration")));

		// Store the player uuid once received from the client
		ServerPlayNetworking.registerGlobalReceiver(CommonPayload.ID, (payload, context) -> receivedPlay.add(context.player().getStringUUID()));
		ServerConfigurationNetworking.registerGlobalReceiver(CommonPayload.ID, (payload, context) -> receivedConfig.add(context.networkHandler().getOwner().getId().toString()));

		// Ensure that the packets were received on the server
		NeoForge.EVENT_BUS.addListener(EntityEvent.EntityConstructing.class, event -> {
			var entity = event.getEntity();
			var world = event.getEntity().level();

			if (!firstLoad) {
				// No need to check again if the player changes dimensions
				return;
			}

			firstLoad = false;

			if (entity instanceof ServerPlayer player) {
				final String uuid = player.getStringUUID();

				// Allow a few ticks for the packets to be received
				executeIn(Objects.requireNonNull(world.getServer()), 50, () -> {
					if (!receivedPlay.remove(uuid)) {
						throw new IllegalStateException("Did not receive play response");
					}

					if (!receivedConfig.remove(uuid)) {
						throw new IllegalStateException("Did not receive configuration response");
					}
				});
			}
		});
	}

	// A payload registered on both sides, for play and configuration
	// This tests that the server can send a packet to the client, and then receive a response from the client
	public record CommonPayload(String data) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<CommonPayload> ID = new Type<>(NetworkingTestmods.id("common_payload"));
		public static final StreamCodec<FriendlyByteBuf, CommonPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(CommonPayload::new, CommonPayload::data).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	private static void executeIn(MinecraftServer server, int ticks, Runnable runnable) {
		int targetTime = server.getTickCount() + ticks;
		server.execute(new Runnable() {
			@Override
			public void run() {
				if (server.getTickCount() >= targetTime) {
					runnable.run();
					return;
				}

				server.execute(this);
			}
		});
	}
}
