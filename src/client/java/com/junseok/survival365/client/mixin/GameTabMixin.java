package com.junseok.survival365.client.mixin;

import com.junseok.survival365.client.ChallengeUiState;
import com.junseok.survival365.scenario.Scenarios;
import com.junseok.survival365.scenario.WorldScenario;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds "365-Day Survival" and "World" right under the Game tab's difficulty button, and locks difficulty. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
abstract class GameTabMixin {
	// Just before the "Allow Commands" button is built, i.e. right after the difficulty button and its listener.
	@Inject(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CycleButton;onOffBuilder(Z)Lnet/minecraft/client/gui/components/CycleButton$Builder;")
	)
	private void survival365$addChallengeButtons(CallbackInfo ci, @Local(argsOnly = true) CreateWorldScreen screen, @Local GridLayout.RowHelper helper,
			@Local(ordinal = 1) CycleButton<Difficulty> difficultyButton) {
		WorldCreationUiState uiState = screen.getUiState();
		ChallengeUiState challenge = (ChallengeUiState) uiState;
		CycleButton<Boolean> challengeButton = helper.addChild(
			CycleButton.onOffBuilder(challenge.survival365$isEnabled())
				.withTooltip(value -> Tooltip.create(Component.translatable("selectWorld.survival365.challenge.info")))
				.create(0, 0, 210, 20, Component.translatable("selectWorld.survival365.challenge"),
					(button, value) -> challenge.survival365$setEnabled(value))
		);
		CycleButton<Identifier> scenarioButton = helper.addChild(
			CycleButton.<Identifier>builder(GameTabMixin::survival365$scenarioName, challenge.survival365$getScenario())
				.withValues(Scenarios.ids())
				.withTooltip(id -> Tooltip.create(survival365$scenarioDescription(id)))
				.create(0, 0, 210, 20, Component.translatable("selectWorld.survival365.scenario"),
					(button, value) -> challenge.survival365$setScenario(value))
		);
		scenarioButton.visible = challenge.survival365$isEnabled();
		// Registered after vanilla's difficulty listener, so the lock below wins over "active = !hardcore".
		uiState.addListener(state -> {
			boolean enabled = challenge.survival365$isEnabled();
			challengeButton.setValue(enabled);
			scenarioButton.visible = enabled;
			scenarioButton.setValue(challenge.survival365$getScenario());
			if (enabled) {
				difficultyButton.setValue(Difficulty.HARD);
				difficultyButton.active = false;
				difficultyButton.setTooltip(Tooltip.create(Component.translatable("selectWorld.survival365.difficulty_locked")));
			}
		});
	}

	@Unique
	private static Component survival365$scenarioName(Identifier id) {
		WorldScenario scenario = Scenarios.create(id);
		return scenario != null ? scenario.displayName() : Component.literal(id.toString());
	}

	@Unique
	private static Component survival365$scenarioDescription(Identifier id) {
		WorldScenario scenario = Scenarios.create(id);
		return scenario != null ? scenario.description() : Component.empty();
	}
}
