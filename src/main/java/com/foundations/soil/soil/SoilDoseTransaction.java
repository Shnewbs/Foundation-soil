package com.foundations.soil.soil;

import com.foundations.soil.FoundationsSoil;
import com.foundations.soil.amendment.AmendmentApplicationOutcome;
import com.foundations.soil.amendment.AmendmentDefinition;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * One-shot amendment journal participating in the same native transaction as a container.
 * Prepare after normal calendar catch-up, before opening the transfer. Calendar catch-up is
 * not part of the dose. Only the private candidate is amended; abort restores the exact
 * original cell object, including pending effects, history, occupancy and monitoring.
 * Notifications are explicitly deferred until the root transfer has closed successfully.
 * This is in-memory transaction safety, not crash-atomic saving of inventory and world files.
 */
public final class SoilDoseTransaction {
    public record Settings(long tick, long day, double diminishing, double strength,
                           int maxPending, double maxDose) {
        public Settings {
            if (tick < 0 || day < 0 || !Double.isFinite(diminishing) || diminishing < 0
                || !Double.isFinite(strength) || strength < 0 || maxPending < 1
                || !Double.isFinite(maxDose) || maxDose <= 0)
                throw new IllegalArgumentException("Invalid transactional amendment settings");
        }
    }
    private final SoilWorldData world;
    private final SoilCellKey key;
    private final SoilCell baseline;
    private final CompoundTag before;
    private final AmendmentDefinition amendment;
    private final Settings settings;
    private final BiConsumer<SoilCell, AmendmentApplicationOutcome> notification;
    private boolean used, committed, notified;
    private SoilCell candidate;
    private CellJournal journal;
    private AmendmentApplicationOutcome outcome;

    public SoilDoseTransaction(SoilWorldData world, SoilCell baseline, AmendmentDefinition amendment,
                              Settings settings, BiConsumer<SoilCell, AmendmentApplicationOutcome> notification) {
        this.world = Objects.requireNonNull(world);
        this.baseline = Objects.requireNonNull(baseline);
        this.amendment = Objects.requireNonNull(amendment);
        this.settings = Objects.requireNonNull(settings);
        this.notification = Objects.requireNonNull(notification);
        key = new SoilCellKey(baseline.cellX(), baseline.cellZ());
        if (world.get(key).orElse(null) != baseline) throw new IllegalArgumentException("Soil cell is not the current world record");
        before = baseline.save();
    }

    public AmendmentApplicationOutcome stage(double amount, AmendmentDoseUnit unit, double concentration,
                                             TransactionContext transaction) {
        if (used) throw new IllegalStateException("Prepare a new dose for each transfer; journals cannot be reused");
        used = true;
        Objects.requireNonNull(transaction, "transaction");
        if (net.neoforged.neoforge.transfer.transaction.Transaction.getCurrentOpenedTransaction() != transaction)
            throw new IllegalStateException("Dose must use the currently open native transaction");
        if (world.get(key).orElse(null) != baseline || !before.equals(baseline.save()))
            throw new IllegalStateException("Soil changed after dose preparation; retry from a fresh snapshot");
        double normalized = amendment.normalizedDose(amount, unit, concentration);
        if (normalized > settings.maxDose() + 1.0e-9)
            throw new IllegalArgumentException("Normalized amendment dose exceeds the server maximum");
        SoilCell next = SoilCell.load(before.copy());
        AmendmentApplicationOutcome applied = next.applyDose(amendment, amount, unit, concentration,
            settings.tick(), settings.day(), settings.diminishing(), settings.strength(), settings.maxPending());
        candidate = next;
        outcome = applied;
        journal = world.doseJournal(key);
        journal.stage(this, next, transaction);
        return applied;
    }

    public boolean committed() { return committed; }

    /** Idempotent publication, never during speculative/nested state or after abort. */
    public boolean publishCommitted() {
        if (!committed || journal == null || journal.isInTransaction()) throw new IllegalStateException("The root soil transfer has not committed");
        if (notified) return false;
        notified = true;
        try {
            notification.accept(candidate, outcome);
            return true;
        } catch (RuntimeException failure) {
            FoundationsSoil.LOGGER.error("[Foundations Soil] Dose committed, but its notification/ledger refresh failed; do not reapply the dose", failure);
            return false;
        }
    }

    /** One resource journal per world/cell: separate dose journals have unsafe rollback order. */
    static final class CellJournal extends SnapshotJournal<CellJournal.State> {
        record State(SoilCell cell, java.util.List<SoilDoseTransaction> pending) {}
        private final SoilWorldData world;
        private final SoilCellKey key;
        private final java.util.ArrayList<SoilDoseTransaction> pending = new java.util.ArrayList<>();
        private int retainedSnapshots;
        CellJournal(SoilWorldData world, SoilCellKey key) { this.world = world; this.key = key; }
        void stage(SoilDoseTransaction dose, SoilCell next, TransactionContext tx) {
            updateSnapshots(tx);
            world.put(key, next);
            pending.add(dose);
        }
        @Override protected State createSnapshot() {
            State snapshot = new State(world.get(key).orElseThrow(), java.util.List.copyOf(pending));
            retainedSnapshots++;
            return snapshot;
        }
        @Override protected void revertToSnapshot(State previous) {
            world.put(key, previous.cell());
            pending.clear(); pending.addAll(previous.pending());
        }
        @Override protected void onRootCommit(State original) {
            for (SoilDoseTransaction dose : pending) dose.committed = true;
            pending.clear();
        }
        @Override protected void releaseSnapshot(State released) {
            // releaseSnapshot runs before NeoForge decrements its internal counter on abort.
            // Count retained snapshots ourselves so aborted transfers release the map entry too.
            if (--retainedSnapshots == 0) {
                pending.clear();
                world.releaseDoseJournal(key, this);
            }
        }
    }

}
