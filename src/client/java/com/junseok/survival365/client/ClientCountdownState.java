package com.junseok.survival365.client;

import com.junseok.survival365.network.CountdownSyncPayload;

/** Last values received from the server. Display only; nothing is computed here. */
public final class ClientCountdownState {
	private static volatile boolean enabled;
	private static volatile int daysLeft;
	private static volatile int warningDays;
	private static volatile String scenarioId = "";

	private ClientCountdownState() {
	}

	public static void update(CountdownSyncPayload payload) {
		enabled = payload.enabled();
		daysLeft = payload.daysLeft();
		warningDays = payload.warningDays();
		scenarioId = payload.scenarioId();
	}

	public static void reset() {
		enabled = false;
		daysLeft = 0;
		scenarioId = "";
	}

	public static boolean enabled() {
		return enabled;
	}

	public static int daysLeft() {
		return daysLeft;
	}

	public static int warningDays() {
		return warningDays;
	}

	public static String scenarioId() {
		return scenarioId;
	}
}
