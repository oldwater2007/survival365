package com.junseok.survival365.network;

import com.junseok.survival365.Survival365;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C: what the client needs to draw the D-day HUD. The client never computes these values itself. */
public record CountdownSyncPayload(boolean enabled, int daysLeft, int warningDays, String scenarioId) implements CustomPacketPayload {
	public static final Type<CountdownSyncPayload> TYPE = new Type<>(Survival365.id("countdown_sync"));
	public static final StreamCodec<ByteBuf, CountdownSyncPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, CountdownSyncPayload::enabled,
		ByteBufCodecs.VAR_INT, CountdownSyncPayload::daysLeft,
		ByteBufCodecs.VAR_INT, CountdownSyncPayload::warningDays,
		ByteBufCodecs.STRING_UTF8, CountdownSyncPayload::scenarioId,
		CountdownSyncPayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
