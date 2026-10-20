package dev.castleforge.core;

/** Versioned, stateless geography. No chunk reads and no dependence on exploration order. */
public final class CultureMap {
    public static final int VERSION = 1;
    // Explicit IDs: never use enum ordinals as persisted/randomized identity.
    private static final String[] STYLES = {
        "central_european", "nordic", "mediterranean", "desert_citadel",
        "east_asian", "gothic", "high_fantasy", "maritime"
    };
    private final long seed;
    private final int spacing;

    public CultureMap(long seed, int biomeScale, int multiplier) {
        if (biomeScale < 128 || biomeScale > 65536 || multiplier < 1 || multiplier > 32) {
            throw new IllegalArgumentException("Biome scale must be 128..65536 and multiplier 1..32");
        }
        this.seed = seed;
        this.spacing = Math.multiplyExact(biomeScale, multiplier);
    }

    public int spacing() { return spacing; }

    public Region at(int x, int z) {
        int gx = Math.floorDiv(x, spacing), gz = Math.floorDiv(z, spacing);
        double best = Double.POSITIVE_INFINITY, second = Double.POSITIVE_INFINITY;
        long winner = 0, runnerUp = 0;
        int centerX = 0, centerZ = 0;
        // A site stays in its cell's central half. The nearest two sites are in this 5x5 window.
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            int cx = gx + dx, cz = gz + dz;
            long id = hash(seed ^ 0x434153544c454631L, cx, cz);
            int px = Math.toIntExact((long) cx * spacing + spacing / 4 + Math.floorMod(id, spacing / 2));
            int pz = Math.toIntExact((long) cz * spacing + spacing / 4 + Math.floorMod(mix(id), spacing / 2));
            double distance = Math.hypot((double) x - px, (double) z - pz);
            if (distance < best || (distance == best && Long.compareUnsigned(id, winner) < 0)) {
                second = best; runnerUp = winner;
                best = distance; winner = id; centerX = px; centerZ = pz;
            } else if (distance < second || (distance == second && Long.compareUnsigned(id, runnerUp) < 0)) {
                second = distance; runnerUp = id;
            }
        }
        // This is a blend weight, not an exact distance to the geometric boundary.
        double blend = 0.5 * Math.max(0, 1 - (second - best) / (spacing * 0.18));
        return new Region(Long.toUnsignedString(winner, 16), style(winner), style(runnerUp),
                centerX, centerZ, blend, spacing);
    }

    private static String style(long id) { return STYLES[Math.floorMod(mix(id ^ 0x5354594c45L), STYLES.length)]; }

    public static long hash(long seed, int x, int z) {
        return mix(seed ^ mix((long) x * 0x9e3779b97f4a7c15L) ^ Long.rotateLeft(mix((long) z * 0xc2b2ae3d27d4eb4fL), 29));
    }
    public static long mix(long v) {
        v = (v ^ (v >>> 30)) * 0xbf58476d1ce4e5b9L;
        v = (v ^ (v >>> 27)) * 0x94d049bb133111ebL;
        return v ^ (v >>> 31);
    }
    public record Region(String id, String style, String neighborStyle, int centerX, int centerZ,
                         double borderBlend, int nominalSpacing) {}
}
