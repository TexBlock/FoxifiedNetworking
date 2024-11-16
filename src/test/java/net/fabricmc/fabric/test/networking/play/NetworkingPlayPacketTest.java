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

package net.fabricmc.fabric.test.networking.play;

import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.test.networking.NetworkingTestmods;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class NetworkingPlayPacketTest {
	private static boolean spamUnknownPackets = false;

	public static void sendToTestChannel(ServerPlayer player, String stuff) {
		ServerPlayNetworking.getSender(player).sendPacket(new OverlayPacket(Component.literal(stuff)), PacketSendListener.thenRun(() -> {
			NetworkingTestmods.LOGGER.info("Sent custom payload packet");
		}));
	}

	private static void sendToUnknownChannel(ServerPlayer player) {
		ServerPlayNetworking.getSender(player).sendPacket(new UnknownPayload("Hello"));
	}

	public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
		NetworkingTestmods.LOGGER.info("Registering test command");

		dispatcher.register(literal("networktestcommand")
				.then(argument("stuff", string()).executes(ctx -> {
					String stuff = StringArgumentType.getString(ctx, "stuff");
					sendToTestChannel(ctx.getSource().getPlayer(), stuff);
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("unknown").executes(ctx -> {
					sendToUnknownChannel(ctx.getSource().getPlayer());
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("spamUnknown").executes(ctx -> {
					spamUnknownPackets = true;
					ctx.getSource().sendSystemMessage(Component.literal("Spamming unknown packets state:" + spamUnknownPackets));
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("simple").executes(ctx -> {
					ServerPlayNetworking.send(ctx.getSource().getPlayer(), new OverlayPacket(Component.literal("simple")));
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("bundled").executes(ctx -> {
					ClientboundBundlePacket packet = new ClientboundBundlePacket(List.of(
							ServerPlayNetworking.createS2CPacket(new OverlayPacket(Component.literal("bundled #1"))),
							ServerPlayNetworking.createS2CPacket(new OverlayPacket(Component.literal("bundled #2")))
					));
					ServerPlayNetworking.getSender(ctx.getSource().getPlayer()).sendPacket(packet);
					return Command.SINGLE_SUCCESS;
				})));
	}

	public void onInitialize() {
		NetworkingTestmods.LOGGER.info("Hello from networking user!");

		PayloadTypeRegistry.playS2C().register(OverlayPacket.ID, OverlayPacket.CODEC);

		if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
			PayloadTypeRegistry.playS2C().register(UnknownPayload.ID, UnknownPayload.CODEC);
		}

		NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
			NetworkingPlayPacketTest.registerCommand(event.getDispatcher());
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sender.sendPacket(new OverlayPacket(Component.literal("Fabric API"))));

		NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class, event -> {
			if (!spamUnknownPackets) {
				return;
			}

			// Send many unknown packets, used to debug https://github.com/FabricMC/fabric/issues/3505
			for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
				for (int i = 0; i < 50; i++) {
					sendToUnknownChannel(player);
				}
			}
		});
	}

	public record OverlayPacket(Component message) implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<OverlayPacket> ID = new Type<>(NetworkingTestmods.id("test_channel"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OverlayPacket> CODEC = CustomPacketPayload.codec(OverlayPacket::write, OverlayPacket::new);

		public OverlayPacket(RegistryFriendlyByteBuf buf) {
			this(ComponentSerialization.STREAM_CODEC.decode(buf));
		}

		public void write(RegistryFriendlyByteBuf buf) {
			ComponentSerialization.STREAM_CODEC.encode(buf, this.message);
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	private record UnknownPayload(String data) implements CustomPacketPayload {
		private static final CustomPacketPayload.Type<UnknownPayload> ID = new Type<>(NetworkingTestmods.id("unknown_test_channel_s2c"));
		private static final StreamCodec<FriendlyByteBuf, UnknownPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(UnknownPayload::new, UnknownPayload::data).cast();

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}
