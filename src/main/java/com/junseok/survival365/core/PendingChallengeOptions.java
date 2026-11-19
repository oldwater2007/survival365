package com.junseok.survival365.core;

import org.jspecify.annotations.Nullable;

/**
 * Hand-off from the create-world screen to the integrated server that starts right after it.
 * Set when "Create New World" is pressed, consumed by the first SERVER_STARTED of a new world.
 */
public final class PendingChallengeOptions {
	private static volatile @Nullable ChallengeOptions pending;

	private PendingChallengeOptions() {
	}

	public static void set(ChallengeOptions options) {
		pending = options;
	}

	public static @Nullable ChallengeOptions take() {
		ChallengeOptions value = pending;
		pending = null;
		return value;
	}

	public static void clear() {
		pending = null;
	}
}
