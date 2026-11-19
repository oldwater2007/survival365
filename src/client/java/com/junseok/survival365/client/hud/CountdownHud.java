package com.junseok.survival365.client.hud;

import com.junseok.survival365.client.ClientCountdownState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Top-left "D-n" counter, drawn at 2x scale (SPEC §6). Values come only from the server. */
public final class CountdownHud implements HudElement {
	private static final int X = 6;
	private static final int Y = 6;
	private static final float SCALE = 2.0F;
	private static final int COLOR_NORMAL = 0xFFFFFFFF;
	private static final int COLOR_WARNING = 0xFFFF5555;

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		if (!ClientCountdownState.enabled()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.gui.hud.isHidden() || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}
		int daysLeft = ClientCountdownState.daysLeft();
		String text = daysLeft > 0 ? "D-" + daysLeft : "D-DAY";
		int color = daysLeft <= ClientCountdownState.warningDays() ? COLOR_WARNING : COLOR_NORMAL;
		graphics.pose().pushMatrix();
		graphics.pose().translate(X, Y);
		graphics.pose().scale(SCALE, SCALE);
		graphics.text(minecraft.font, text, 0, 0, color, true);
		graphics.pose().popMatrix();
	}
}
