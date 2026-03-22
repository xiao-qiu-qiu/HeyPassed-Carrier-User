package com.bidiu.hpcarrier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class HeypassedCarrierClient implements ClientModInitializer {
	public static final String KEY_CATEGORY = "key.categories.heypassed-carrier";
	private static final String INVITE_HINT = "邀请您加入他的队伍";

	private static HeypassedCarrierConfig config;
	private static KeyBinding sendInviteKey;
	private static KeyBinding openScreenKey;

	@Override
	public void onInitializeClient() {
		config = HeypassedCarrierConfig.load();

		sendInviteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.heypassed-carrier.send_invite",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_BACKSLASH,
			KEY_CATEGORY
		));
		openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.heypassed-carrier.open_settings",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_BRACKET,
			KEY_CATEGORY
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::handleKeyPresses);
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> tryAutoJoin(message));
	}

	private void handleKeyPresses(MinecraftClient client) {
		while (sendInviteKey.wasPressed()) {
			String playerId = resolvePlayerId(client);
			if (playerId != null) {
				client.getNetworkHandler().sendChatMessage(config.buildInviteMessage(playerId));
			}
		}

		while (openScreenKey.wasPressed()) {
			client.setScreen(new HeypassedCarrierScreen(client.currentScreen));
		}
	}

	private static void tryAutoJoin(Text message) {
		if (!config.autoJoinParty || !message.getString().contains(INVITE_HINT)) {
			return;
		}

		String command = findRunCommand(message);
		if (command == null || command.isBlank()) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.getNetworkHandler() == null) {
			return;
		}

		if (command.startsWith("/")) {
			client.getNetworkHandler().sendChatCommand(command.substring(1));
		} else {
			client.getNetworkHandler().sendChatCommand(command);
		}
	}

	private static @Nullable String resolvePlayerId(MinecraftClient client) {
		if (client.player == null || client.getNetworkHandler() == null) {
			return null;
		}

		return client.player.getGameProfile().getName();
	}

	private static @Nullable String findRunCommand(Text text) {
		Style style = text.getStyle();
		ClickEvent clickEvent = style.getClickEvent();
		if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
			return clickEvent.getValue();
		}

		for (Text sibling : text.getSiblings()) {
			String command = findRunCommand(sibling);
			if (command != null) {
				return command;
			}
		}

		return null;
	}

	public static HeypassedCarrierConfig getConfig() {
		if (config == null) {
			config = HeypassedCarrierConfig.load();
		}
		return config;
	}

	public static void saveConfig() {
		getConfig().save();
	}
}