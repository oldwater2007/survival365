package com.junseok.survival365.scenario.baghdad;

import net.minecraft.nbt.CompoundTag;

/** Phase state machine: which phase is running and how many ticks are left in it. */
public final class BaghdadCycle {
	private BaghdadPhase phase = BaghdadPhase.CALM;
	private int ticksLeft;
	private int phaseLength;

	public BaghdadPhase phase() {
		return phase;
	}

	public int ticksLeft() {
		return ticksLeft;
	}

	/** Ticks since the current phase started (0 on its first tick). */
	public int ticksIntoPhase() {
		return Math.max(0, phaseLength - ticksLeft);
	}

	public float progress() {
		return phaseLength <= 0 ? 0.0F : Math.clamp((float) ticksLeft / phaseLength, 0.0F, 1.0F);
	}

	public void start(BaghdadPhase newPhase, BaghdadConfig config) {
		phase = newPhase;
		phaseLength = newPhase.lengthTicks(config);
		ticksLeft = phaseLength;
	}

	/** Counts one tick down; returns true when the phase has just run out. */
	public boolean countDown() {
		ticksLeft--;
		return ticksLeft <= 0;
	}

	public void skip(int ticks) {
		ticksLeft = Math.max(1, ticksLeft - ticks);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putString("phase", phase.key());
		tag.putInt("phaseTicksLeft", ticksLeft);
		tag.putInt("phaseLength", phaseLength);
		return tag;
	}

	public void load(CompoundTag tag, BaghdadConfig config) {
		phase = BaghdadPhase.byKey(tag.getStringOr("phase", BaghdadPhase.CALM.key()));
		phaseLength = tag.getIntOr("phaseLength", phase.lengthTicks(config));
		ticksLeft = tag.getIntOr("phaseTicksLeft", phaseLength);
		if (ticksLeft <= 0) {
			ticksLeft = phaseLength;
		}
	}
}
