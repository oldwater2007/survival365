package com.junseok.survival365.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

/** Small per-player feedback helpers (titles, action bar, sounds by configurable id). */
public final class Effects {
	private Effects() {
	}

	public static void title(ServerPlayer player, Component text) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
		player.connection.send(new ClientboundSetTitleTextPacket(text));
	}

	public static void actionBar(ServerPlayer player, Component text) {
		player.connection.send(new ClientboundSetActionBarTextPacket(text));
	}

	/** Plays a sound only this player hears. Unknown or empty ids are ignored (config typo should not crash). */
	public static void sound(ServerPlayer player, String soundId, float volume, float pitch) {
		Identifier id = soundId == null || soundId.isEmpty() ? null : Identifier.tryParse(soundId);
		if (id == null) {
			return;
		}
		BuiltInRegistries.SOUND_EVENT.get(id).ifPresent(sound -> player.connection.send(new ClientboundSoundPacket(
			sound, SoundSource.MASTER, player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()
		)));
	}
}
