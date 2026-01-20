package aqario.equine.common.network;

import aqario.equine.common.Equine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ServerboundHorseRearUpPayload implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ServerboundHorseRearUpPayload> STREAM_CODEC = CustomPacketPayload.codec(
        (object, object2) -> {
        }, object -> new ServerboundHorseRearUpPayload()
    );
    public static final CustomPacketPayload.Type<ServerboundHorseRearUpPayload> TYPE = new CustomPacketPayload.Type<>(
        Equine.id("horse_rear_up")
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
