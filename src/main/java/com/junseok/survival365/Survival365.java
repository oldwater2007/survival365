package com.junseok.survival365;

import com.junseok.survival365.command.Survival365Command;
import com.junseok.survival365.config.ModConfig;
import com.junseok.survival365.core.ChallengeManager;
import com.junseok.survival365.network.CountdownSyncPayload;
import com.junseok.survival365.scenario.Scenarios;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Survival365 implements ModInitializer {
	public static final String MOD_ID = "survival365";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Scenarios.bootstrap();
		ModConfig.load();
		PayloadTypeRegistry.clientboundPlay().register(CountdownSyncPayload.TYPE, CountdownSyncPayload.CODEC);
		ChallengeManager.register();
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> Survival365Command.register(dispatcher));
	}
}
