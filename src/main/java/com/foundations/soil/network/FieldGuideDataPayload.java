package com.foundations.soil.network;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.config.SoilConfig;
import com.foundations.soil.ledger.FieldLedgerEntry;
import com.foundations.soil.ledger.FieldLedgerField;
import com.foundations.soil.ledger.FieldLedgerManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Bounded read-only server snapshot used by the Field Guide GUI. */
public record FieldGuideDataPayload(
    List<FieldView> fields,
    List<CellView> cells,
    List<AmendmentView> amendments,
    WarningView warnings,
    String preset,
    int applicationsPerBag,
    long currentTfcDay,
    int totalCells,
    boolean truncated
) implements CustomPacketPayload {
    private static final int HARD_MAX_SYNC_CELLS = 2048;
    private static final int HARD_MAX_SYNC_FIELDS = 512;
    private static final int HARD_MAX_EVENTS_PER_CELL = 16;
    private static final int HARD_MAX_AMENDMENTS = 64;

    public static final Type<FieldGuideDataPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(FoundationsSoil.MOD_ID, "field_guide_data")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FieldGuideDataPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public FieldGuideDataPayload decode(RegistryFriendlyByteBuf buffer) {
            int fieldCount = Math.min(HARD_MAX_SYNC_FIELDS, buffer.readVarInt());
            ArrayList<FieldView> fields = new ArrayList<>(fieldCount);
            for (int i = 0; i < fieldCount; i++) fields.add(FieldView.decode(buffer));

            int cellCount = Math.min(HARD_MAX_SYNC_CELLS, buffer.readVarInt());
            ArrayList<CellView> cells = new ArrayList<>(cellCount);
            for (int i = 0; i < cellCount; i++) cells.add(CellView.decode(buffer));

            int amendmentCount = Math.min(HARD_MAX_AMENDMENTS, buffer.readVarInt());
            ArrayList<AmendmentView> amendments = new ArrayList<>(amendmentCount);
            for (int i = 0; i < amendmentCount; i++) amendments.add(AmendmentView.decode(buffer));

            WarningView warnings = WarningView.decode(buffer);
            String preset = buffer.readUtf(32);
            int applicationsPerBag = buffer.readVarInt();
            long currentTfcDay = buffer.readVarLong();
            int totalCells = buffer.readVarInt();
            boolean truncated = buffer.readBoolean();
            return new FieldGuideDataPayload(List.copyOf(fields), List.copyOf(cells), List.copyOf(amendments),
                warnings, preset, applicationsPerBag, currentTfcDay, totalCells, truncated);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, FieldGuideDataPayload payload) {
            buffer.writeVarInt(payload.fields().size());
            for (FieldView field : payload.fields()) field.encode(buffer);
            buffer.writeVarInt(payload.cells().size());
            for (CellView cell : payload.cells()) cell.encode(buffer);
            buffer.writeVarInt(payload.amendments().size());
            for (AmendmentView amendment : payload.amendments()) amendment.encode(buffer);
            payload.warnings().encode(buffer);
            buffer.writeUtf(payload.preset(), 32);
            buffer.writeVarInt(payload.applicationsPerBag());
            buffer.writeVarLong(payload.currentTfcDay());
            buffer.writeVarInt(payload.totalCells());
            buffer.writeBoolean(payload.truncated());
        }
    };

    public static FieldGuideDataPayload from(ServerPlayer player) {
        int maxFields = Math.min(HARD_MAX_SYNC_FIELDS, SoilConfig.LEDGER_MAX_SYNC_FIELDS.get());
        int maxCells = Math.min(HARD_MAX_SYNC_CELLS, SoilConfig.LEDGER_MAX_SYNC_CELLS.get());
        int maxEvents = Math.min(HARD_MAX_EVENTS_PER_CELL, SoilConfig.LEDGER_EVENTS_PER_CELL_SYNC.get());

        List<FieldLedgerField> rawFields = FieldLedgerManager.fields(player);
        ArrayList<FieldView> fields = new ArrayList<>();
        for (int i = 0; i < Math.min(maxFields, rawFields.size()); i++) {
            FieldLedgerField field = rawFields.get(i);
            fields.add(new FieldView(field.id(), field.name(), field.note(), field.favorite(), FieldLedgerManager.fieldMembers(player, field.id())));
        }

        List<FieldLedgerEntry> rawCells = FieldLedgerManager.entries(player);
        ArrayList<CellView> cells = new ArrayList<>();
        for (int i = 0; i < Math.min(maxCells, rawCells.size()); i++) cells.add(CellView.from(rawCells.get(i), maxEvents));

        ArrayList<AmendmentView> amendments = new ArrayList<>();
        double strength = SoilConfig.effectiveAmendmentStrength();
        for (AmendmentDefinition definition : AmendmentRegistry.all().stream().limit(HARD_MAX_AMENDMENTS).toList()) {
            amendments.add(AmendmentView.from(definition, strength));
        }

        return new FieldGuideDataPayload(List.copyOf(fields), List.copyOf(cells), List.copyOf(amendments),
            WarningView.server(), SoilConfig.preset(), SoilConfig.APPLICATIONS_PER_BAG.get(),
            Math.max(0L, Calendars.SERVER.getTotalCalendarDays()), rawCells.size(), rawCells.size() > cells.size());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record FieldView(int id, String name, String note, boolean favorite, int members) {
        private static FieldView decode(RegistryFriendlyByteBuf buffer) {
            return new FieldView(buffer.readVarInt(), buffer.readUtf(64), buffer.readUtf(256), buffer.readBoolean(), buffer.readVarInt());
        }
        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(id);
            buffer.writeUtf(name, 64);
            buffer.writeUtf(note, 256);
            buffer.writeBoolean(favorite);
            buffer.writeVarInt(members);
        }
    }

    public record AmendmentView(
        String id, String name, String description, String form, String doseUnit,
        double referenceAmount, double maxSingleAmount, double maxConcentration,
        double immediateFraction, double releaseDays, double overapplicationThreshold, double overapplicationSalinity,
        double organicMatter, double pH, double drainage, double compaction,
        double structure, double biology, double salinity, double organicReserve,
        double decompositionRate, double diminishingWeight
    ) {
        static AmendmentView from(AmendmentDefinition raw, double strength) {
            AmendmentDefinition a = raw.scaled(strength);
            return new AmendmentView(a.id(), a.displayName(), a.description(), a.form().name().toLowerCase(Locale.ROOT),
                a.doseUnit().name().toLowerCase(Locale.ROOT), a.referenceAmount(), a.maxSingleAmount(), a.maxConcentration(),
                a.immediateFraction(), a.releaseDays(), a.overapplicationThreshold(), a.overapplicationSalinity(),
                a.organicMatter(), a.pH(), a.drainage(), a.compaction(), a.structure(), a.biology(), a.salinity(),
                a.organicReserve(), a.decompositionRate(), a.diminishingWeight());
        }

        static AmendmentView decode(RegistryFriendlyByteBuf b) {
            return new AmendmentView(b.readUtf(64), b.readUtf(80), b.readUtf(320), b.readUtf(32), b.readUtf(32),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
        }

        void encode(RegistryFriendlyByteBuf b) {
            b.writeUtf(id, 64); b.writeUtf(name, 80); b.writeUtf(description, 320); b.writeUtf(form, 32); b.writeUtf(doseUnit, 32);
            b.writeDouble(referenceAmount); b.writeDouble(maxSingleAmount); b.writeDouble(maxConcentration);
            b.writeDouble(immediateFraction); b.writeDouble(releaseDays); b.writeDouble(overapplicationThreshold); b.writeDouble(overapplicationSalinity);
            b.writeDouble(organicMatter); b.writeDouble(pH); b.writeDouble(drainage); b.writeDouble(compaction);
            b.writeDouble(structure); b.writeDouble(biology); b.writeDouble(salinity); b.writeDouble(organicReserve);
            b.writeDouble(decompositionRate); b.writeDouble(diminishingWeight);
        }
    }

    public record WarningView(double lowOrganicMatter, double highCompaction, double highSalinity,
                              double highCropPressure, double acidicPH, double alkalinePH, double lowBiology) {
        static WarningView server() {
            return new WarningView(SoilConfig.WARNING_LOW_ORGANIC_MATTER.get(), SoilConfig.WARNING_HIGH_COMPACTION.get(),
                SoilConfig.WARNING_HIGH_SALINITY.get(), SoilConfig.WARNING_HIGH_CROP_PRESSURE.get(),
                SoilConfig.WARNING_ACIDIC_PH.get(), SoilConfig.WARNING_ALKALINE_PH.get(), SoilConfig.WARNING_LOW_BIOLOGY.get());
        }
        static WarningView decode(RegistryFriendlyByteBuf b) {
            return new WarningView(b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble());
        }
        void encode(RegistryFriendlyByteBuf b) {
            b.writeDouble(lowOrganicMatter); b.writeDouble(highCompaction); b.writeDouble(highSalinity);
            b.writeDouble(highCropPressure); b.writeDouble(acidicPH); b.writeDouble(alkalinePH); b.writeDouble(lowBiology);
        }
    }

    public record CellView(
        String dimension, int cellX, int cellZ, int sampleY, long lastScannedDay,
        boolean favorite, String note, int fieldId,
        String tfcSoilBlock, boolean tfcFarmlandDataPresent, int tfcHydration, int tfcMinHydration, int tfcMaxHydration,
        double tfcNitrogen, double tfcPhosphorus, double tfcPotassium,
        double organicMatter, double pH, double drainage, double compaction,
        double structure, double biology, double salinity, double nutrientAccess,
        int amendmentApplications, int cropCycles, int continuousCropCycles,
        double continuousPressure, long daysBare, long daysCovered, double organicReserve,
        int activePlantings, String lastHarvestFamily,
        String lastAmendmentId, long lastAmendmentDay, double lastAmendmentAmount, String lastAmendmentUnit,
        double lastAmendmentConcentration, boolean lastAmendmentOverApplied, int pendingAmendmentCount,
        double pendingAmendmentDaysRemaining, double recentAmendmentLoad, List<String> recentEvents
    ) {
        private static CellView from(FieldLedgerEntry entry, int maxEvents) {
            List<String> events = entry.recentEvents().stream().limit(Math.max(0, maxEvents)).toList();
            return new CellView(
                entry.dimension(), entry.cellX(), entry.cellZ(), entry.sampleY(), entry.lastScannedDay(),
                entry.favorite(), entry.note(), entry.fieldId(),
                entry.tfcSoilBlock(), entry.tfcFarmlandDataPresent(), entry.tfcHydration(), entry.tfcMinHydration(), entry.tfcMaxHydration(),
                entry.tfcNitrogen(), entry.tfcPhosphorus(), entry.tfcPotassium(),
                entry.organicMatter(), entry.pH(), entry.drainage(), entry.compaction(),
                entry.structure(), entry.biology(), entry.salinity(), entry.nutrientAccess(),
                entry.amendmentApplications(), entry.cropCycles(), entry.continuousCropCycles(), entry.continuousPressure(),
                entry.daysBare(), entry.daysCovered(), entry.organicReserve(), entry.activePlantings(),
                entry.lastHarvestFamily(), entry.lastAmendmentId(), entry.lastAmendmentDay(), entry.lastAmendmentAmount(),
                entry.lastAmendmentUnit(), entry.lastAmendmentConcentration(), entry.lastAmendmentOverApplied(),
                entry.pendingAmendmentCount(), entry.pendingAmendmentDaysRemaining(), entry.recentAmendmentLoad(), events
            );
        }

        private static CellView decode(RegistryFriendlyByteBuf buffer) {
            String dimension = buffer.readUtf(128);
            int cellX = buffer.readVarInt();
            int cellZ = buffer.readVarInt();
            int sampleY = buffer.readVarInt();
            long lastScannedDay = buffer.readVarLong();
            boolean favorite = buffer.readBoolean();
            String note = buffer.readUtf(256);
            int fieldId = buffer.readVarInt();
            String tfcSoilBlock = buffer.readUtf(128);
            boolean tfcFarmlandDataPresent = buffer.readBoolean();
            int tfcHydration = buffer.readInt();
            int tfcMinHydration = buffer.readInt();
            int tfcMaxHydration = buffer.readInt();
            double tfcNitrogen = buffer.readDouble();
            double tfcPhosphorus = buffer.readDouble();
            double tfcPotassium = buffer.readDouble();
            double organicMatter = buffer.readDouble();
            double pH = buffer.readDouble();
            double drainage = buffer.readDouble();
            double compaction = buffer.readDouble();
            double structure = buffer.readDouble();
            double biology = buffer.readDouble();
            double salinity = buffer.readDouble();
            double nutrientAccess = buffer.readDouble();
            int amendmentApplications = buffer.readVarInt();
            int cropCycles = buffer.readVarInt();
            int continuousCropCycles = buffer.readVarInt();
            double continuousPressure = buffer.readDouble();
            long daysBare = buffer.readVarLong();
            long daysCovered = buffer.readVarLong();
            double organicReserve = buffer.readDouble();
            int activePlantings = buffer.readVarInt();
            String lastHarvestFamily = buffer.readUtf(64);
            String lastAmendmentId = buffer.readUtf(64);
            long lastAmendmentDay = buffer.readLong();
            double lastAmendmentAmount = buffer.readDouble();
            String lastAmendmentUnit = buffer.readUtf(32);
            double lastAmendmentConcentration = buffer.readDouble();
            boolean lastAmendmentOverApplied = buffer.readBoolean();
            int pendingAmendmentCount = buffer.readVarInt();
            double pendingAmendmentDaysRemaining = buffer.readDouble();
            double recentAmendmentLoad = buffer.readDouble();
            int eventCount = Math.min(HARD_MAX_EVENTS_PER_CELL, buffer.readVarInt());
            ArrayList<String> events = new ArrayList<>(eventCount);
            for (int i = 0; i < eventCount; i++) events.add(buffer.readUtf(160));
            return new CellView(dimension, cellX, cellZ, sampleY, lastScannedDay, favorite, note, fieldId,
                tfcSoilBlock, tfcFarmlandDataPresent, tfcHydration, tfcMinHydration, tfcMaxHydration, tfcNitrogen, tfcPhosphorus, tfcPotassium,
                organicMatter, pH, drainage, compaction, structure, biology, salinity, nutrientAccess,
                amendmentApplications, cropCycles, continuousCropCycles, continuousPressure, daysBare,
                daysCovered, organicReserve, activePlantings, lastHarvestFamily, lastAmendmentId, lastAmendmentDay,
                lastAmendmentAmount, lastAmendmentUnit, lastAmendmentConcentration, lastAmendmentOverApplied,
                pendingAmendmentCount, pendingAmendmentDaysRemaining, recentAmendmentLoad, List.copyOf(events));
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(dimension, 128);
            buffer.writeVarInt(cellX);
            buffer.writeVarInt(cellZ);
            buffer.writeVarInt(sampleY);
            buffer.writeVarLong(lastScannedDay);
            buffer.writeBoolean(favorite);
            buffer.writeUtf(note, 256);
            buffer.writeVarInt(fieldId);
            buffer.writeUtf(tfcSoilBlock, 128);
            buffer.writeBoolean(tfcFarmlandDataPresent);
            buffer.writeInt(tfcHydration);
            buffer.writeInt(tfcMinHydration);
            buffer.writeInt(tfcMaxHydration);
            buffer.writeDouble(tfcNitrogen);
            buffer.writeDouble(tfcPhosphorus);
            buffer.writeDouble(tfcPotassium);
            buffer.writeDouble(organicMatter);
            buffer.writeDouble(pH);
            buffer.writeDouble(drainage);
            buffer.writeDouble(compaction);
            buffer.writeDouble(structure);
            buffer.writeDouble(biology);
            buffer.writeDouble(salinity);
            buffer.writeDouble(nutrientAccess);
            buffer.writeVarInt(amendmentApplications);
            buffer.writeVarInt(cropCycles);
            buffer.writeVarInt(continuousCropCycles);
            buffer.writeDouble(continuousPressure);
            buffer.writeVarLong(daysBare);
            buffer.writeVarLong(daysCovered);
            buffer.writeDouble(organicReserve);
            buffer.writeVarInt(activePlantings);
            buffer.writeUtf(lastHarvestFamily, 64);
            buffer.writeUtf(lastAmendmentId, 64);
            buffer.writeLong(lastAmendmentDay);
            buffer.writeDouble(lastAmendmentAmount);
            buffer.writeUtf(lastAmendmentUnit, 32);
            buffer.writeDouble(lastAmendmentConcentration);
            buffer.writeBoolean(lastAmendmentOverApplied);
            buffer.writeVarInt(pendingAmendmentCount);
            buffer.writeDouble(pendingAmendmentDaysRemaining);
            buffer.writeDouble(recentAmendmentLoad);
            buffer.writeVarInt(recentEvents.size());
            for (String event : recentEvents) buffer.writeUtf(event, 160);
        }

        public int centerX() { return cellX * 4 + 2; }
        public int centerZ() { return cellZ * 4 + 2; }
        public int chunkX() { return Math.floorDiv(centerX(), 16); }
        public int chunkZ() { return Math.floorDiv(centerZ(), 16); }
    }
}
