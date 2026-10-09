package com.foundations.soil.amendment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Persisted direct-effect residue that is released across TFC calendar days. */
public record PendingAmendmentEffect(String amendmentId, double remainingDays, AmendmentEffect remainingEffect) {
    public PendingAmendmentEffect {
        amendmentId = amendmentId == null ? "unknown" : amendmentId;
        if (!Double.isFinite(remainingDays) || remainingDays < 0) throw new IllegalArgumentException("remainingDays must be finite and >= 0");
        if (remainingEffect == null) throw new IllegalArgumentException("remainingEffect cannot be null");
    }

    public Release release(double elapsedDays) {
        if (!Double.isFinite(elapsedDays) || elapsedDays <= 0 || remainingDays <= 0 || remainingEffect.effectivelyZero()) {
            return new Release(new AmendmentEffect(0, 0, 0, 0, 0, 0, 0), this, false);
        }
        double fraction = Math.min(1.0, elapsedDays / remainingDays);
        AmendmentEffect released = remainingEffect.scale(fraction);
        AmendmentEffect left = remainingEffect.scale(1.0 - fraction);
        double daysLeft = Math.max(0.0, remainingDays - elapsedDays);
        boolean completed = fraction >= 1.0 - 1.0e-12 || left.effectivelyZero() || daysLeft <= 1.0e-9;
        return new Release(released, completed ? null : new PendingAmendmentEffect(amendmentId, daysLeft, left), completed);
    }

    public PendingAmendmentEffect merge(PendingAmendmentEffect other) {
        if (other == null) return this;
        String id = amendmentId.equals(other.amendmentId) ? amendmentId : "mixed";
        return new PendingAmendmentEffect(id, Math.max(remainingDays, other.remainingDays), remainingEffect.plus(other.remainingEffect));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", amendmentId);
        tag.putDouble("remaining_days", remainingDays);
        tag.putDouble("organic_matter", remainingEffect.organicMatter());
        tag.putDouble("ph", remainingEffect.pH());
        tag.putDouble("drainage", remainingEffect.drainage());
        tag.putDouble("compaction", remainingEffect.compaction());
        tag.putDouble("structure", remainingEffect.structure());
        tag.putDouble("biology", remainingEffect.biology());
        tag.putDouble("salinity", remainingEffect.salinity());
        return tag;
    }

    public static PendingAmendmentEffect load(CompoundTag tag) {
        String id = com.foundations.soil.storage.NbtAccess.contains(tag, "id", Tag.TAG_STRING) ? tag.getString("id").orElse("") : "legacy";
        double days = Math.max(0.0, tag.getDouble("remaining_days").orElse(0.0));
        return new PendingAmendmentEffect(id, days, new AmendmentEffect(
            tag.getDouble("organic_matter").orElse(0.0), tag.getDouble("ph").orElse(0.0), tag.getDouble("drainage").orElse(0.0),
            tag.getDouble("compaction").orElse(0.0), tag.getDouble("structure").orElse(0.0), tag.getDouble("biology").orElse(0.0), tag.getDouble("salinity").orElse(0.0)));
    }

    public record Release(AmendmentEffect released, PendingAmendmentEffect remaining, boolean completed) {}
}
