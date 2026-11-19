package com.junseok.survival365.scenario;

import com.junseok.survival365.core.ChallengeState;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.OptionalLong;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.jspecify.annotations.Nullable;

/**
 * A selectable "world" for the 365-day challenge. One instance is created per running server
 * (and throwaway instances on the client for the create-world screen), so constructors must stay cheap.
 */
public interface WorldScenario {
	Identifier id();

	default Component displayName() {
		return Component.translatable(id().toLanguageKey("scenario"));
	}

	default Component description() {
		return Component.translatable(id().toLanguageKey("scenario") + ".desc");
	}

	/** World preset forced while this scenario is selected; null keeps the player's choice. */
	default @Nullable ResourceKey<WorldPreset> worldPreset() {
		return null;
	}

	/** Fixed time of day in ticks; empty means the normal day/night cycle. */
	default OptionalLong fixedDayTime() {
		return OptionalLong.empty();
	}

	/** Once, when the challenge starts in a new world. */
	default void onChallengeStart(MinecraftServer server, ChallengeState state) {
	}

	/** Every server tick (once, not per dimension). */
	default void tick(MinecraftServer server, ChallengeState state) {
	}

	default void onPlayerJoin(ServerPlayer player, ChallengeState state) {
	}

	default void onPlayerLeave(ServerPlayer player, ChallengeState state) {
	}

	/** Called when the server stops so per-server resources (boss bars etc.) can be released. */
	default void onServerStopped(MinecraftServer server) {
	}

	/** Extra lines for /s365 status. */
	default void appendStatus(java.util.function.Consumer<Component> out) {
	}

	/**
	 * Scenario-specific /s365 subcommands. Called once per registered scenario at command registration;
	 * handlers must look up the running scenario through {@code ChallengeManager.activeScenario()}.
	 */
	default void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
	}

	CompoundTag save();

	void load(CompoundTag tag);
}
