package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LaboratoryOpenPayload(long position, FieldGuideDataPayload ledger) implements CustomPacketPayload {
    public static final Type<LaboratoryOpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "laboratory_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaboratoryOpenPayload> STREAM_CODEC = new StreamCodec<>() {
        public LaboratoryOpenPayload decode(RegistryFriendlyByteBuf b) { return new LaboratoryOpenPayload(b.readLong(), FieldGuideDataPayload.STREAM_CODEC.decode(b)); }
        public void encode(RegistryFriendlyByteBuf b, LaboratoryOpenPayload p) { b.writeLong(p.position); FieldGuideDataPayload.STREAM_CODEC.encode(b, p.ledger); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
