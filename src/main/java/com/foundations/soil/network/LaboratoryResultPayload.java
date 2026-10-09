package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LaboratoryResultPayload(UUID requestId, List<String> first, List<String> second) implements CustomPacketPayload {
    public static final int MAX_LINES = 96;
    public LaboratoryResultPayload {
        first = List.copyOf(first); second = List.copyOf(second);
        NetworkBounds.count(first.size(), MAX_LINES); NetworkBounds.count(second.size(), MAX_LINES);
        for (String line : first) if (line.length() > 256) throw new IllegalArgumentException("Laboratory line too long");
        for (String line : second) if (line.length() > 256) throw new IllegalArgumentException("Laboratory line too long");
    }
    public static final Type<LaboratoryResultPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "laboratory_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LaboratoryResultPayload> STREAM_CODEC = new StreamCodec<>() {
        public LaboratoryResultPayload decode(RegistryFriendlyByteBuf b) { return new LaboratoryResultPayload(b.readUUID(), read(b), read(b)); }
        public void encode(RegistryFriendlyByteBuf b, LaboratoryResultPayload p) { b.writeUUID(p.requestId); write(b, p.first); write(b, p.second); }
        private List<String> read(RegistryFriendlyByteBuf b) { int n = NetworkBounds.count(b.readVarInt(), MAX_LINES); var lines = new ArrayList<String>(n); for (int i = 0; i < n; i++) lines.add(b.readUtf(256)); return lines; }
        private void write(RegistryFriendlyByteBuf b, List<String> lines) { b.writeVarInt(lines.size()); for (String line : lines) b.writeUtf(line, 256); }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
