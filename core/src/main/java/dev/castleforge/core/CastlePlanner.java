package dev.castleforge.core;

import java.util.*;

/** Small, deterministic architecture laboratory. Not a finished world-generation engine. */
public final class CastlePlanner {
    public static final int VERSION = 1;
    public static final int RADIUS = 24;
    public static final int MAX_BLOCKS = 100_000;
    public enum Material { AIR, FOUNDATION, MASONRY, WEATHERED, FLOOR, ROOF, GLASS, LIGHT, LADDER_SOUTH }
    public enum Condition { RESTORED, WEATHERED, RUINED }
    public record Pos(int x, int y, int z) {}
    public record Voxel(Pos pos, Material material) {}
    public record Plan(List<Voxel> voxels, int floorY, int relief, int keepHeight,
                       Condition condition, CultureMap.Region region, int originX, int originZ) {
        public Plan { voxels = List.copyOf(voxels); }
    }
    /** Returns the first free Y above the solid, dry ground; adapter must validate ground and chunks. */
    @FunctionalInterface public interface Ground { int surfaceY(int x, int z); }

    public static Plan plan(long seed, int x, int z, Ground ground, CultureMap.Region region) {
        Objects.requireNonNull(ground); Objects.requireNonNull(region);
        if (Math.abs((long) x) > 29_999_900 || Math.abs((long) z) > 29_999_900)
            throw new IllegalArgumentException("Site outside supported world coordinates");
        int[][] heights = new int[RADIUS * 2 + 1][RADIUS * 2 + 1];
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            int h = ground.surfaceY(x + dx, z + dz);
            if (h < -1024 || h > 4096) throw new IllegalArgumentException("Unsupported ground height");
            heights[dx + RADIUS][dz + RADIUS] = h;
            min = Math.min(min, h); max = Math.max(max, h);
        }
        if (max - min > 6) throw new IllegalArgumentException("Terrain too steep for prototype (maximum relief 6 blocks)");
        // Put the entrance on its local ground and raise the court above the highest sample.
        int floor = max;
        long siteSeed = CultureMap.hash(seed ^ 0x504c414e31L, x, z);
        int keepHeight = 13 + Math.floorMod(siteSeed, 5);
        Condition condition = Condition.values()[Math.floorMod(CultureMap.mix(siteSeed), 3)];
        Builder b = new Builder(siteSeed, x, z, floor, condition);
        int extent = 17 + Math.floorMod(CultureMap.mix(siteSeed ^ 31), 3);
        // Supported court. Foundations stop at the sampled ground; no mountain excavation.
        for (int dx = -extent; dx <= extent; dx++) for (int dz = -extent; dz <= extent; dz++) {
            int groundY = heights[dx + RADIUS][dz + RADIUS];
            for (int y = groundY; y < floor; y++) b.put(dx, y - floor, dz, Material.FOUNDATION);
            b.put(dx, 0, dz, Material.FLOOR);
            for (int y = 1; y <= keepHeight + 5; y++) b.put(dx, y, dz, Material.AIR);
        }
        // Curtain wall with a clear 3-wide south gate and usable wall walk.
        for (int n = -extent; n <= extent; n++) for (int t = 0; t < 3; t++) {
            for (int y = 1; y <= 5; y++) {
                b.stone(n, y, -extent + t);
                if (Math.abs(n) > 1 || y >= 5) b.stone(n, y, extent - t);
                b.stone(-extent + t, y, n); b.stone(extent - t, y, n);
            }
            if (t == 0 && Math.floorMod(n, 2) == 0) {
                b.stone(n, 6, -extent); b.stone(n, 6, extent);
                b.stone(-extent, 6, n); b.stone(extent, 6, n);
            }
        }
        // Towers are deliberately simple in this milestone; style-specific silhouettes are next.
        for (int tx : new int[]{-extent + 3, extent - 3}) for (int tz : new int[]{-extent + 3, extent - 3}) {
            b.tower(tx, tz, 9 + Math.floorMod(CultureMap.hash(siteSeed, tx, tz), 4));
        }
        // A raised keep, two usable floors, windows, door and a supported internal ladder.
        int keepZ = -4;
        b.box(-6, 1, keepZ - 6, 6, 2, keepZ + 6, Material.FOUNDATION);
        for (int dx = -6; dx <= 6; dx++) for (int dz = keepZ - 6; dz <= keepZ + 6; dz++) {
            for (int y = 3; y <= keepHeight; y++) {
                if (Math.abs(dx) == 6 || Math.abs(dz - keepZ) == 6) b.stone(dx, y, dz);
                else if (y == 8) b.put(dx, y, dz, Material.FLOOR);
            }
            b.put(dx, keepHeight + 1, dz, Material.ROOF);
        }
        // Front entrance at y=3, with broad exterior steps. Gate -> court -> keep stays connected.
        b.box(-1, 3, keepZ + 6, 1, 5, keepZ + 6, Material.AIR);
        b.box(-2, 1, keepZ + 7, 2, 1, keepZ + 9, Material.FLOOR);
        b.box(-2, 2, keepZ + 7, 2, 2, keepZ + 8, Material.FLOOR);
        for (int y = 3; y <= keepHeight + 1; y++) {
            b.put(0, y, keepZ - 6, Material.MASONRY);
            b.put(0, y, keepZ - 5, Material.LADDER_SOUTH);
        }
        for (int y : new int[]{5, 11}) for (int w : new int[]{-3, 3}) {
            b.put(w, y, keepZ - 6, Material.GLASS); b.put(w, y + 1, keepZ - 6, Material.GLASS);
            b.put(-6, y, keepZ + w, Material.GLASS); b.put(6, y, keepZ + w, Material.GLASS);
        }
        b.put(3, 3, keepZ + 3, Material.LIGHT); b.put(3, 9, keepZ + 3, Material.LIGHT);
        // Exterior entrance ramp: each floor step is at most one block high, with foundations.
        int previous = floor;
        for (int dz = extent + 1; dz <= RADIUS; dz++) {
            int localMax = Integer.MIN_VALUE;
            for (int dx = -1; dx <= 1; dx++) localMax = Math.max(localMax, heights[dx + RADIUS][dz + RADIUS] - 1);
            int step = Math.max(localMax, previous - 1);
            for (int dx = -1; dx <= 1; dx++) {
                int groundY = heights[dx + RADIUS][dz + RADIUS];
                for (int y = groundY; y <= step; y++) b.put(dx, y - floor, dz, Material.FOUNDATION);
                for (int y = step + 1; y <= Math.max(floor + 4, step + 3); y++) b.put(dx, y - floor, dz, Material.AIR);
            }
            previous = step;
        }
        int exitGround = heights[RADIUS][2 * RADIUS] - 1;
        if (previous - exitGround > 1) throw new IllegalArgumentException("Entrance ramp cannot safely reach terrain");
        return new Plan(b.finish(), floor, max - min, keepHeight, condition, region, x, z);
    }

    private static final class Builder {
        private final LinkedHashMap<Pos, Material> blocks = new LinkedHashMap<>();
        private final long seed;
        private final int ox, oz, base;
        private final Condition condition;
        Builder(long seed, int x, int z, int base, Condition condition) {
            this.seed = seed; ox = x; oz = z; this.base = base; this.condition = condition;
        }
        void put(int x, int y, int z, Material material) {
            blocks.put(new Pos(ox + x, base + y, oz + z), material);
            if (blocks.size() > MAX_BLOCKS) throw new IllegalStateException("Prototype block budget exceeded");
        }
        void stone(int x, int y, int z) {
            long h = CultureMap.hash(seed ^ y, x, z);
            // Remove only upper masonry: doors, floors, ladders and load-bearing bases survive.
            if (condition == Condition.RUINED && y >= 7 && Math.floorMod(h, 100) < 22) {
                put(x, y, z, Material.AIR); return;
            }
            int weather = condition == Condition.RESTORED ? 5 : condition == Condition.WEATHERED ? 30 : 55;
            put(x, y, z, Math.floorMod(h, 100) < weather ? Material.WEATHERED : Material.MASONRY);
        }
        void box(int x1, int y1, int z1, int x2, int y2, int z2, Material m) {
            for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = y1; y <= y2; y++) put(x, y, z, m);
        }
        void tower(int tx, int tz, int h) {
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
                for (int y = 1; y <= h; y++) {
                    if (Math.abs(x) == 3 || Math.abs(z) == 3) stone(tx + x, y, tz + z);
                    else put(tx + x, y, tz + z, Material.AIR);
                }
                put(tx + x, h + 1, tz + z, Material.ROOF);
                if ((Math.abs(x) == 3 || Math.abs(z) == 3) && Math.floorMod(x + z, 2) == 0)
                    stone(tx + x, h + 2, tz + z);
            }
            // Doors toward the court; intermediate floor connects the tower to the wall walk.
            int inward = tz < 0 ? 3 : -3;
            box(tx + 1, 1, tz + inward, tx + 2, 3, tz + inward, Material.AIR);
            box(tx - 2, 5, tz - 2, tx + 2, 5, tz + 2, Material.FLOOR);
            int side = tx < 0 ? 3 : -3;
            int wallZ = tz + (tz < 0 ? -2 : 2);
            box(tx + side, 6, wallZ, tx + side, 8, wallZ, Material.AIR);
            // Intact backing column, including in ruins. No unsupported ladders.
            for (int y = 1; y <= h + 1; y++) {
                put(tx, y, tz - 3, Material.MASONRY);
                put(tx, y, tz - 2, Material.LADDER_SOUTH);
            }
        }
        List<Voxel> finish() {
            return blocks.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Pos, Material>>comparingInt(e -> e.getKey().y())
                    .thenComparingInt(e -> e.getKey().z()).thenComparingInt(e -> e.getKey().x()))
                .map(e -> new Voxel(e.getKey(), e.getValue())).toList();
        }
    }
    private CastlePlanner() {}
}
