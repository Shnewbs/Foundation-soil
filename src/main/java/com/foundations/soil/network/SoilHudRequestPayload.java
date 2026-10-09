package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SoilHudRequestPayload(long packedPos) implements CustomPacketPayload {
    public static final Type<SoilHudRequestPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "soil_hud_request")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SoilHudRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SoilHudRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoilHudRequestPayload(buffer.readLong());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SoilHudRequestPayload payload) {
            buffer.writeLong(payload.packedPos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
