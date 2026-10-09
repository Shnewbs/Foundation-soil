package com.foundations.soil.soil;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** At most sixteen observed TFC-day endpoints. Gaps are not fabricated as daily observations. */
public final class SoilTrend {
    public static final int CAPACITY = 16;
    public record Point(long day, SoilProfile profile, double cropPressure) {
        public Point {
            if (day < 0 || profile == null || !Double.isFinite(cropPressure) || cropPressure < 0 || cropPressure > 1)
                throw new IllegalArgumentException("Invalid soil trend sample");
        }
    }
    private final ArrayList<Point> points = new ArrayList<>(CAPACITY);
    public List<Point> points() { return List.copyOf(points); }
    public void clear() { points.clear(); }
    public void record(long day, SoilProfile profile, double cropPressure) {
        Point point = new Point(day, profile, cropPressure);
        if (!points.isEmpty()) {
            long previous = points.getLast().day();
            if (day < previous) return;
            if (day == previous) { points.set(points.size() - 1, point); return; }
        }
        if (points.size() == CAPACITY) points.removeFirst();
        points.add(point);
    }
    public ListTag save() {
        ListTag list = new ListTag();
        for (Point point : points) {
            CompoundTag tag = new CompoundTag();
            SoilProfile p = point.profile();
            tag.putLong("day", point.day());
            tag.putDouble("om", p.organicMatter()); tag.putDouble("ph", p.pH());
            tag.putDouble("dr", p.drainage()); tag.putDouble("cp", p.compaction());
            tag.putDouble("st", p.structure()); tag.putDouble("bio", p.biology());
            tag.putDouble("sal", p.salinity()); tag.putDouble("pressure", point.cropPressure());
            list.add(tag);
        }
        return list;
    }
    public void load(ListTag list) {
        if (list.size() > CAPACITY) throw new IllegalStateException("Oversized soil trend history");
        clear();
        long previous = -1;
        for (Tag value : list) {
            CompoundTag tag = (CompoundTag) value;
            long day = tag.getLong("day");
            if (day <= previous) throw new IllegalStateException("Unordered soil trend history");
            record(day, new SoilProfile(tag.getDouble("om"), tag.getDouble("ph"), tag.getDouble("dr"),
                tag.getDouble("cp"), tag.getDouble("st"), tag.getDouble("bio"), tag.getDouble("sal")), tag.getDouble("pressure"));
            previous = day;
        }
    }
}
