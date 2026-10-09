package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.ledger.LaboratorySelection;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LaboratoryQueryPayload(UUID requestId, long position, LaboratorySelection first, LaboratorySelection second) implements CustomPacketPayload {
    public static final Type<LaboratoryQueryPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "laboratory_query"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaboratoryQueryPayload> STREAM_CODEC = new StreamCodec<>() {
        public LaboratoryQueryPayload decode(RegistryFriendlyByteBuf b) { return new LaboratoryQueryPayload(b.readUUID(), b.readLong(), read(b), read(b)); }
        public void encode(RegistryFriendlyByteBuf b, LaboratoryQueryPayload p) { b.writeUUID(p.requestId); b.writeLong(p.position); write(b, p.first); write(b, p.second); }
        private LaboratorySelection read(RegistryFriendlyByteBuf b) { return new LaboratorySelection(b.readBoolean(), b.readUtf(160), b.readVarInt()); }
        private void write(RegistryFriendlyByteBuf b, LaboratorySelection s) { b.writeBoolean(s.field()); b.writeUtf(s.cellKey(), 160); b.writeVarInt(s.fieldId()); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
