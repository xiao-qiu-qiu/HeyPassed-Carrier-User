package com.bidiu.hpcarrier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

public class HeypassedCarrierClient implements ClientModInitializer {
	public static final String KEY_CATEGORY = "key.categories.heypassed-carrier";
	private static final String HUB_TAB_HEADER = "§d§o§d§l布吉岛\n§r§7欢迎你的到来~";
	private static final String INVITE_HINT = "邀请您加入他的队伍";
	private static final long AUTO_JOIN_WINDOW_MILLIS = 10_000L;
	private static final long SEND_CONFIRM_WINDOW_MILLIS = 500L;
	private static final Text AUTO_JOIN_TIMEOUT_TEXT = buildAutoJoinTimeoutText();
	private static final Text SEND_CONFIRM_TEXT = buildSendConfirmText();

	private static HeypassedCarrierConfig config;
	private static KeyBinding sendInviteKey;
	private static KeyBinding openScreenKey;
	private static long lastInviteShortcutAtMillis;
	private static long firstSendConfirmAtMillis;
	private static boolean autoJoinWindowActive;
	private static boolean waitingForSecondSendPress;

	@Override
	public void onInitializeClient() {
		config = HeypassedCarrierConfig.load();

		sendInviteKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.heypassed-carrier.send_invite",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_BACKSLASH,
				KEY_CATEGORY));
		openScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.heypassed-carrier.open_settings",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_BRACKET,
				KEY_CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(this::handleKeyPresses);
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> tryAutoJoin(message));
	}

	private void handleKeyPresses(MinecraftClient client) {
		maybeExpireAutoJoinWindow(client);

		while (sendInviteKey.wasPressed()) {
			String playerId = resolvePlayerId(client);
			if (playerId != null && shouldSendInviteNow(client)) {
				sendInviteMessage(client, playerId);
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

		if (config.autoJoinPartyRecentInviteWindow) {
			if (!autoJoinWindowActive) {
				return;
			}

			long elapsedMillis = System.currentTimeMillis() - lastInviteShortcutAtMillis;
			if (elapsedMillis < 0 || elapsedMillis > AUTO_JOIN_WINDOW_MILLIS) {
				autoJoinWindowActive = false;
				return;
			}
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

		if (config.autoJoinPartyRecentInviteWindow) {
			autoJoinWindowActive = false;
		}
	}

	private static void maybeExpireAutoJoinWindow(MinecraftClient client) {
		if (!config.autoJoinPartyRecentInviteWindow || !autoJoinWindowActive) {
			return;
		}

		long elapsedMillis = System.currentTimeMillis() - lastInviteShortcutAtMillis;
		if (elapsedMillis < 0 || elapsedMillis <= AUTO_JOIN_WINDOW_MILLIS) {
			return;
		}

		autoJoinWindowActive = false;
		if (client.player != null) {
			client.player.sendMessage(AUTO_JOIN_TIMEOUT_TEXT, false);
		}
	}

	private static Text buildAutoJoinTimeoutText() {
		MutableText prefix = Text.literal("[运兵车] ")
				.formatted(Formatting.AQUA, Formatting.BOLD);
		MutableText body = Text.literal("10s内未接到组队邀请，已停止接受组队邀请")
				.formatted(Formatting.YELLOW);
		return prefix.append(body);
	}

	private static Text buildSendConfirmText() {
		MutableText prefix = Text.literal("[运兵车] ")
				.formatted(Formatting.AQUA, Formatting.BOLD);
		MutableText body = Text.literal("检测到当前不在主城，需在0.5s内双击发送键发送指令")
				.formatted(Formatting.GOLD);
		return prefix.append(body);
	}

	private static boolean shouldSendInviteNow(MinecraftClient client) {
		if (!config.inGameSendConfirm || isInHub(client)) {
			waitingForSecondSendPress = false;
			firstSendConfirmAtMillis = 0L;
			return true;
		}

		long now = System.currentTimeMillis();
		if (waitingForSecondSendPress && now - firstSendConfirmAtMillis > SEND_CONFIRM_WINDOW_MILLIS) {
			waitingForSecondSendPress = false;
			firstSendConfirmAtMillis = 0L;
		}

		if (waitingForSecondSendPress) {
			waitingForSecondSendPress = false;
			firstSendConfirmAtMillis = 0L;
			return true;
		}

		waitingForSecondSendPress = true;
		firstSendConfirmAtMillis = now;
		if (client.player != null) {
			client.player.sendMessage(SEND_CONFIRM_TEXT, false);
		}
		return false;
	}

	private static void sendInviteMessage(MinecraftClient client, String playerId) {
		client.getNetworkHandler().sendChatMessage(config.buildInviteMessage(playerId));
		lastInviteShortcutAtMillis = System.currentTimeMillis();
		autoJoinWindowActive = true;
	}

	private static boolean isInHub(MinecraftClient client) {
		if (client.getNetworkHandler() == null || client.player == null) {
			return false;
		}

		Text tabListHeader = getTabListHeader(client.inGameHud.getPlayerListHud());
		if (tabListHeader == null) {
			return false;
		}

		return HUB_TAB_HEADER.equals(tabListHeader.getString());
	}

	private static @Nullable Text getTabListHeader(PlayerListHud playerListHud) {
		try {
			Field headerField = PlayerListHud.class.getDeclaredField("header");
			headerField.setAccessible(true);
			Object value = headerField.get(playerListHud);
			if (value instanceof Text text) {
				return text;
			}
		} catch (ReflectiveOperationException ignored) {
		}

		return null;
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