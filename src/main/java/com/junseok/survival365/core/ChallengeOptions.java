package com.junseok.survival365.core;

import net.minecraft.resources.Identifier;

/** What the player picked on the create-world screen (or the dedicated server config). */
public record ChallengeOptions(boolean enabled, Identifier scenarioId) {
}
