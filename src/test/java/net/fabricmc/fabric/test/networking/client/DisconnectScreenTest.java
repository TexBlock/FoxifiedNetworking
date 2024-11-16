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

package net.fabricmc.fabric.test.networking.client;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class DisconnectScreenTest {
	public void onInitializeClient() {
		NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
			var dispatcher = event.getDispatcher();
			dispatcher.register(Commands.literal("disconnect_screen_test").executes(context -> {
					StringBuilder builder = new StringBuilder("A very long disconnect reason:");

					for (int i = 0; i < 100; i++) {
						builder.append("\nLine ").append(i + 1);
					}

					context.getSource().getPlayer().connection.getConnection().disconnect(Component.nullToEmpty(builder.toString()));
					return 1;
				})
			);
		});
	}
}
