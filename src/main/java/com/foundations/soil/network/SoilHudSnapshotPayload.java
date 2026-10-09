package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.integration.TfcSoilSnapshot;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public record SoilHudSnapshotPayload(
    long packedPos,
    int cellX,
    int cellZ,
    String tfcSoilBlock,
    boolean tfcFarmlandDataPresent,
    int tfcHydration,
    int tfcMinHydration,
    int tfcMaxHydration,
    double tfcNitrogen,
    double tfcPhosphorus,
    double tfcPotassium,
    double organicMatter,
    double pH,
    double drainage,
    double compaction,
    double structure,
    double biology,
    double salinity,
    double nutrientAccess,
    int amendmentApplications,
    double continuousPressure,
    double organicReserve,
    int cropCycles,
    int activePlantings,
    String lastHarvestFamily
) implements CustomPacketPayload {
    public static final Type<SoilHudSnapshotPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "soil_hud_snapshot")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, SoilHudSnapshotPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SoilHudSnapshotPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoilHudSnapshotPayload(
                buffer.readLong(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readUtf(128), buffer.readBoolean(), buffer.readInt(), buffer.readInt(), buffer.readInt(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readUtf(64)
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SoilHudSnapshotPayload payload) {
            buffer.writeLong(payload.packedPos());
            buffer.writeVarInt(payload.cellX());
            buffer.writeVarInt(payload.cellZ());
            buffer.writeUtf(payload.tfcSoilBlock(), 128);
            buffer.writeBoolean(payload.tfcFarmlandDataPresent());
            buffer.writeInt(payload.tfcHydration());
            buffer.writeInt(payload.tfcMinHydration());
            buffer.writeInt(payload.tfcMaxHydration());
            buffer.writeDouble(payload.tfcNitrogen());
            buffer.writeDouble(payload.tfcPhosphorus());
            buffer.writeDouble(payload.tfcPotassium());
            buffer.writeDouble(payload.organicMatter());
            buffer.writeDouble(payload.pH());
            buffer.writeDouble(payload.drainage());
            buffer.writeDouble(payload.compaction());
            buffer.writeDouble(payload.structure());
            buffer.writeDouble(payload.biology());
            buffer.writeDouble(payload.salinity());
            buffer.writeDouble(payload.nutrientAccess());
            buffer.writeVarInt(payload.amendmentApplications());
            buffer.writeDouble(payload.continuousPressure());
            buffer.writeDouble(payload.organicReserve());
            buffer.writeVarInt(payload.cropCycles());
            buffer.writeVarInt(payload.activePlantings());
            buffer.writeUtf(payload.lastHarvestFamily(), 64);
        }
    };

    public static SoilHudSnapshotPayload from(ServerLevel level, BlockPos pos, SoilCell cell) {
        SoilProfile p = cell.profile();
        TfcSoilSnapshot tfc = TfcSoilSnapshot.capture(level, pos);
        return new SoilHudSnapshotPayload(
            pos.asLong(), cell.cellX(), cell.cellZ(),
            tfc.soilBlock(), tfc.farmlandDataPresent(), tfc.hydration(), tfc.minHydration(), tfc.maxHydration(),
            tfc.nitrogen(), tfc.phosphorus(), tfc.potassium(),
            p.organicMatter(), p.pH(), p.drainage(), p.compaction(), p.structure(), p.biology(), p.salinity(),
            cell.nutrientAccessModifier(), cell.amendmentApplications(), cell.continuousPressure(),
            cell.organicReserve() + cell.residueReserve(), cell.cropCycles(), cell.activePlantings(), cell.lastHarvestFamily()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
