package com.foundations.soil.diagnostic;

import com.foundations.soil.api.FoundationsSoilApi;
import com.foundations.soil.api.event.SoilEvents;
import com.foundations.soil.api.input.SoilInputHooks;
import com.foundations.soil.api.input.SoilInputType;
import com.foundations.soil.amendment.AmendmentRegistry;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.crop.CropRegistry;
import com.foundations.soil.definition.AgronomyDefinitionLoader;
import com.foundations.soil.integration.FrameworkSoilBridge;
import com.foundations.soil.ledger.FieldLedgerData;
import com.foundations.soil.ledger.FieldLedgerManager;
import com.foundations.soil.registry.SoilFluids;
import com.foundations.soil.soil.SoilCell;
import com.foundations.soil.soil.SoilManager;
import com.foundations.soil.soil.SoilProfile;
import com.foundations.soil.soil.SoilProfileRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import com.foundations.soil.integration.TfcRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Read-only world self-test for the 0.0.3a amendment engine. */
public final class SoilSelfTest {
    private SoilSelfTest() {}

    public static Result run(ServerPlayer player) {
        List<Check> checks = new ArrayList<>();

        check(checks, "Storage", () -> {
            int cells = SoilManager.loadedCellCount(player.level());
            if (cells < 0) throw new IllegalStateException("negative cell count");
            return cells + " persistent cell(s)";
        });

        check(checks, "Schema 3/3", () -> {
            if (SoilCell.SCHEMA_VERSION != 3 || SoilCell.MODEL_VERSION != 3) {
                throw new IllegalStateException("unexpected schema/model " + SoilCell.SCHEMA_VERSION + "/" + SoilCell.MODEL_VERSION);
            }
            SoilCell sample = new SoilCell(2, -3, 64, new SoilProfile(.31, 6.57, .62, .13, .60, .48, .03), 42L, 7L);
            SoilCell copy = SoilCell.load(sample.save());
            if (copy.cellX() != 2 || copy.cellZ() != -3 || Math.abs(copy.profile().pH() - 6.57) > 0.00001) {
                throw new IllegalStateException("in-memory round-trip mismatch");
            }
            return "round-trip OK";
        });

        check(checks, "TFC Calendar", () -> {
            if (!ModList.get().isLoaded("tfc")) throw new IllegalStateException("TFC not loaded");
            return "day=" + TfcRuntime.days();
        });

        check(checks, "Definitions", () -> {
            var report = AgronomyDefinitionLoader.lastReport();
            if (report.errors() > 0) throw new IllegalStateException(report.errors() + " rejected definition(s)");
            if (AmendmentRegistry.size() <= 0 || CropRegistry.familyCount() <= 0 || SoilProfileRegistry.size() <= 0) {
                throw new IllegalStateException("one or more registries are empty");
            }
            return "amendments=" + AmendmentRegistry.size() + ", families=" + CropRegistry.familyCount()
                + ", soils=" + SoilProfileRegistry.size();
        });

        check(checks, "Amendment Engine", () -> {
            long liquids = AmendmentRegistry.all().stream().filter(a -> a.form().isLiquid()).count();
            long solids = AmendmentRegistry.size() - liquids;
            if (liquids <= 0 || solids <= 0) throw new IllegalStateException("solid/liquid definitions missing");
            SoilCell sample = new SoilCell(0, 0, 64, new SoilProfile(.30, 6.5, .60, .12, .58, .42, .02), 10L, 10L);
            var outcome = sample.applyDose(Amendments.COMPOST_TEA, 250.0, Amendments.COMPOST_TEA.doseUnit(), 1.0,
                11L, 10L, .22, 1.0, 32);
            if (Math.abs(outcome.normalizedDose() - 1.0) > 0.000001 || sample.pendingAmendmentCount() <= 0) {
                throw new IllegalStateException("dose normalization/delayed release failed");
            }
            SoilCell copy = SoilCell.load(sample.save());
            if (copy.pendingAmendmentCount() != sample.pendingAmendmentCount()) {
                throw new IllegalStateException("pending amendment persistence failed");
            }
            return solids + " solid / " + liquids + " liquid; delayed persistence OK";
        });

        check(checks, "Physical Amendment Fluids", () -> {
            if (SoilFluids.all().size() != 5) throw new IllegalStateException("expected 5 physical amendment fluids");
            for (var family : SoilFluids.all()) {
                if (AmendmentRegistry.get(family.amendmentId()).isEmpty()) {
                    throw new IllegalStateException("fluid " + family.name() + " has no amendment definition " + family.amendmentId());
                }
                if (family.source().get() == null || family.bucket().get() == null) {
                    throw new IllegalStateException("fluid family not registered: " + family.name());
                }
            }
            return "5 fluids registered; barrel/container bridge READY";
        });

        check(checks, "Framework Module", () -> {
            FrameworkSoilBridge.bootstrap();
            if (FrameworkSoilBridge.status() != FrameworkSoilBridge.Status.ACTIVE) {
                throw new IllegalStateException(FrameworkSoilBridge.status() + " — " + FrameworkSoilBridge.detail());
            }
            return FrameworkSoilBridge.detail();
        });

        check(checks, "Framework Services", () -> {
            var services = FrameworkSoilBridge.services();
            if (services.size() < 3 || services.values().stream().anyMatch(v -> !v)) {
                throw new IllegalStateException("services=" + services);
            }
            return services.keySet().toString();
        });

        check(checks, "Event Dispatch", () -> {
            AtomicBoolean delivered = new AtomicBoolean(false);
            var subscription = SoilEvents.on(SoilEvents.DiagnosticProbe.class, event -> delivered.set(true));
            try {
                SoilEvents.bus().post(new SoilEvents.DiagnosticProbe(System.nanoTime()));
            } finally {
                subscription.close();
            }
            if (!delivered.get()) throw new IllegalStateException("diagnostic probe was not delivered");
            if (SoilEvents.bus().failureCount() > 0) {
                return "dispatch OK; listenerFailures=" + SoilEvents.bus().failureCount();
            }
            return "dispatch OK";
        });

        check(checks, "Field Ledger", () -> {
            int cells = FieldLedgerManager.entryCount(player);
            int fields = FieldLedgerManager.fieldCount(player);
            if (cells < 0 || fields < 0) throw new IllegalStateException("negative ledger count");
            if (FieldLedgerData.SCHEMA_VERSION != 4) throw new IllegalStateException("unexpected Field Ledger schema " + FieldLedgerData.SCHEMA_VERSION);
            return cells + " discovered / " + fields + " field(s); schema=" + FieldLedgerData.SCHEMA_VERSION;
        });

        check(checks, "Script Gateway", () -> {
            if (FoundationsSoilApi.scripting() == null) throw new IllegalStateException("gateway unavailable");
            return ModList.get().isLoaded("kubejs") ? "ready; KubeJS present" : "ready; KubeJS absent";
        });

        check(checks, "Server Authority", () -> {
            if (!player.level().getServer().isSameThread()) throw new IllegalStateException("not on server thread");
            return "authoritative thread OK";
        });

        check(checks, "Amendment Delivery Hooks", () -> {
            for (SoilInputType type : SoilInputType.values()) {
                if (!SoilInputHooks.counts().containsKey(type)) throw new IllegalStateException("missing " + type);
            }
            if (SoilInputHooks.handlerCount(SoilInputType.SOLID_AMENDMENT) < 1
                || SoilInputHooks.handlerCount(SoilInputType.LIQUID_AMENDMENT) < 1
                || SoilInputHooks.handlerCount(SoilInputType.FERTIGATION) < 1) {
                throw new IllegalStateException("built-in amendment delivery bridge not registered");
            }
            return "solid/liquid/fertigation handlers READY; total=" + SoilInputHooks.handlerCount();
        });

        int failures = (int) checks.stream().filter(c -> c.status() == Status.FAIL).count();
        int warnings = (int) checks.stream().filter(c -> c.status() == Status.WARN).count();
        return new Result(List.copyOf(checks), failures, warnings);
    }

    private static void check(List<Check> checks, String name, Probe probe) {
        try {
            String detail = probe.run();
            checks.add(new Check(name, Status.PASS, detail == null ? "" : detail));
        } catch (Throwable error) {
            String message = error.getMessage();
            checks.add(new Check(name, Status.FAIL,
                message == null || message.isBlank() ? error.getClass().getSimpleName() : message));
        }
    }

    @FunctionalInterface
    private interface Probe { String run() throws Exception; }

    public enum Status { PASS, WARN, FAIL }
    public record Check(String name, Status status, String detail) {}
    public record Result(List<Check> checks, int failures, int warnings) {
        public boolean passed() { return failures == 0; }
    }
}
