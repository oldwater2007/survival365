package com.junseok.survival365.test;

import com.junseok.survival365.client.ChallengeUiState;
import com.junseok.survival365.client.ClientCountdownState;
import com.junseok.survival365.scenario.baghdad.BaghdadScenario;
import com.junseok.survival365.scenario.baghdad.RaidSpawner;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** SPEC §12 checks that can be automated: create-world UI, HUD sync, rules, raid and bombardment. */
public class Survival365ClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		testCreateWorldScreen(context);
		testBaghdadWorld(context);
	}

	private static void testCreateWorldScreen(ClientGameTestContext context) {
		context.runOnClient(client -> CreateWorldScreen.openFresh(client, () -> client.gui.setScreen(null)));
		context.waitForScreen(CreateWorldScreen.class);
		context.takeScreenshot("s365_create_default");
		check(context.computeOnClient(c -> !challenge(c).survival365$isEnabled()), "challenge is off by default");

		context.clickScreenButton("selectWorld.survival365.challenge");
		context.waitTick();
		context.takeScreenshot("s365_create_enabled");
		check(context.computeOnClient(c -> ui(c).getDifficulty() == Difficulty.HARD), "difficulty forced to Hard");
		check(context.computeOnClient(c -> ui(c).getWorldType().preset().is(BaghdadScenario.PRESET)), "world type forced to Baghdad");
		check(context.computeOnClient(c -> challenge(c).survival365$isPresetLocked()), "world type locked");

		context.clickScreenButton("selectWorld.survival365.challenge");
		context.waitTick();
		check(context.computeOnClient(c -> ui(c).getDifficulty() == Difficulty.NORMAL), "difficulty restored");
		check(context.computeOnClient(c -> ui(c).getWorldType().preset().is(WorldPresets.NORMAL)), "world type restored");
		context.takeScreenshot("s365_create_disabled");
		context.setScreen(() -> null);
	}

	private static void testBaghdadWorld(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(ui -> ((ChallengeUiState) ui).survival365$setEnabled(true))
				.create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("effect give @a minecraft:resistance infinite 4 true");
			context.waitFor(c -> ClientCountdownState.enabled());
			check(ClientCountdownState.daysLeft() == 365, "HUD shows D-365");

			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(level.getBiome(player.blockPosition()).is(Biomes.DESERT), "desert biome");
				check(level.getChunkSource().getGenerator().getSeaLevel() == -64, "no sea level");
				check(level.getOverworldClockTime() % 24000 == 18000, "midnight");
				check(server.getWorldData().getDifficulty() == Difficulty.HARD, "hard difficulty");
			});
			context.waitTicks(40);
			context.takeScreenshot("s365_ingame_hud");

			world.getServer().runCommand("difficulty easy");
			world.getServer().runCommand("time set day");
			context.waitTicks(25);
			world.getServer().runOnServer(server -> {
				check(server.getWorldData().getDifficulty() == Difficulty.HARD, "difficulty forced back to Hard");
				check(server.overworld().getOverworldClockTime() % 24000 == 18000, "time forced back to midnight");
			});

			world.getServer().runCommand("s365 day set 1");
			context.waitTicks(2);
			check(ClientCountdownState.daysLeft() == 1, "HUD shows D-1");

			world.getServer().runCommand("s365 phase raid");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				int raiders = server.overworld().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
					m -> m.entityTags().contains(RaidSpawner.RAIDER_TAG)).size();
				check(raiders > 0, "raid mobs spawned (" + raiders + ")");
			});
			context.waitTicks(20);
			context.takeScreenshot("s365_raid");
			world.getServer().runCommand("s365 cleanup");

			world.getServer().runCommand("s365 phase bombardment");
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				int tnt = server.overworld().getEntitiesOfClass(PrimedTnt.class, player.getBoundingBox().inflate(16, 64, 16)).size();
				check(tnt == 9, "one 3x3 wave of TNT (" + tnt + ")");
			});
			context.takeScreenshot("s365_bombardment_falling");
			context.waitTicks(60);
			context.takeScreenshot("s365_bombardment_after");
		}
	}

	private static WorldCreationUiState ui(net.minecraft.client.Minecraft client) {
		return ((CreateWorldScreen) client.gui.screen()).getUiState();
	}

	private static ChallengeUiState challenge(net.minecraft.client.Minecraft client) {
		return (ChallengeUiState) ui(client);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Check failed: " + what);
		}
	}
}
