package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.ledger.LedgerMutation;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LedgerEditPayload(UUID requestId, LedgerMutation mutation, int fieldOffset, int cellOffset) implements CustomPacketPayload {
    public static final Type<LedgerEditPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "ledger_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LedgerEditPayload> STREAM_CODEC = new StreamCodec<>() {
        public LedgerEditPayload decode(RegistryFriendlyByteBuf b) {
            return new LedgerEditPayload(b.readUUID(), new LedgerMutation(b.readEnum(LedgerMutation.Action.class),
                b.readUtf(160), b.readVarInt(), b.readUtf(64), b.readUtf(256), b.readBoolean()), b.readVarInt(), b.readVarInt());
        }
        public void encode(RegistryFriendlyByteBuf b, LedgerEditPayload p) {
            b.writeUUID(p.requestId); b.writeEnum(p.mutation.action()); b.writeUtf(p.mutation.cellKey(), 160);
            b.writeVarInt(p.mutation.fieldId()); b.writeUtf(p.mutation.name(), 64); b.writeUtf(p.mutation.note(), 256);
            b.writeBoolean(p.mutation.favorite()); b.writeVarInt(p.fieldOffset); b.writeVarInt(p.cellOffset);
        }
    };
    public LedgerEditPayload {
        if (requestId == null || mutation == null || fieldOffset < 0 || cellOffset < 0) throw new IllegalArgumentException("Invalid ledger edit");
    }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
