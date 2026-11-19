package com.junseok.survival365.scenario.baghdad;

import java.util.Locale;
import net.minecraft.world.BossEvent;

/** CALM → BOMBARDMENT → RAID → AFTERMATH → CALM … (SPEC §7.3). */
public enum BaghdadPhase {
	CALM(BossEvent.BossBarColor.YELLOW),
	BOMBARDMENT(BossEvent.BossBarColor.RED),
	RAID(BossEvent.BossBarColor.PURPLE),
	AFTERMATH(BossEvent.BossBarColor.GREEN);

	public final BossEvent.BossBarColor color;

	BaghdadPhase(BossEvent.BossBarColor color) {
		this.color = color;
	}

	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String bossBarKey() {
		return "bossbar.survival365.baghdad." + key();
	}

	public BaghdadPhase next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public int lengthTicks(BaghdadConfig config) {
		int seconds = switch (this) {
			case CALM -> config.calmSeconds;
			case BOMBARDMENT -> config.bombardmentSeconds;
			case RAID -> config.raidSeconds;
			case AFTERMATH -> config.aftermathSeconds;
		};
		return seconds * 20;
	}

	public static BaghdadPhase byKey(String key) {
		for (BaghdadPhase phase : values()) {
			if (phase.key().equals(key)) {
				return phase;
			}
		}
		return CALM;
	}
}
