package com.foundations.soil.network;
import com.foundations.soil.FoundationsSoil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** A refresh cannot open a closed screen; initial guide-open remains a separate payload. */
public record FieldGuidePageReply(FieldGuideDataPayload data) implements CustomPacketPayload {
    public static final Type<FieldGuidePageReply> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "field_guide_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FieldGuidePageReply> STREAM_CODEC = FieldGuideDataPayload.STREAM_CODEC.map(FieldGuidePageReply::new, FieldGuidePageReply::data);
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
