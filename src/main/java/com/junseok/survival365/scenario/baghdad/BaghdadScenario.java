package com.junseok.survival365.scenario.baghdad;

import com.junseok.survival365.Survival365;
import com.junseok.survival365.config.ModConfig;
import com.junseok.survival365.core.ChallengeManager;
import com.junseok.survival365.core.ChallengeState;
import com.junseok.survival365.core.Effects;
import com.junseok.survival365.scenario.WorldScenario;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.jspecify.annotations.Nullable;

/** "Baghdad Craft": endless desert, eternal night, periodic TNT bombardment and raids (SPEC §7). */
public final class BaghdadScenario implements WorldScenario {
	public static final Identifier ID = Survival365.id("baghdad");
	public static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, ID);
	private static final int BOSS_BAR_UPDATE_INTERVAL = 20;

	private final BaghdadCycle cycle = new BaghdadCycle();
	private @Nullable ServerBossEvent bossBar;

	private static BaghdadConfig config() {
		return ModConfig.get().baghdad;
	}

	@Override
	public Identifier id() {
		return ID;
	}

	@Override
	public ResourceKey<WorldPreset> worldPreset() {
		return PRESET;
	}

	@Override
	public OptionalLong fixedDayTime() {
		return OptionalLong.of(config().fixedDayTime);
	}

	@Override
	public void onChallengeStart(MinecraftServer server, ChallengeState state) {
		cycle.start(BaghdadPhase.CALM, config());
		updateBossBar();
	}

	@Override
	public void tick(MinecraftServer server, ChallengeState state) {
		BaghdadConfig config = config();
		int into = cycle.ticksIntoPhase();
		switch (cycle.phase()) {
			case BOMBARDMENT -> {
				if (into % (config.tntWaveIntervalSeconds * 20) == 0) {
					TntBombardment.wave(server.overworld(), targets(server), config);
				}
			}
			case RAID -> {
				if (into % config.raidSpawnIntervalTicks == 0) {
					RaidSpawner.spawnRound(server.overworld(), targets(server), config);
				}
			}
			default -> {
			}
		}
		if (cycle.countDown()) {
			enterPhase(server, cycle.phase().next());
		} else if (cycle.ticksLeft() % BOSS_BAR_UPDATE_INTERVAL == 0) {
			updateBossBar();
		}
	}

	/** Starts {@code phase} from its beginning, with the transition sound/title for everyone. */
	public void enterPhase(MinecraftServer server, BaghdadPhase phase) {
		cycle.start(phase, config());
		updateBossBar();
		String sound = switch (phase) {
			case BOMBARDMENT -> config().bombardmentStartSound;
			case RAID -> config().raidStartSound;
			default -> null;
		};
		if (sound == null) {
			return;
		}
		Component title = Component.translatable("title.survival365.baghdad." + phase.key());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Effects.title(player, title);
			Effects.sound(player, sound, 1.0F, 1.0F);
		}
	}

	public void skip(int seconds) {
		cycle.skip(seconds * 20);
		updateBossBar();
	}

	/** Survival/adventure players who are alive and in the overworld. */
	private static List<ServerPlayer> targets(MinecraftServer server) {
		List<ServerPlayer> result = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() == Level.OVERWORLD && player.isAlive() && !player.isSpectator() && !player.isCreative()) {
				result.add(player);
			}
		}
		return result;
	}

	private ServerBossEvent bossBar() {
		if (bossBar == null) {
			bossBar = new ServerBossEvent(UUID.randomUUID(), Component.empty(), cycle.phase().color, BossEvent.BossBarOverlay.PROGRESS);
		}
		return bossBar;
	}

	private void updateBossBar() {
		ServerBossEvent bar = bossBar();
		int seconds = (cycle.ticksLeft() + 19) / 20;
		String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
		bar.setName(Component.translatable(cycle.phase().bossBarKey(), time));
		bar.setColor(cycle.phase().color);
		bar.setProgress(cycle.progress());
	}

	@Override
	public void onPlayerJoin(ServerPlayer player, ChallengeState state) {
		bossBar().addPlayer(player);
	}

	@Override
	public void onPlayerLeave(ServerPlayer player, ChallengeState state) {
		bossBar().removePlayer(player);
	}

	@Override
	public void onServerStopped(MinecraftServer server) {
		if (bossBar != null) {
			bossBar.removeAllPlayers();
		}
	}

	@Override
	public void appendStatus(Consumer<Component> out) {
		int seconds = (cycle.ticksLeft() + 19) / 20;
		out.accept(Component.translatable("commands.survival365.status.phase",
			Component.translatable("phase.survival365.baghdad." + cycle.phase().key()),
			String.format("%02d:%02d", seconds / 60, seconds % 60)));
	}

	@Override
	public void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
		LiteralArgumentBuilder<CommandSourceStack> phase = Commands.literal("phase");
		for (BaghdadPhase p : BaghdadPhase.values()) {
			phase.then(Commands.literal(p.key()).executes(ctx -> withActive(ctx, s -> {
				s.enterPhase(ctx.getSource().getServer(), p);
				ctx.getSource().sendSuccess(() -> Component.translatable("commands.survival365.phase.success",
					Component.translatable("phase.survival365.baghdad." + p.key())), true);
			})));
		}
		root.then(phase);
		root.then(Commands.literal("skip").then(Commands.argument("seconds", IntegerArgumentType.integer(1)).executes(ctx -> withActive(ctx, s -> {
			int seconds = IntegerArgumentType.getInteger(ctx, "seconds");
			s.skip(seconds);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.survival365.skip.success", seconds), true);
		}))));
	}

	private static int withActive(CommandContext<CommandSourceStack> ctx, Consumer<BaghdadScenario> action) {
		if (ChallengeManager.activeScenario() instanceof BaghdadScenario scenario) {
			action.accept(scenario);
			return 1;
		}
		ctx.getSource().sendFailure(Component.translatable("commands.survival365.not_baghdad"));
		return 0;
	}

	@Override
	public CompoundTag save() {
		return cycle.save();
	}

	@Override
	public void load(CompoundTag tag) {
		cycle.load(tag, config());
		updateBossBar();
	}
}
