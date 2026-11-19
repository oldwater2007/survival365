package com.junseok.survival365.client.mixin;

import com.junseok.survival365.client.ChallengeUiState;
import com.junseok.survival365.scenario.Scenarios;
import com.junseok.survival365.scenario.WorldScenario;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Stores the challenge choices on the create-world UI state and applies/undoes their side effects. */
@Mixin(WorldCreationUiState.class)
abstract class WorldCreationUiStateMixin implements ChallengeUiState {
	@Shadow
	private Difficulty difficulty;
	@Shadow
	private WorldCreationUiState.WorldTypeEntry worldType;

	@Shadow
	public abstract void setDifficulty(Difficulty difficulty);

	@Shadow
	public abstract void setWorldType(WorldCreationUiState.WorldTypeEntry worldType);

	@Shadow
	public abstract WorldCreationContext getSettings();

	@Shadow
	public abstract void updateDimensions(WorldCreationContext.DimensionsUpdater modifier);

	@Shadow
	public abstract void onChanged();

	@Unique
	private boolean survival365$enabled;
	@Unique
	private Identifier survival365$scenario = Scenarios.defaultId();
	@Unique
	private boolean survival365$presetLocked;
	@Unique
	private @Nullable Difficulty survival365$savedDifficulty;
	@Unique
	private WorldCreationUiState.@Nullable WorldTypeEntry survival365$savedWorldType;
	@Unique
	private @Nullable WorldDimensions survival365$savedDimensions;

	@Override
	public boolean survival365$isEnabled() {
		return survival365$enabled;
	}

	@Override
	public Identifier survival365$getScenario() {
		return survival365$scenario;
	}

	@Override
	public boolean survival365$isPresetLocked() {
		return survival365$enabled && survival365$presetLocked;
	}

	@Override
	public void survival365$setEnabled(boolean enabled) {
		if (enabled == survival365$enabled) {
			return;
		}
		survival365$enabled = enabled;
		if (enabled) {
			survival365$savedDifficulty = difficulty;
			survival365$savedWorldType = worldType;
			survival365$savedDimensions = getSettings().selectedDimensions();
			survival365$applyScenarioPreset();
			setDifficulty(Difficulty.HARD);
		} else {
			survival365$restoreWorldType();
			survival365$presetLocked = false;
			setDifficulty(survival365$savedDifficulty != null ? survival365$savedDifficulty : Difficulty.NORMAL);
		}
	}

	@Override
	public void survival365$setScenario(Identifier scenario) {
		survival365$scenario = scenario;
		if (survival365$enabled) {
			survival365$applyScenarioPreset();
		}
		onChanged();
	}

	@Unique
	private void survival365$applyScenarioPreset() {
		WorldScenario scenario = Scenarios.create(survival365$scenario);
		ResourceKey<WorldPreset> key = scenario == null ? null : scenario.worldPreset();
		Holder<WorldPreset> preset = key == null
			? null
			: getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).get(key).orElse(null);
		survival365$presetLocked = preset != null;
		if (preset != null) {
			setWorldType(new WorldCreationUiState.WorldTypeEntry(preset));
		} else {
			survival365$restoreWorldType();
		}
	}

	/** Puts back exactly the world type and dimensions the player had before turning the challenge on. */
	@Unique
	private void survival365$restoreWorldType() {
		if (survival365$savedWorldType == null || survival365$savedDimensions == null || worldType == survival365$savedWorldType) {
			return;
		}
		worldType = survival365$savedWorldType;
		WorldDimensions dimensions = survival365$savedDimensions;
		updateDimensions((registries, current) -> dimensions);
	}
}
