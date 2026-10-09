package com.foundations.soil.api.event;

import com.foundations.soil.api.SoilSnapshot;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Public event entry point plus immutable event payloads. */
public final class SoilEvents {
    private static final SoilEventBus BUS = new SoilEventBus();

    private SoilEvents() {}

    public static SoilEventBus bus() { return BUS; }

    public static <T extends SoilEvent> SoilEventBus.Subscription on(Class<T> type, Consumer<T> consumer) {
        return BUS.listen(type, consumer);
    }

    public record CellInitialized(Identifier dimension, BlockPos soilPos, SoilSnapshot snapshot) implements SoilEvent {}
    public record CellChanged(Identifier dimension, BlockPos soilPos, String reason, SoilSnapshot snapshot) implements SoilEvent {}
    public record AmendmentApplied(Identifier dimension, BlockPos soilPos, String amendmentId,
                                   double effectiveness, SoilSnapshot snapshot) implements SoilEvent {}
    /** Detailed 0.0.3a dose event; AmendmentApplied remains for v1 listeners. */
    public record AmendmentDoseApplied(Identifier dimension, BlockPos soilPos, String amendmentId,
                                       String form, double amount, String unit, double concentration,
                                       double normalizedDose, double effectiveness, boolean overApplied,
                                       String source, SoilSnapshot snapshot) implements SoilEvent {}
    public record CropPlanted(Identifier dimension, BlockPos soilPos, String family,
                              SoilSnapshot snapshot) implements SoilEvent {}
    public record CropRemoved(Identifier dimension, BlockPos soilPos, String family, boolean mature,
                              SoilSnapshot snapshot) implements SoilEvent {}
    public record DailySimulation(Identifier dimension, BlockPos soilPos, long fromDay, long toDay,
                                  SoilSnapshot snapshot) implements SoilEvent {}
    public record FieldDiscovered(UUID playerId, String dimension, int cellX, int cellZ, int centerX, int centerZ)
        implements SoilEvent {}
    public record FieldUpdated(UUID playerId, String dimension, int cellX, int cellZ, int fieldId, String action)
        implements SoilEvent {}
    /** Internal/public diagnostic event used by the read-only R8 self-test to verify dispatch isolation. */
    public record DiagnosticProbe(long nonce) implements SoilEvent {}
}
