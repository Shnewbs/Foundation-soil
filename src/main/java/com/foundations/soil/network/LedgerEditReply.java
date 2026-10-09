package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** A null snapshot is an explicit rejected edit, never an optimistic client-side mutation. */
public record LedgerEditReply(UUID requestId, String message, FieldGuideDataPayload snapshot) implements CustomPacketPayload {
    public static final Type<LedgerEditReply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "ledger_edit_reply"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LedgerEditReply> STREAM_CODEC = new StreamCodec<>() {
        public LedgerEditReply decode(RegistryFriendlyByteBuf b) {
            UUID id = b.readUUID(); String message = b.readUtf(256);
            return new LedgerEditReply(id, message, b.readBoolean() ? FieldGuideDataPayload.STREAM_CODEC.decode(b) : null);
        }
        public void encode(RegistryFriendlyByteBuf b, LedgerEditReply p) {
            b.writeUUID(p.requestId); b.writeUtf(p.message, 256); b.writeBoolean(p.snapshot != null);
            if (p.snapshot != null) FieldGuideDataPayload.STREAM_CODEC.encode(b, p.snapshot);
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
