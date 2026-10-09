package com.foundations.soil;

import static org.junit.jupiter.api.Assertions.*;
import com.foundations.soil.amendment.AmendmentDoseUnit;
import com.foundations.soil.amendment.Amendments;
import com.foundations.soil.soil.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.junit.jupiter.api.Test;

class SoilDoseTransactionTest {
    private static final SoilCellKey KEY = new SoilCellKey(2, -3);
    private static final SoilProfile PROFILE = new SoilProfile(.30, 6.2, .60, .15, .55, .40, .02);
    private static final SoilDoseTransaction.Settings SETTINGS = new SoilDoseTransaction.Settings(100, 10, .22, 1, 32, 16);
    private SoilWorldData world() {
        SoilWorldData world = new SoilWorldData();
        SoilCell cell = new SoilCell(2, -3, 64, PROFILE, 1, 10);
        cell.initializePhysical(SoilTexture.LOAM, "tfc:loam");
        cell.enableMonitoring(); cell.markPlanting(8, -12, true);
        cell.applyDose(Amendments.LIQUID_LIME, 100, AmendmentDoseUnit.MILLIBUCKET, 1, 2, 10, .22, 1, 32);
        world.put(KEY, cell);
        return world;
    }
    private SoilDoseTransaction prepare(SoilWorldData w, AtomicInteger events) {
        return new SoilDoseTransaction(w, w.get(KEY).orElseThrow(), Amendments.COMPOST_TEA, SETTINGS,
            (cell, outcome) -> events.incrementAndGet());
    }
    private void stage(SoilDoseTransaction dose, TransactionContext tx) {
        dose.stage(250, AmendmentDoseUnit.MILLIBUCKET, 1, tx);
    }
    /** A test quantity journal shares the genuine NeoForge transaction; not a replacement mod. */
    private static final class Quantity extends SnapshotJournal<Integer> {
        int value = 1000;
        void extract(int count, TransactionContext tx) { updateSnapshots(tx); value -= count; }
        protected Integer createSnapshot() { return value; }
        protected void revertToSnapshot(Integer before) { value = before; }
    }
    @Test void rootAbortRestoresBothExactCellAndContainer() {
        var w = world(); var before = w.get(KEY).orElseThrow(); var nbt = before.save();
        var events = new AtomicInteger(); var dose = prepare(w, events); var tank = new Quantity();
        try (var tx = Transaction.openRoot()) { tank.extract(250, tx); stage(dose, tx); }
        assertEquals(1000, tank.value); assertSame(before, w.get(KEY).orElseThrow());
        assertEquals(nbt, before.save()); assertFalse(dose.committed()); assertEquals(0, events.get());
        assertThrows(IllegalStateException.class, dose::publishCommitted);
    }
    @Test void successCommitsBothAndPublishesOnlyOnceAfterRootClose() {
        var w = world(); var original = w.get(KEY).orElseThrow(); var events = new AtomicInteger();
        var dose = prepare(w, events); var tank = new Quantity();
        try (var tx = Transaction.openRoot()) {
            tank.extract(250, tx); stage(dose, tx);
            assertEquals(0, events.get()); assertFalse(dose.committed());
            assertThrows(IllegalStateException.class, dose::publishCommitted);
            tx.commit();
        }
        assertEquals(750, tank.value); assertTrue(dose.committed());
        assertEquals(original.amendmentApplications() + 1, w.get(KEY).orElseThrow().amendmentApplications());
        assertTrue(dose.publishCommitted()); assertFalse(dose.publishCommitted()); assertEquals(1, events.get());
        assertEquals(original.save(), SoilCell.load(original.save()).save());
    }
    @Test void nestedCommitStillRollsBackWhenParentAborts() {
        var w = world(); var before = w.get(KEY).orElseThrow().save(); var events = new AtomicInteger();
        var dose = prepare(w, events); var tank = new Quantity();
        try (var root = Transaction.openRoot()) {
            try (var nested = Transaction.open(root)) { tank.extract(250, nested); stage(dose, nested); nested.commit(); }
            assertFalse(dose.committed());
        }
        assertEquals(before, w.get(KEY).orElseThrow().save()); assertEquals(1000, tank.value);
        assertFalse(dose.committed()); assertEquals(0, events.get());
    }
    @Test void nestedAbortDoesNotPublishEvenWhenParentCommits() {
        var w = world(); var before = w.get(KEY).orElseThrow().save(); var events = new AtomicInteger();
        var dose = prepare(w, events);
        try (var root = Transaction.openRoot()) {
            try (var nested = Transaction.open(root)) { stage(dose, nested); }
            root.commit();
        }
        assertFalse(dose.committed()); assertEquals(before, w.get(KEY).orElseThrow().save());
    }
    @Test void creativeProbeRestoresContainerButCommitsExactlyOneDose() {
        var w = world(); int count = w.get(KEY).orElseThrow().amendmentApplications();
        var dose = prepare(w, new AtomicInteger()); var tank = new Quantity();
        try (var root = Transaction.openRoot()) {
            try (var probe = Transaction.open(root)) { tank.extract(250, probe); }
            assertEquals(1000, tank.value); stage(dose, root); root.commit();
        }
        assertEquals(1000, tank.value); assertEquals(count + 1, w.get(KEY).orElseThrow().amendmentApplications());
    }
    @Test void exceptionAfterDoseStageRollsBackEverything() {
        var w = world(); var before = w.get(KEY).orElseThrow().save(); var dose = prepare(w, new AtomicInteger()); var tank = new Quantity();
        assertThrows(IllegalStateException.class, () -> {
            try (var tx = Transaction.openRoot()) { tank.extract(250, tx); stage(dose, tx); throw new IllegalStateException("transfer rejected"); }
        });
        assertEquals(before, w.get(KEY).orElseThrow().save()); assertEquals(1000, tank.value);
    }
    @Test void invalidUnitAndExcessDoseDoNotAmendTheOriginal() {
        var w = world(); var before = w.get(KEY).orElseThrow().save();
        try (var tx = Transaction.openRoot()) {
            assertThrows(IllegalArgumentException.class, () -> prepare(w, new AtomicInteger()).stage(250, AmendmentDoseUnit.APPLICATION, 1, tx));
            assertThrows(IllegalArgumentException.class, () -> prepare(w, new AtomicInteger()).stage(250, AmendmentDoseUnit.MILLIBUCKET, Double.NaN, tx));
        }
        assertEquals(before, w.get(KEY).orElseThrow().save());
    }
    @Test void staleInPlaceAndReplacedCellSnapshotsCannotBeOverwritten() {
        var w = world(); var dose = prepare(w, new AtomicInteger());
        w.get(KEY).orElseThrow().touch(101);
        try (var tx = Transaction.openRoot()) { assertThrows(IllegalStateException.class, () -> stage(dose, tx)); }
        var second = prepare(w, new AtomicInteger()); w.put(KEY, SoilCell.load(w.get(KEY).orElseThrow().save()));
        try (var tx = Transaction.openRoot()) { assertThrows(IllegalStateException.class, () -> stage(second, tx)); }
    }
    @Test void committedOrAbortedJournalCannotBeReused() {
        var w = world(); var dose = prepare(w, new AtomicInteger());
        try (var tx = Transaction.openRoot()) { stage(dose, tx); }
        try (var tx = Transaction.openRoot()) { assertThrows(IllegalStateException.class, () -> stage(dose, tx)); }
    }
    @Test void notificationFailureCannotReverseCommittedInventoryOrReapplyDose() {
        var w = world(); var calls = new AtomicInteger();
        var dose = new SoilDoseTransaction(w, w.get(KEY).orElseThrow(), Amendments.COMPOST_TEA, SETTINGS,
            (c, o) -> { calls.incrementAndGet(); throw new IllegalStateException("observer failed"); });
        try (var tx = Transaction.openRoot()) { stage(dose, tx); tx.commit(); }
        var committed = w.get(KEY).orElseThrow().save();
        assertFalse(dose.publishCommitted()); assertFalse(dose.publishCommitted());
        assertEquals(1, calls.get()); assertTrue(dose.committed()); assertEquals(committed, w.get(KEY).orElseThrow().save());
    }
    @Test void serverDoseLimitRejectsBeforePublishingSpeculativeState() {
        var w = world(); var before = w.get(KEY).orElseThrow().save();
        var settings = new SoilDoseTransaction.Settings(100, 10, .22, 1, 32, .5);
        var dose = new SoilDoseTransaction(w, w.get(KEY).orElseThrow(), Amendments.COMPOST_TEA, settings, (c,o) -> fail());
        try(var tx = Transaction.openRoot()) { assertThrows(IllegalArgumentException.class, () -> stage(dose, tx)); }
        assertEquals(before, w.get(KEY).orElseThrow().save());
    }
    @Test void sharedCellJournalHandles512NestedTwoDoseSequencesWithoutLeaking() {
        var random = new java.util.Random(2612);
        for (int i = 0; i < 512; i++) {
            var w = world(); var original = w.get(KEY).orElseThrow(); var before = original.save();
            boolean rootCommit = random.nextBoolean(), nestedCommit = random.nextBoolean();
            int first = 10 + random.nextInt(240), second = 10 + random.nextInt(240);
            var events = new AtomicInteger(); var d1 = prepare(w, events);
            SoilDoseTransaction d2;
            try (var root = Transaction.openRoot()) {
                d1.stage(first, AmendmentDoseUnit.MILLIBUCKET, 1, root);
                d2 = prepare(w, events);
                try (var nested = Transaction.open(root)) {
                    d2.stage(second, AmendmentDoseUnit.MILLIBUCKET, 1, nested);
                    if (nestedCommit) nested.commit();
                }
                assertEquals(1, w.activeDoseJournals());
                if (rootCommit) root.commit();
            }
            var expected = SoilCell.load(before.copy());
            if (rootCommit) {
                expected.applyDose(Amendments.COMPOST_TEA, first, AmendmentDoseUnit.MILLIBUCKET, 1, 100, 10, .22, 1, 32);
                if (nestedCommit) expected.applyDose(Amendments.COMPOST_TEA, second, AmendmentDoseUnit.MILLIBUCKET, 1, 100, 10, .22, 1, 32);
                assertTrue(d1.publishCommitted());
                if (nestedCommit) assertTrue(d2.publishCommitted());
            }
            assertEquals(rootCommit, d1.committed());
            assertEquals(rootCommit && nestedCommit, d2.committed());
            assertEquals(expected.save(), w.get(KEY).orElseThrow().save(), "Sequence " + i);
            assertEquals(before, original.save());
            assertEquals(0, w.activeDoseJournals());
            assertEquals(rootCommit ? nestedCommit ? 2 : 1 : 0, events.get());
        }
    }
    @Test void abortedNestedJournalIsReleasedBeforeLaterUseInTheSameRoot() {
        var w = world(); var before = w.get(KEY).orElseThrow(); var events = new AtomicInteger();
        var abandoned = prepare(w, events);
        try (var root = Transaction.openRoot()) {
            try (var nested = Transaction.open(root)) { stage(abandoned, nested); }
            assertEquals(0, w.activeDoseJournals()); assertSame(before, w.get(KEY).orElseThrow());
            var later = prepare(w, events); stage(later, root);
        }
        assertEquals(0, w.activeDoseJournals()); assertSame(before, w.get(KEY).orElseThrow());
        assertFalse(abandoned.committed()); assertEquals(0, events.get());
    }

}
