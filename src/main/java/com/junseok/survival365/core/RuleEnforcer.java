package com.junseok.survival365.core;

import com.junseok.survival365.scenario.WorldScenario;
import java.util.OptionalLong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

/** Common 365-day rules (SPEC §5): locked Hard difficulty and, if the scenario wants it, a frozen time of day. */
public final class RuleEnforcer {
	private static final int CHECK_INTERVAL = 20;

	private RuleEnforcer() {
	}

	public static void apply(MinecraftServer server, @Nullable WorldScenario scenario) {
		server.setDifficulty(Difficulty.HARD, true);
		server.setDifficultyLocked(true);
		enforceTime(server, scenario);
	}

	public static void tick(MinecraftServer server, ChallengeState state, @Nullable WorldScenario scenario) {
		if (state.elapsedTicks % CHECK_INTERVAL != 0) {
			return;
		}
		if (server.getWorldData().getDifficulty() != Difficulty.HARD || !server.getWorldData().isDifficultyLocked()) {
			server.setDifficulty(Difficulty.HARD, true);
			server.setDifficultyLocked(true);
		}
		enforceTime(server, scenario);
	}

	private static void enforceTime(MinecraftServer server, @Nullable WorldScenario scenario) {
		OptionalLong fixed = scenario == null ? OptionalLong.empty() : scenario.fixedDayTime();
		if (fixed.isEmpty()) {
			return;
		}
		GameRules rules = server.getGlobalGameRules();
		if (rules.get(GameRules.ADVANCE_TIME)) {
			rules.set(GameRules.ADVANCE_TIME, false, server);
		}
		DayClock.overworldClock(server).ifPresent(clock -> {
			long now = server.clockManager().getInstance(clock).totalTicks();
			long target = Math.floorDiv(now, DayClock.TICKS_PER_DAY) * DayClock.TICKS_PER_DAY + fixed.getAsLong();
			if (now != target) {
				server.clockManager().setTotalTicks(clock, target);
			}
		});
	}
}
