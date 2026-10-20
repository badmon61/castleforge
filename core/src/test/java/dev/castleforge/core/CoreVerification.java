package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.CastlePlanner.*;

/** Dependency-free regression suite, runnable with Java 21+ both locally and in CI. */
public final class CoreVerification {
    private static int checks;
    private static void check(boolean ok, String name) {
        checks++;
        if (!ok) throw new AssertionError(name);
    }
    private static void rejects(Runnable task, String name) {
        boolean failed = false;
        try { task.run(); } catch (IllegalArgumentException e) { failed = true; }
        check(failed, name);
    }
    public static void main(String[] args) {
        CultureMap map = new CultureMap(42L, 1024, 7);
        check(map.spacing() == 7168, "sevenfold linear spacing");
        rejects(() -> new CultureMap(42, 0, 7), "reject zero biome scale");
        rejects(() -> new CultureMap(42, 1024, 0), "reject zero multiplier");
        rejects(() -> new CultureMap(42, 1024, 33), "reject excessive multiplier");
        Set<String> ids = new HashSet<>();
        for (int x : new int[]{-29_999_900, -7169, -7168, -1, 0, 1, 7168, 29_999_900}) {
            var a = map.at(x, -x);
            check(a.equals(new CultureMap(42, 1024, 7).at(x, -x)), "seed determinism including negatives");
            check(a.borderBlend() >= 0 && a.borderBlend() <= .5, "bounded border mix");
            ids.add(a.id());
        }
        check(ids.size() > 3, "multiple regions");
        check(!map.at(1234, 5678).id().equals(new CultureMap(43, 1024, 7).at(1234, 5678).id()), "world seeds differ");
        var frozen = map.at(-12345, 67890);
        for (int i = 0; i < 1000; i++) map.at(i * 143, -i * 37);
        check(frozen.equals(map.at(-12345, 67890)), "exploration order independence");
        var p = plan(42, 0, 0, (x,z) -> 64, map.at(0,0));
        check(p.equals(plan(42,0,0,(x,z)->64,map.at(0,0))), "plan deterministic");
        check(p.voxels().size() <= MAX_BLOCKS, "bounded block budget");
        check(p.relief() == 0 && p.floorY() == 64, "flat terrain");
        rejects(() -> plan(42,0,0,(x,z)->x < 0 ? 64 : 80,map.at(0,0)), "reject cliffs instead of burial");
        rejects(() -> plan(42,30_000_000,0,(x,z)->64,map.at(0,0)), "reject world edge overflow");
        rejects(() -> plan(42,0,0,(x,z)->Integer.MAX_VALUE,map.at(0,0)), "reject invalid terrain");
        var slope = plan(42,0,0,(x,z)->64 + Math.floorDiv(x + 24, 10),map.at(0,0));
        check(slope.relief() == 4, "gentle slope supported");
        check(slope.floorY() == 68, "court above highest sampled ground");
        Set<Integer> sizes = new HashSet<>();
        Set<Condition> conditions = new HashSet<>();
        for (int seed = 0; seed < 50; seed++) {
            Plan q = plan(seed, 0, 0, (x,z)->64, new CultureMap(seed,1024,7).at(0,0));
            sizes.add(q.voxels().size()); conditions.add(q.condition());
            Map<Pos,Material> voxels = new HashMap<>();
            int previousY = Integer.MIN_VALUE;
            for (Voxel v : q.voxels()) {
                check(voxels.put(v.pos(), v.material()) == null, "one final state per coordinate");
                check(v.pos().y() >= previousY, "bottom-up placement order");
                previousY = v.pos().y();
                check(Math.abs(v.pos().x()) <= RADIUS && Math.abs(v.pos().z()) <= RADIUS, "bounded footprint");
                check(v.pos().y() >= q.floorY(), "flat site never excavated");
            }
            check(voxels.get(new Pos(0, q.floorY() + 3, 2)) == Material.AIR, "keep doorway open");
            for (Voxel v : q.voxels()) if (v.material() == Material.LADDER_SOUTH) {
                Material backing = voxels.get(new Pos(v.pos().x(),v.pos().y(),v.pos().z()-1));
                check(backing != null && backing != Material.AIR && backing != Material.LADDER_SOUTH, "ladder support survives ruin pass");
            }
            Set<Pos> reached = reachable(voxels);
            check(reached.contains(new Pos(0,67,0)), "gate-to-keep route");
            check(reached.contains(new Pos(0,73,-4)), "keep second floor reachable");
            check(reached.stream().anyMatch(pos -> pos.y() > 78 && pos.z() < 0), "upper castle reachable");
        }
        check(sizes.size() >= 5, "multiple dimensions");
        check(conditions.size() == 3, "all preservation states");
        System.out.println("PASS: " + checks + " assertions, 50 castle seeds; determinism, terrain, routes, supports and budgets.");
    }
    private static boolean passable(Map<Pos,Material> m, Pos p) {
        Material a = m.getOrDefault(p, p.y() <= 63 ? Material.FOUNDATION : Material.AIR);
        return a == Material.AIR || a == Material.LADDER_SOUTH;
    }
    private static boolean stand(Map<Pos,Material> m, Pos p) {
        return passable(m,p) && passable(m,new Pos(p.x(),p.y()+1,p.z()))
            && (!passable(m,new Pos(p.x(),p.y()-1,p.z())) || m.get(p) == Material.LADDER_SOUTH
                || m.get(new Pos(p.x(),p.y()-1,p.z())) == Material.LADDER_SOUTH);
    }
    private static Set<Pos> reachable(Map<Pos,Material> m) {
        Pos start = new Pos(0,64,24);
        Set<Pos> seen = new HashSet<>(); ArrayDeque<Pos> todo = new ArrayDeque<>();
        seen.add(start); todo.add(start);
        while (!todo.isEmpty()) {
            Pos p = todo.remove();
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1},{0,0}}) for (int dy=-1;dy<=1;dy++) {
                if (d[0]==0 && d[1]==0 && (dy==0 || (m.get(p)!=Material.LADDER_SOUTH
                    && m.get(new Pos(p.x(),p.y()-1,p.z()))!=Material.LADDER_SOUTH))) continue;
                Pos n = new Pos(p.x()+d[0],p.y()+dy,p.z()+d[1]);
                if (Math.abs(n.x())>25 || Math.abs(n.z())>25 || n.y()<63 || n.y()>90) continue;
                if (stand(m,n) && (dy <= 0 || passable(m,new Pos(p.x(),p.y()+2,p.z()))) && seen.add(n)) todo.add(n);
            }
        }
        return seen;
    }
}
