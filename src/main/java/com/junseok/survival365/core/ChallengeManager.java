package com.junseok.survival365.core;

import com.junseok.survival365.Survival365;
import com.junseok.survival365.config.ModConfig;
import com.junseok.survival365.network.CountdownSyncPayload;
import com.junseok.survival365.scenario.Scenarios;
import com.junseok.survival365.scenario.WorldScenario;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * Wires the challenge into the server lifecycle. Holds the state of the one running server
 * (integrated or dedicated); everything here runs on the server thread.
 */
public final class ChallengeManager {
	private static @Nullable ChallengeState state;
	private static @Nullable WorldScenario scenario;

	private ChallengeManager() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(ChallengeManager::onServerStarted);
		ServerLifecycleEvents.SERVER_STOPPED.register(ChallengeManager::onServerStopped);
		ServerTickEvents.END_SERVER_TICK.register(ChallengeManager::onServerTick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onPlayerLeave(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> sync(player));
	}

	public static @Nullable ChallengeState state() {
		return state;
	}

	public static @Nullable WorldScenario activeScenario() {
		return state != null && state.enabled ? scenario : null;
	}

	public static boolean isActive() {
		return state != null && state.enabled;
	}

	private static void onServerStarted(MinecraftServer server) {
		ModConfig.load();
		ChallengeOptions pending = PendingChallengeOptions.take();
		ChallengeState existing = server.getDataStorage().get(ChallengeState.TYPE);
		if (existing == null) {
			ChallengeOptions options = pending;
			if (options == null && server.isDedicatedServer() && isFreshWorld(server)) {
				ModConfig.loadServer();
				ModConfig.ServerConfig cfg = ModConfig.server();
				Identifier id = Identifier.tryParse(cfg.scenario);
				options = new ChallengeOptions(cfg.enableOnNewWorld, id != null ? id : Scenarios.defaultId());
			}
			existing = server.getDataStorage().computeIfAbsent(ChallengeState.TYPE);
			if (options != null && options.enabled()) {
				startNew(server, existing, options);
			}
		} else if (existing.enabled) {
			scenario = Scenarios.create(Identifier.tryParse(existing.scenarioId));
			if (scenario == null) {
				Survival365.LOGGER.warn("Unknown scenario '{}' in this world; the countdown keeps running without it", existing.scenarioId);
			} else {
				scenario.load(existing.scenarioData());
			}
		}
		state = existing;
		state.attachScenario(scenario);
		if (state.enabled) {
			RuleEnforcer.apply(server, scenario);
		}
	}

	/** A dedicated server has generated a new world when no tick has run yet. */
	private static boolean isFreshWorld(MinecraftServer server) {
		return server.overworld().getGameTime() == 0L;
	}

	private static void startNew(MinecraftServer server, ChallengeState newState, ChallengeOptions options) {
		newState.enabled = true;
		newState.scenarioId = options.scenarioId().toString();
		newState.elapsedTicks = 0L;
		newState.completed = false;
		scenario = Scenarios.create(options.scenarioId());
		if (scenario == null) {
			Survival365.LOGGER.warn("Unknown scenario '{}' selected; starting the countdown without it", options.scenarioId());
		}
		RuleEnforcer.apply(server, scenario);
		newState.startDayTime = DayClock.overworldTime(server);
		newState.lastAnnouncedDay = ModConfig.get().countdown.totalDays;
		if (scenario != null) {
			scenario.onChallengeStart(server, newState);
		}
		newState.setDirty();
	}

	private static void onServerStopped(MinecraftServer server) {
		if (scenario != null) {
			scenario.onServerStopped(server);
		}
		state = null;
		scenario = null;
		PendingChallengeOptions.clear();
	}

	private static void onServerTick(MinecraftServer server) {
		ChallengeState s = state;
		if (s == null || !s.enabled) {
			return;
		}
		s.elapsedTicks++;
		s.setDirty();
		RuleEnforcer.tick(server, s, scenario);
		if (scenario != null) {
			scenario.tick(server, s);
		}
		checkDayChange(server, s);
	}

	private static void checkDayChange(MinecraftServer server, ChallengeState s) {
		int daysLeft = DayClock.daysLeft(server, s, scenario);
		if (daysLeft == s.lastAnnouncedDay) {
			return;
		}
		boolean passed = daysLeft < s.lastAnnouncedDay;
		s.lastAnnouncedDay = daysLeft;
		syncAll(server);
		if (passed && ModConfig.get().countdown.announceDayChange) {
			Component msg = Component.translatable("hud.survival365.day_passed", DayClock.format(daysLeft));
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Effects.actionBar(player, msg);
				Effects.sound(player, ModConfig.get().countdown.dayChangeSound, 1.0F, 1.0F);
			}
		}
		if (daysLeft == 0 && !s.completed) {
			s.completed = true;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				Effects.title(player, Component.translatable("title.survival365.survived"));
				Effects.sound(player, ModConfig.get().countdown.completeSound, 1.0F, 1.0F);
			}
		}
	}

	private static void onPlayerJoin(ServerPlayer player) {
		sync(player);
		if (state != null && state.enabled && scenario != null) {
			scenario.onPlayerJoin(player, state);
		}
	}

	private static void onPlayerLeave(ServerPlayer player) {
		if (state != null && state.enabled && scenario != null) {
			scenario.onPlayerLeave(player, state);
		}
	}

	public static void syncAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			sync(player);
		}
	}

	public static void sync(ServerPlayer player) {
		ChallengeState s = state;
		if (s == null || !ServerPlayNetworking.canSend(player, CountdownSyncPayload.TYPE)) {
			return;
		}
		int daysLeft = s.enabled ? DayClock.daysLeft(player.level().getServer(), s, scenario) : 0;
		ServerPlayNetworking.send(player, new CountdownSyncPayload(s.enabled, daysLeft, ModConfig.get().countdown.warningDays, s.scenarioId));
	}
}
