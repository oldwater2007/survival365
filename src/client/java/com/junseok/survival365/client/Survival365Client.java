package com.junseok.survival365.client;

import com.junseok.survival365.Survival365;
import com.junseok.survival365.client.hud.CountdownHud;
import com.junseok.survival365.network.CountdownSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

public class Survival365Client implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(CountdownSyncPayload.TYPE, (payload, context) -> ClientCountdownState.update(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientCountdownState.reset());
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> client.execute(() -> UpdateChecker.announce(client)));
		HudElementRegistry.addLast(Survival365.id("countdown"), new CountdownHud());
		UpdateChecker.start();
	}
}
