package com.junseok.survival365.scenario.baghdad;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** One bombardment wave: a grid of falling primed TNT above each target player (SPEC §7.4). */
public final class TntBombardment {
	// Vanilla PrimedTnt physics: getDefaultGravity() = 0.04, Entity.getAirDrag() = 0.98, applied gravity → move → drag.
	private static final double GRAVITY = 0.04;
	private static final double AIR_DRAG = 0.98;
	private static final int MAX_FALL_TICKS = 400;

	private TntBombardment() {
	}

	public static void wave(ServerLevel level, List<ServerPlayer> targets, BaghdadConfig config) {
		LongSet used = new LongOpenHashSet();
		int spawned = 0;
		int half = (config.tntGridSize - 1) / 2;
		for (ServerPlayer player : targets) {
			BlockPos base = player.blockPosition();
			for (int gx = 0; gx < config.tntGridSize; gx++) {
				for (int gz = 0; gz < config.tntGridSize; gz++) {
					if (spawned >= config.maxTntPerWave) {
						return;
					}
					int x = base.getX() + (gx - half) * config.tntSpacing;
					int z = base.getZ() + (gz - half) * config.tntSpacing;
					if (!used.add(BlockPos.asLong(x, 0, z))) {
						continue;
					}
					if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
						continue;
					}
					// First free block above the ground (players dug in below the surface are not targeted directly).
					int landY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					int spawnY = Math.min(landY + config.tntDropHeight, level.getMaxY() - 2);
					PrimedTnt tnt = new PrimedTnt(level, x + 0.5, spawnY, z + 0.5, null);
					tnt.setDeltaMovement(Vec3.ZERO);
					tnt.setFuse(fallTicks(spawnY, landY) + config.tntFuseAfterLanding);
					level.addFreshEntity(tnt);
					spawned++;
				}
			}
		}
	}

	/** Ticks a TNT dropped from rest at {@code fromY} needs to reach {@code landY}. */
	static int fallTicks(double fromY, double landY) {
		double y = fromY;
		double vy = 0.0;
		int ticks = 0;
		while (y > landY && ticks < MAX_FALL_TICKS) {
			vy -= GRAVITY;
			y += vy;
			vy *= AIR_DRAG;
			ticks++;
		}
		return ticks;
	}
}
