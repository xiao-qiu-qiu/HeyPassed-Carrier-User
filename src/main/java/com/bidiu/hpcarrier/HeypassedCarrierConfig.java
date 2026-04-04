package com.bidiu.hpcarrier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class HeypassedCarrierConfig {
	public static final List<String> GAME_MODES = List.of(
		"sw1",
		"sw2",
		"swwzy",
		"xyzz1",
		"xyzz2",
		"bw8-1",
		"bw8-2",
		"bw4-4",
		"bwxp4-8",
		"bwxp8-4",
		"bwxp32-32",
		"bwwuhuo",
		"zyzz"
	);

	public static final Map<String, String> GAME_MODE_NAMES = Map.ofEntries(
		Map.entry("sw1", "空岛单人"),
		Map.entry("sw2", "空岛双人"),
		Map.entry("swwzy", "空岛单人 无职业"),
		Map.entry("xyzz1", "幸运之柱 单人"),
		Map.entry("xyzz2", "幸运之柱 双人"),
		Map.entry("bw8-1", "起床8队单人"),
		Map.entry("bw8-2", "起床8队双人"),
		Map.entry("bw4-4", "起床4队4人"),
		Map.entry("bwxp4-8", "经验起床 4队8人"),
		Map.entry("bwxp8-4", "经验起床 8队4人"),
		Map.entry("bwxp32-32", "经验起床 64人（32v32）"),
		Map.entry("bwwuhuo", "起床 无限火力"),
		Map.entry("zyzz", "职业战争")
	);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("heypassed-carrier.json");

	public boolean autoJoinParty = true;
	public boolean autoJoinPartyRecentInviteWindow = true;
	public String selectedMode = GAME_MODES.getFirst();
	public String messageTemplate = ".irc chat $tell Bi_Diu .i [id] [mode]";

	public static HeypassedCarrierConfig load() {
		if (!Files.exists(CONFIG_PATH)) {
			HeypassedCarrierConfig config = new HeypassedCarrierConfig();
			config.save();
			return config;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			HeypassedCarrierConfig config = GSON.fromJson(reader, HeypassedCarrierConfig.class);
			if (config == null) {
				config = new HeypassedCarrierConfig();
			}
			config.normalize();
			return config;
		} catch (IOException | JsonSyntaxException exception) {
			HeypassedCarrier.LOGGER.warn("Failed to load config, using defaults", exception);
			HeypassedCarrierConfig config = new HeypassedCarrierConfig();
			config.save();
			return config;
		}
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			HeypassedCarrier.LOGGER.error("Failed to save config", exception);
		}
	}

	public String buildInviteMessage(String playerId) {
		return messageTemplate
			.replace("[id]", playerId)
			.replace("[mode]", getNormalizedSelectedMode());
	}

	public void setSelectedMode(String selectedMode) {
		this.selectedMode = selectedMode;
		normalize();
	}

	public void normalize() {
		if (selectedMode != null) {
			selectedMode = selectedMode.trim();
		}
		if (!GAME_MODES.contains(selectedMode)) {
			selectedMode = GAME_MODES.getFirst();
		}
		if (messageTemplate == null || messageTemplate.isBlank()) {
			messageTemplate = ".irc chat $tell Bi_Diu .i [id] [mode]";
		}
	}

	private String getNormalizedSelectedMode() {
		normalize();
		return selectedMode;
	}

	public static String getModeDisplayName(String modeId) {
		return GAME_MODE_NAMES.getOrDefault(modeId, modeId);
	}
}