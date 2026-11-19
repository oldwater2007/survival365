package com.junseok.survival365.client.mixin;

import com.junseok.survival365.client.ChallengeUiState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Locks the World tab's world type button while the selected scenario forces its own preset. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
abstract class WorldTabMixin {
	@Inject(method = "<init>", at = @At("RETURN"))
	private void survival365$lockWorldType(CallbackInfo ci, @Local(argsOnly = true) CreateWorldScreen screen, @Local(ordinal = 0) CycleButton<WorldCreationUiState.WorldTypeEntry> typeButton) {
		WorldCreationUiState uiState = screen.getUiState();
		ChallengeUiState challenge = (ChallengeUiState) uiState;
		// Registered after vanilla's own listener, so this runs last and wins.
		uiState.addListener(state -> {
			if (challenge.survival365$isPresetLocked()) {
				typeButton.active = false;
				typeButton.setTooltip(Tooltip.create(Component.translatable("selectWorld.survival365.world_type_locked")));
			}
		});
	}
}
