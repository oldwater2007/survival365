package com.junseok.survival365.command;

import com.junseok.survival365.config.ModConfig;
import com.junseok.survival365.core.ChallengeManager;
import com.junseok.survival365.core.ChallengeState;
import com.junseok.survival365.core.DayClock;
import com.junseok.survival365.scenario.Scenarios;
import com.junseok.survival365.scenario.WorldScenario;
import com.junseok.survival365.scenario.baghdad.RaidSpawner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

/** /s365 debug and admin commands (SPEC §10). Requires gamemaster permission (level 2). */
public final class Survival365Command {
	private Survival365Command() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("s365").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		root.then(Commands.literal("status").executes(Survival365Command::status));
		root.then(Commands.literal("day").then(Commands.literal("set")
			.then(Commands.argument("daysLeft", IntegerArgumentType.integer(0)).executes(Survival365Command::setDay))));
		root.then(Commands.literal("cleanup").executes(Survival365Command::cleanup));
		root.then(Commands.literal("reload").executes(Survival365Command::reload));
		for (var id : Scenarios.ids()) {
			WorldScenario scenario = Scenarios.create(id);
			if (scenario != null) {
				scenario.registerCommands(root);
			}
		}
		dispatcher.register(root);
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack source = ctx.getSource();
		ChallengeState state = ChallengeManager.state();
		if (state == null || !state.enabled) {
			source.sendSuccess(() -> Component.translatable("commands.survival365.status.disabled"), false);
			return 0;
		}
		WorldScenario scenario = ChallengeManager.activeScenario();
		int daysLeft = DayClock.daysLeft(source.getServer(), state, scenario);
		Component scenarioName = scenario != null ? scenario.displayName() : Component.literal(state.scenarioId);
		source.sendSuccess(() -> Component.translatable("commands.survival365.status.enabled", scenarioName, DayClock.format(daysLeft)), false);
		if (scenario != null) {
			scenario.appendStatus(line -> source.sendSuccess(() -> line, false));
		}
		return 1;
	}

	private static int setDay(CommandContext<CommandSourceStack> ctx) {
		ChallengeState state = requireActive(ctx);
		if (state == null) {
			return 0;
		}
		int daysLeft = Math.min(IntegerArgumentType.getInteger(ctx, "daysLeft"), ModConfig.get().countdown.totalDays);
		MinecraftServer server = ctx.getSource().getServer();
		DayClock.setDaysLeft(server, state, ChallengeManager.activeScenario(), daysLeft);
		ChallengeManager.syncAll(server);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.survival365.day.success", DayClock.format(daysLeft)), true);
		return 1;
	}

	private static int cleanup(CommandContext<CommandSourceStack> ctx) {
		int removed = 0;
		for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
			List<? extends Entity> raiders = level.getEntities(EntityTypeTest.forClass(Entity.class), e -> e.entityTags().contains(RaidSpawner.RAIDER_TAG));
			for (Entity entity : raiders) {
				entity.discard();
				removed++;
			}
		}
		int count = removed;
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.survival365.cleanup.success", count), true);
		return count;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		ModConfig.load();
		ChallengeManager.syncAll(ctx.getSource().getServer());
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.survival365.reload.success"), true);
		return 1;
	}

	private static ChallengeState requireActive(CommandContext<CommandSourceStack> ctx) {
		ChallengeState state = ChallengeManager.state();
		if (state == null || !state.enabled) {
			ctx.getSource().sendFailure(Component.translatable("commands.survival365.status.disabled"));
			return null;
		}
		return state;
	}
}
