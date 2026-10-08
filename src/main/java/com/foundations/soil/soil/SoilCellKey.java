package com.foundations.soil.soil;

import net.minecraft.core.BlockPos;

public record SoilCellKey(int cellX, int cellZ) {
    public static final int CELL_SIZE = 4;

    public static SoilCellKey from(BlockPos pos) {
        return new SoilCellKey(Math.floorDiv(pos.getX(), CELL_SIZE), Math.floorDiv(pos.getZ(), CELL_SIZE));
    }

    public long packed() {
        return ((long) cellX << 32) ^ (cellZ & 0xffffffffL);
    }

    public static SoilCellKey unpack(long packed) {
        return new SoilCellKey((int) (packed >> 32), (int) packed);
    }
}
