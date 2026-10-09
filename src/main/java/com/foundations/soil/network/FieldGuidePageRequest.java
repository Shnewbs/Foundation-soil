package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FieldGuidePageRequest(int fieldOffset, int cellOffset) implements CustomPacketPayload {
    public FieldGuidePageRequest { if (fieldOffset < 0 || cellOffset < 0) throw new IllegalArgumentException("Negative ledger page"); }
    public static final Type<FieldGuidePageRequest> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "ledger_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldGuidePageRequest> STREAM_CODEC = new StreamCodec<>() {
        public FieldGuidePageRequest decode(RegistryFriendlyByteBuf b) { return new FieldGuidePageRequest(b.readVarInt(), b.readVarInt()); }
        public void encode(RegistryFriendlyByteBuf b, FieldGuidePageRequest p) { b.writeVarInt(p.fieldOffset); b.writeVarInt(p.cellOffset); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
