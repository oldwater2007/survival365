package com.junseok.survival365.client;

import net.minecraft.resources.Identifier;

/** Duck interface mixed into WorldCreationUiState: the challenge choices on the create-world screen. */
public interface ChallengeUiState {
	boolean survival365$isEnabled();

	void survival365$setEnabled(boolean enabled);

	Identifier survival365$getScenario();

	void survival365$setScenario(Identifier scenario);

	/** True while the selected scenario forces a world preset, i.e. the world type button must stay locked. */
	boolean survival365$isPresetLocked();
}
