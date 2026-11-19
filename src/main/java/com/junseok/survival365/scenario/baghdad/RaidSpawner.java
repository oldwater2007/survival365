package com.junseok.survival365.scenario.baghdad;

import com.junseok.survival365.Survival365;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Spawns illager groups around target players during the RAID phase (SPEC §7.5). No vanilla Raid involved. */
public final class RaidSpawner {
	public static final String RAIDER_TAG = "survival365_raider";
	private static final int MAX_POSITION_TRIES = 10;
	private static final double CAP_RADIUS = 64.0;

	private RaidSpawner() {
	}

	public static void spawnRound(ServerLevel level, List<ServerPlayer> targets, BaghdadConfig config) {
		List<Weighted> table = weights(config);
		if (table.isEmpty()) {
			return;
		}
		RandomSource random = level.getRandom();
		for (ServerPlayer player : targets) {
			AABB area = player.getBoundingBox().inflate(CAP_RADIUS);
			int nearby = level.getEntitiesOfClass(Mob.class, area, m -> m.entityTags().contains(RAIDER_TAG)).size();
			for (int i = 0; i < config.raidGroupSize && nearby < config.maxRaidMobsPerPlayer; i++) {
				if (spawnOne(level, player, pick(table, random), config, random)) {
					nearby++;
				}
			}
		}
	}

	private static boolean spawnOne(ServerLevel level, ServerPlayer target, EntityType<?> type, BaghdadConfig config, RandomSource random) {
		BlockPos pos = findPosition(level, target, type, config, random);
		if (pos == null) {
			return false;
		}
		Mob mob = createMob(level, type, pos, target, config, random);
		if (mob == null) {
			return false;
		}
		if (type == EntityTypes.RAVAGER && random.nextDouble() < config.ravagerRiderChance) {
			Mob rider = createMob(level, EntityTypes.PILLAGER, pos, target, config, random);
			if (rider != null) {
				rider.startRiding(mob, true, false);
			}
		}
		level.addFreshEntityWithPassengers(mob);
		return true;
	}

	private static @Nullable Mob createMob(ServerLevel level, EntityType<?> type, BlockPos pos, ServerPlayer target, BaghdadConfig config, RandomSource random) {
		Entity entity = type.create(level, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob mob)) {
			if (entity != null) {
				entity.discard();
			}
			return null;
		}
		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		// EVENT reason already skips the patrol-leader roll; reset anyway so no ominous banner is ever given.
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		if (mob instanceof PatrollingMonster patrolling) {
			patrolling.setPatrolLeader(false);
		}
		mob.setTarget(target);
		mob.addTag(RAIDER_TAG);
		if (config.raidersPersistent) {
			mob.setPersistenceRequired();
		}
		return mob;
	}

	private static @Nullable BlockPos findPosition(ServerLevel level, ServerPlayer target, EntityType<?> type, BaghdadConfig config, RandomSource random) {
		for (int attempt = 0; attempt < MAX_POSITION_TRIES; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = config.raidMinDistance + random.nextDouble() * (config.raidMaxDistance - config.raidMinDistance);
			int x = (int) Math.floor(target.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(target.getZ() + Math.sin(angle) * distance);
			if (!level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
				continue;
			}
			BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			BlockPos below = pos.below();
			if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
				continue;
			}
			// Spawn box covers the mob's own size, so the ravager automatically needs its larger free space.
			AABB box = type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
			if (level.noCollision(box) && !level.containsAnyLiquid(box)) {
				return pos;
			}
		}
		return null;
	}

	private record Weighted(EntityType<?> type, int weight) {
	}

	private static List<Weighted> weights(BaghdadConfig config) {
		List<Weighted> table = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : config.raidWeights.entrySet()) {
			Identifier id = Identifier.tryParse(entry.getKey());
			if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
				Survival365.LOGGER.warn("Unknown raid mob '{}' in config, skipped", entry.getKey());
				continue;
			}
			table.add(new Weighted(BuiltInRegistries.ENTITY_TYPE.getValue(id), entry.getValue()));
		}
		return table;
	}

	private static EntityType<?> pick(List<Weighted> table, RandomSource random) {
		int total = 0;
		for (Weighted w : table) {
			total += w.weight();
		}
		int roll = random.nextInt(total);
		for (Weighted w : table) {
			roll -= w.weight();
			if (roll < 0) {
				return w.type();
			}
		}
		return table.getLast().type();
	}
}
