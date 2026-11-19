package com.junseok.survival365.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.junseok.survival365.Survival365;
import com.junseok.survival365.config.ModConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Asks GitHub once per game launch whether a newer release exists, and tells the player in chat
 * (with a download link) the first time they enter a world. Never downloads or replaces anything.
 */
public final class UpdateChecker {
	private static final String LATEST_RELEASE_API = "https://api.github.com/repos/oldwater2007/survival365/releases/latest";
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private record Release(String version, String url) {
	}

	private static volatile @Nullable Release available;
	private static boolean announced;

	private UpdateChecker() {
	}

	public static void start() {
		if (!ModConfig.get().updates.checkForUpdates) {
			return;
		}
		String current = currentVersion();
		HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_RELEASE_API))
			.timeout(TIMEOUT)
			.header("Accept", "application/vnd.github+json")
			.header("User-Agent", "survival365-update-checker")
			.GET()
			.build();
		HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
			.sendAsync(request, HttpResponse.BodyHandlers.ofString())
			.thenAccept(response -> {
				if (response.statusCode() != 200) {
					Survival365.LOGGER.info("Update check skipped (HTTP {})", response.statusCode());
					return;
				}
				JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
				String latest = json.get("tag_name").getAsString();
				if (isNewer(latest, current)) {
					available = new Release(stripPrefix(latest), json.get("html_url").getAsString());
					Survival365.LOGGER.info("Survival 365 {} is available (running {})", stripPrefix(latest), current);
				}
			})
			.exceptionally(error -> {
				Survival365.LOGGER.info("Update check failed: {}", error.toString());
				return null;
			});
	}

	/** Called when the player joins a world; shows the notice at most once per launch. */
	public static void announce(Minecraft client) {
		Release release = available;
		if (release == null || announced) {
			return;
		}
		announced = true;
		Component link = Component.translatable("update.survival365.download")
			.withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(release.url()))).withUnderlined(true))
			.withStyle(ChatFormatting.AQUA);
		client.gui.hud.getChat().addClientSystemMessage(
			Component.translatable("update.survival365.available", release.version(), currentVersion()).append(" ").append(link)
		);
	}

	private static String currentVersion() {
		return FabricLoader.getInstance().getModContainer(Survival365.MOD_ID)
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
			.orElse("0");
	}

	private static String stripPrefix(String version) {
		return version.startsWith("v") || version.startsWith("V") ? version.substring(1) : version;
	}

	/** Numeric dotted-version comparison: "v1.2.0" is newer than "1.1.9"; anything after '-' or '+' is ignored. */
	public static boolean isNewer(String latest, String current) {
		String[] a = stripPrefix(latest).split("[-+]")[0].split("\\.");
		String[] b = stripPrefix(current).split("[-+]")[0].split("\\.");
		for (int i = 0; i < Math.max(a.length, b.length); i++) {
			int x = i < a.length ? parse(a[i]) : 0;
			int y = i < b.length ? parse(b[i]) : 0;
			if (x != y) {
				return x > y;
			}
		}
		return false;
	}

	private static int parse(String part) {
		try {
			return Integer.parseInt(part.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
