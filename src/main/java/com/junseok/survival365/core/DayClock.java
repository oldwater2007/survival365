package com.junseok.survival365.core;

import com.junseok.survival365.config.ModConfig;
import com.junseok.survival365.scenario.WorldScenario;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.WorldClock;
import org.jspecify.annotations.Nullable;

/** D-day math (SPEC §3.3). Server side only. */
public final class DayClock {
	public static final long TICKS_PER_DAY = 24000L;

	private DayClock() {
	}

	/** Fixed-time scenarios count real server ticks; others follow the overworld clock (sleeping skips a day). */
	private static boolean usesElapsedTicks(@Nullable WorldScenario scenario) {
		return scenario != null && scenario.fixedDayTime().isPresent();
	}

	public static long elapsedDays(MinecraftServer server, ChallengeState state, @Nullable WorldScenario scenario) {
		long ticks = usesElapsedTicks(scenario) ? state.elapsedTicks : overworldTime(server) - state.startDayTime;
		return Math.max(0L, ticks / TICKS_PER_DAY);
	}

	public static int daysLeft(MinecraftServer server, ChallengeState state, @Nullable WorldScenario scenario) {
		long left = ModConfig.get().countdown.totalDays - elapsedDays(server, state, scenario);
		return (int) Math.max(0L, left);
	}

	/** /s365 day set: moves the counter so that exactly {@code daysLeft} remain at the start of that day. */
	public static void setDaysLeft(MinecraftServer server, ChallengeState state, @Nullable WorldScenario scenario, int daysLeft) {
		long elapsed = (long) (ModConfig.get().countdown.totalDays - daysLeft) * TICKS_PER_DAY;
		if (usesElapsedTicks(scenario)) {
			state.elapsedTicks = elapsed;
		} else {
			state.startDayTime = overworldTime(server) - elapsed;
		}
		state.completed = daysLeft <= 0 && state.completed;
		state.setDirty();
	}

	public static Optional<Holder<WorldClock>> overworldClock(MinecraftServer server) {
		return server.overworld().dimensionType().defaultClock();
	}

	public static long overworldTime(MinecraftServer server) {
		return overworldClock(server).map(c -> server.clockManager().getInstance(c).totalTicks()).orElse(0L);
	}

	public static String format(int daysLeft) {
		return daysLeft > 0 ? "D-" + daysLeft : "D-DAY";
	}
}
