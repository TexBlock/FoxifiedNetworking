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

package net.fabricmc.fabric.test.networking.client.keybindreciever;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.test.networking.keybindreciever.KeybindPayload;
import net.minecraft.client.KeyMapping;

// Sends a packet to the server when a keybinding was pressed
// The server in response will send a chat message to the client.
public class NetworkingKeybindClientPacketTest {
	public static final KeyMapping TEST_BINDING = new KeyMapping("key.fabric-networking-api-v1-testmod.test", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, "key.category.fabric-networking-api-v1-testmod");

	public void onInitializeClient() {
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
			var client = Minecraft.getInstance();

			// Player must be in game to send packets, i.e. client.player != null
			if (client.getConnection() != null) {
				if (TEST_BINDING.consumeClick()) {
					// Send an empty payload, server just needs to be told when packet is sent
					// Since KeybindPayload is an empty payload, it can be a singleton.
					ClientPlayNetworking.send(KeybindPayload.INSTANCE);
				}
			}
		});
	}
}
