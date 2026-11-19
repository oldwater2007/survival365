package com.junseok.survival365.client.mixin;

import com.junseok.survival365.client.ChallengeUiState;
import com.junseok.survival365.core.ChallengeOptions;
import com.junseok.survival365.core.PendingChallengeOptions;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands the challenge choice to the integrated server that "Create New World" is about to start. */
@Mixin(CreateWorldScreen.class)
abstract class CreateWorldScreenMixin {
	@Inject(method = "onCreate", at = @At("HEAD"))
	private void survival365$storeOptions(CallbackInfo ci) {
		ChallengeUiState challenge = (ChallengeUiState) ((CreateWorldScreen) (Object) this).getUiState();
		PendingChallengeOptions.set(new ChallengeOptions(challenge.survival365$isEnabled(), challenge.survival365$getScenario()));
	}

	@Inject(method = "popScreen", at = @At("HEAD"))
	private void survival365$clearOptions(CallbackInfo ci) {
		PendingChallengeOptions.clear();
	}
}
