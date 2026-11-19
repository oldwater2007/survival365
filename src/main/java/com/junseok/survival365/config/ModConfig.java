package com.junseok.survival365.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.junseok.survival365.Survival365;
import com.junseok.survival365.scenario.baghdad.BaghdadConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * config/survival365.json. Scenario sections are named after the scenario id path ("baghdad").
 * Loaded at server start and by /s365 reload; invalid values fall back to defaults with a warning.
 */
public final class ModConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FILE_NAME = "survival365.json";
	private static final String SERVER_FILE_NAME = "survival365-server.json";

	private static ModConfig instance = new ModConfig();
	private static ServerConfig serverInstance = new ServerConfig();

	public Countdown countdown = new Countdown();
	public BaghdadConfig baghdad = new BaghdadConfig();

	public static final class Countdown {
		public int totalDays = 365;
		public int warningDays = 30;
		public boolean announceDayChange = true;
		public String dayChangeSound = "minecraft:block.note_block.pling";
		public String completeSound = "minecraft:ui.toast.challenge_complete";

		void validate() {
			Countdown d = new Countdown();
			if (totalDays < 1) totalDays = warn("countdown.totalDays", d.totalDays);
			if (warningDays < 0) warningDays = warn("countdown.warningDays", d.warningDays);
			if (dayChangeSound == null) dayChangeSound = d.dayChangeSound;
			if (completeSound == null) completeSound = d.completeSound;
		}
	}

	/** Dedicated-server only: decides whether a freshly generated world starts the challenge (SPEC §11). */
	public static final class ServerConfig {
		public boolean enableOnNewWorld = true;
		public String scenario = "survival365:baghdad";
	}

	public static ModConfig get() {
		return instance;
	}

	public static ServerConfig server() {
		return serverInstance;
	}

	public static void load() {
		ModConfig loaded = read(FILE_NAME, ModConfig.class, new ModConfig());
		if (loaded.countdown == null) loaded.countdown = new Countdown();
		if (loaded.baghdad == null) loaded.baghdad = new BaghdadConfig();
		loaded.countdown.validate();
		loaded.baghdad.validate();
		instance = loaded;
		write(FILE_NAME, instance);
	}

	public static void loadServer() {
		serverInstance = read(SERVER_FILE_NAME, ServerConfig.class, new ServerConfig());
		write(SERVER_FILE_NAME, serverInstance);
	}

	private static <T> T read(String name, Class<T> type, T fallback) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(name);
		if (!Files.exists(path)) {
			return fallback;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			T value = GSON.fromJson(reader, type);
			return value != null ? value : fallback;
		} catch (Exception e) {
			Survival365.LOGGER.warn("Failed to read config {}, using defaults", path, e);
			return fallback;
		}
	}

	private static void write(String name, Object value) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(name);
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(value, writer);
			}
		} catch (IOException e) {
			Survival365.LOGGER.warn("Failed to write config {}", path, e);
		}
	}

	public static <T> T warn(String key, T fallback) {
		Survival365.LOGGER.warn("Invalid config value for {}, using default {}", key, fallback);
		return fallback;
	}
}
