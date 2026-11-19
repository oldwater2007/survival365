package com.junseok.survival365.core;

import com.junseok.survival365.Survival365;
import com.junseok.survival365.scenario.WorldScenario;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Per-world challenge progress, stored in the server's data folder as survival365_challenge.dat. */
public final class ChallengeState extends SavedData {
	public static final int FORMAT_VERSION = 1;

	public static final Codec<ChallengeState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.optionalFieldOf("enabled", false).forGetter(s -> s.enabled),
		Codec.STRING.optionalFieldOf("scenarioId", "").forGetter(s -> s.scenarioId),
		Codec.LONG.optionalFieldOf("startDayTime", 0L).forGetter(s -> s.startDayTime),
		Codec.LONG.optionalFieldOf("elapsedTicks", 0L).forGetter(s -> s.elapsedTicks),
		Codec.INT.optionalFieldOf("lastAnnouncedDay", -1).forGetter(s -> s.lastAnnouncedDay),
		Codec.BOOL.optionalFieldOf("completed", false).forGetter(s -> s.completed),
		Codec.INT.optionalFieldOf("formatVersion", FORMAT_VERSION).forGetter(s -> FORMAT_VERSION),
		CompoundTag.CODEC.optionalFieldOf("scenarioData", new CompoundTag()).forGetter(ChallengeState::currentScenarioData)
	).apply(i, ChallengeState::new));

	// Saved as data/survival365/challenge.dat. Null DataFixTypes is supported by Fabric API (SavedDataStorageMixin).
	public static final SavedDataType<ChallengeState> TYPE = new SavedDataType<>(
		Survival365.id("challenge"), ChallengeState::new, CODEC, null
	);

	public boolean enabled;
	public String scenarioId;
	public long startDayTime;
	public long elapsedTicks;
	public int lastAnnouncedDay;
	public boolean completed;
	public final int loadedFormatVersion;
	private CompoundTag scenarioData;
	/** Live scenario; its save() replaces scenarioData whenever the state is written. */
	private transient @Nullable WorldScenario scenario;

	private ChallengeState() {
		this(false, "", 0L, 0L, -1, false, FORMAT_VERSION, new CompoundTag());
	}

	private ChallengeState(boolean enabled, String scenarioId, long startDayTime, long elapsedTicks, int lastAnnouncedDay,
			boolean completed, int formatVersion, CompoundTag scenarioData) {
		this.enabled = enabled;
		this.scenarioId = scenarioId;
		this.startDayTime = startDayTime;
		this.elapsedTicks = elapsedTicks;
		this.lastAnnouncedDay = lastAnnouncedDay;
		this.completed = completed;
		this.loadedFormatVersion = formatVersion;
		this.scenarioData = scenarioData;
	}

	public CompoundTag scenarioData() {
		return scenarioData;
	}

	public void attachScenario(@Nullable WorldScenario scenario) {
		this.scenario = scenario;
	}

	private CompoundTag currentScenarioData() {
		if (scenario != null) {
			scenarioData = scenario.save();
		}
		return scenarioData;
	}
}
