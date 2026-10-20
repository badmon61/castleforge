package dev.castleforge.core;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static dev.castleforge.core.TerracedCastlePlanner.*;
import static dev.castleforge.core.CastlePlanner.Pos;

/** Standalone deterministic/geometry regression gate. No Minecraft runtime is simulated here. */
public final class TerracedVerification {
    private static int assertions,accepted,rejected;
    private static void check(boolean ok,String name) {
        assertions++; if(!ok) throw new AssertionError(name);
    }
    private static void rejects(Runnable action,String name) {
        boolean rejected=false; try { action.run(); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected,name);
    }
    private static final String[] STYLES={"central_european","nordic","mediterranean","desert_citadel","east_asian","gothic","high_fantasy","maritime"};
    private static CultureMap.Region region(int i) {
        return new CultureMap.Region("test",STYLES[i%STYLES.length],STYLES[(i+1)%STYLES.length],0,0,0,7168);
    }
    public static void main(String[] args) throws Exception {
        long started=System.nanoTime();
        Ground flat=(x,z)->64;
        List<Ground> terrains=List.of(flat,(x,z)->80-Math.floorDiv(z,6),(x,z)->80+Math.floorDiv(x,5),
            (x,z)->80-Math.floorDiv(z,7)-Math.floorDiv(Math.abs(x),18),
            (x,z)->Math.abs(z)>=13 && Math.abs(z)<=15?58:64);
        Set<CastlePlanner.Condition> conditions=new HashSet<>(); Set<Integer> rotations=new HashSet<>();
        Set<Roof> roofs=new HashSet<>(); Set<Integer> sizes=new HashSet<>(); int bridges=0,stairs=0;
        for(int seed=0;seed<32;seed++) for(int terrain=0;terrain<terrains.size();terrain++) {
            Ground ground=terrains.get(terrain); final Plan p;
            try { p=plan(seed,0,0,ground,region(seed)); }
            catch(IllegalArgumentException e) { rejected++; throw new AssertionError("Expected reference terrain accepted: seed="+seed+" terrain="+terrain,e); }
            accepted++;
            check(p.terraces().size()==3,"three individually planned terraces");
            check(p.rooms().size()==8,"eight architectural modules");
            check(p.links().size()==3,"two inter-court links and entrance");
            check(p.blocks().size()<MAX_BLOCKS,"bounded plan");
            check(p.targets().size()>=31,"interior and walkway targets");
            conditions.add(p.condition()); rotations.add(p.quarterTurns()); sizes.add(p.blocks().size());
            if(p.links().stream().anyMatch(Link::bridge)) bridges++;
            if(p.blocks().stream().anyMatch(b->b.kind()==Kind.STAIR)) stairs++;
            for(Room room:p.rooms()) roofs.add(room.roof());
            boolean dependencies=false; int lastY=Integer.MIN_VALUE; Set<Pos> coordinates=new HashSet<>();
            for(Block b:p.blocks()) {
                check(coordinates.add(b.pos()),"unique coordinates");
                check(Math.abs(b.pos().x())<=RADIUS && Math.abs(b.pos().z())<=RADIUS,"finite survey footprint");
                check(b.pos().y()>=ground.surfaceY(b.pos().x(),b.pos().z()),"never excavate terrain");
                if(b.kind()==Kind.LADDER) dependencies=true;
                else {
                    check(!dependencies,"all backing geometry precedes ladders");
                    check(b.pos().y()>=lastY,"bottom-up structural placement"); lastY=b.pos().y();
                }
            }
            for(Terrace t:p.terraces()) check(t.maximumFill()<=MAX_FOUNDATION,"bounded terrace foundations");
            // Planner already ran navigation; repeat on an independently supplied terrain callback.
            check(PlanNavigation.verify(p,ground).targets()==p.targets().size(),"all declared rooms and walks reachable");
            if(terrain==0) check(p.equals(plan(seed,0,0,flat,region(seed))),"same seed produces byte-equivalent plan order");
        }
        check(bridges>=32,"ravines create pier-supported bridges");
        check(stairs>=80,"slopes create directional stairs");
        check(conditions.size()==3,"all condition states");
        check(rotations.size()==4,"all orientations");
        check(roofs.size()==4,"four roof grammars");
        check(sizes.size()>50,"multiple footprints and silhouettes");

        AtomicInteger reads=new AtomicInteger();
        plan(42,0,0,(x,z)->{reads.incrementAndGet();return 64;},region(0));
        check(reads.get()==(2*RADIUS+1)*(2*RADIUS+1),"terrain read once; candidate retries only use immutable snapshot");
        rejects(()->plan(1,0,0,(x,z)->UNUSABLE,region(0)),"ocean or protected site rejected");
        rejects(()->plan(1,0,0,(x,z)->x<0?64:110,region(0)),"cliff rejected rather than buried");
        rejects(()->plan(1,0,0,(x,z)->x==0&&z==0?UNUSABLE:64,region(0)),"protected footprint rejected");
        check(!plan(1,0,0,(x,z)->x==RADIUS&&z==RADIUS?UNUSABLE:64,region(0)).blocks().isEmpty(),"unused bad terrain does not reject a valid site");
        rejects(()->plan(1,29_999_801,0,flat,region(0)),"world coordinate limit");
        rejects(()->plan(1,0,0,(x,z)->Integer.MAX_VALUE,region(0)),"invalid terrain height");
        rejects(()->plan(1,0,0,(x,z)->-1025,region(0)),"unsupported low terrain");
        for(int x:new int[]{-29_999_800,-7168,-1,29_999_800}) {
            Plan p=plan(42,x,-x,flat,region(0));
            check(p.equals(plan(42,x,-x,flat,region(0))),"negative/edge coordinate stability");
        }
        Plan reference=plan(42,0,0,flat,region(0));
        List<Block> broken=reference.blocks().stream().map(b->b.kind()==Kind.LADDER?new Block(b.pos(),Kind.AIR,b.facing()):b).toList();
        Plan disconnected=new Plan(broken,reference.terraces(),reference.rooms(),reference.links(),reference.targets(),reference.entrance(),
            reference.quarterTurns(),reference.condition(),reference.region(),0,0);
        rejects(()->PlanNavigation.verify(disconnected,flat),"missing vertical access must be rejected");
        List<Block> duplicates=new ArrayList<>(reference.blocks()); duplicates.add(duplicates.getFirst());
        Plan duplicate=new Plan(duplicates,reference.terraces(),reference.rooms(),reference.links(),reference.targets(),reference.entrance(),
            reference.quarterTurns(),reference.condition(),reference.region(),0,0);
        rejects(()->PlanNavigation.verify(duplicate,flat),"duplicate write sets rejected");
        try(ExecutorService workers=Executors.newFixedThreadPool(4)) {
            List<Future<Plan>> futures=new ArrayList<>();
            for(int i=0;i<8;i++) futures.add(workers.submit(()->plan(42,0,0,flat,region(0))));
            for(Future<Plan> future:futures) check(reference.equals(future.get()),"parallel planning has no shared RNG state");
        }
        String digest=digest(reference);
        check(digest.equals("6fd20d8855fdc190a7e81e9772cef9d4aad3974a3a405ca25f31161b643f7828"),"frozen geometry contract v1");
        System.out.println("Reference SHA-256: "+digest);
        if(args.length>0) export(reference,Path.of(args[0]));
        System.out.printf(Locale.ROOT,"PASS terraced: %d accepted reference sites, %d unexpected rejections, %d bridges, %d stair sites; %d invariant assertions; %.2f s.%n",
            accepted,rejected,bridges,stairs,assertions,(System.nanoTime()-started)/1e9);
    }
    private static String digest(Plan p) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(Block b:p.blocks()) digest.update((b.toString()+"\n").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    private static void export(Plan p,Path path) throws Exception {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try(var writer=Files.newBufferedWriter(path)) {
            writer.write("x,y,z,kind,facing\n");
            for(Block b:p.blocks()) writer.write(b.pos().x()+","+b.pos().y()+","+b.pos().z()+","+b.kind()+","+b.facing()+"\n");
        }
    }
    private TerracedVerification() {}
}
