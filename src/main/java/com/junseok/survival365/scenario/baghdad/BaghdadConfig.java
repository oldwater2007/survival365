package com.junseok.survival365.scenario.baghdad;

import static com.junseok.survival365.config.ModConfig.warn;

import java.util.LinkedHashMap;
import java.util.Map;

/** "baghdad" section of config/survival365.json (SPEC §9). */
public final class BaghdadConfig {
	public int calmSeconds = 600;
	public int bombardmentSeconds = 120;
	public int raidSeconds = 60;
	public int aftermathSeconds = 60;
	public long fixedDayTime = 18000;

	public int tntWaveIntervalSeconds = 20;
	public int tntGridSize = 3;
	public int tntSpacing = 4;
	public int tntDropHeight = 40;
	public int tntFuseAfterLanding = 10;
	public int maxTntPerWave = 90;

	public int raidSpawnIntervalTicks = 100;
	public int raidGroupSize = 2;
	public int raidMinDistance = 16;
	public int raidMaxDistance = 32;
	public int maxRaidMobsPerPlayer = 30;
	public double ravagerRiderChance = 0.3;
	public boolean raidersPersistent = false;
	public Map<String, Integer> raidWeights = defaultWeights();

	public String bombardmentStartSound = "minecraft:block.bell.use";
	public String raidStartSound = "minecraft:event.raid.horn";

	private static Map<String, Integer> defaultWeights() {
		Map<String, Integer> weights = new LinkedHashMap<>();
		weights.put("minecraft:pillager", 40);
		weights.put("minecraft:vindicator", 30);
		weights.put("minecraft:witch", 12);
		weights.put("minecraft:evoker", 8);
		weights.put("minecraft:ravager", 5);
		return weights;
	}

	public void validate() {
		BaghdadConfig d = new BaghdadConfig();
		if (calmSeconds < 1) calmSeconds = warn("baghdad.calmSeconds", d.calmSeconds);
		if (bombardmentSeconds < 1) bombardmentSeconds = warn("baghdad.bombardmentSeconds", d.bombardmentSeconds);
		if (raidSeconds < 1) raidSeconds = warn("baghdad.raidSeconds", d.raidSeconds);
		if (aftermathSeconds < 1) aftermathSeconds = warn("baghdad.aftermathSeconds", d.aftermathSeconds);
		if (fixedDayTime < 0 || fixedDayTime >= 24000) fixedDayTime = warn("baghdad.fixedDayTime", d.fixedDayTime);
		if (tntWaveIntervalSeconds < 1) tntWaveIntervalSeconds = warn("baghdad.tntWaveIntervalSeconds", d.tntWaveIntervalSeconds);
		if (tntGridSize < 1) tntGridSize = warn("baghdad.tntGridSize", d.tntGridSize);
		if (tntSpacing < 1) tntSpacing = warn("baghdad.tntSpacing", d.tntSpacing);
		if (tntDropHeight < 0) tntDropHeight = warn("baghdad.tntDropHeight", d.tntDropHeight);
		if (tntFuseAfterLanding < 0) tntFuseAfterLanding = warn("baghdad.tntFuseAfterLanding", d.tntFuseAfterLanding);
		if (maxTntPerWave < 0) maxTntPerWave = warn("baghdad.maxTntPerWave", d.maxTntPerWave);
		if (raidSpawnIntervalTicks < 1) raidSpawnIntervalTicks = warn("baghdad.raidSpawnIntervalTicks", d.raidSpawnIntervalTicks);
		if (raidGroupSize < 0) raidGroupSize = warn("baghdad.raidGroupSize", d.raidGroupSize);
		if (raidMinDistance < 0) raidMinDistance = warn("baghdad.raidMinDistance", d.raidMinDistance);
		if (raidMaxDistance < raidMinDistance) raidMaxDistance = warn("baghdad.raidMaxDistance", Math.max(d.raidMaxDistance, raidMinDistance));
		if (maxRaidMobsPerPlayer < 0) maxRaidMobsPerPlayer = warn("baghdad.maxRaidMobsPerPlayer", d.maxRaidMobsPerPlayer);
		if (ravagerRiderChance < 0 || ravagerRiderChance > 1) ravagerRiderChance = warn("baghdad.ravagerRiderChance", d.ravagerRiderChance);
		if (raidWeights == null || raidWeights.isEmpty()) raidWeights = warn("baghdad.raidWeights", d.raidWeights);
		raidWeights.entrySet().removeIf(e -> e.getValue() == null || e.getValue() <= 0);
		if (bombardmentStartSound == null) bombardmentStartSound = d.bombardmentStartSound;
		if (raidStartSound == null) raidStartSound = d.raidStartSound;
	}
}
