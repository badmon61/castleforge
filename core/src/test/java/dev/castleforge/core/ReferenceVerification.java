package dev.castleforge.core;

import dev.castleforge.core.architecture.ReferenceBlock;
import dev.castleforge.core.architecture.ReferenceCastlePlanner;
import dev.castleforge.core.architecture.ArchitectureModel;
import dev.castleforge.core.architecture.QualityChecks;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CancellationException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

import static dev.castleforge.core.CastlePlanner.Pos;
import static dev.castleforge.core.architecture.ReferenceBlock.Kind;
import static dev.castleforge.core.architecture.ReferenceBlock.Facing;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import static dev.castleforge.core.architecture.ReferenceCastlePlanner.*;

/** Geometry/contract tests; this does not simulate Minecraft collisions, lighting or block updates. */
public final class ReferenceVerification {
    private static int assertions, accepted, negativeScenarios;
    private static final CastlePlanner.Ground FLAT = (x,z) -> 64;
    private static final String[] STYLES = {"central_european", "nordic", "mediterranean", "desert_citadel",
        "east_asian", "gothic", "high_fantasy", "maritime"};

    private static void check(boolean pass, String message) {
        assertions++;
        if (!pass) throw new AssertionError(message);
    }
    private static void rejects(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, message);
        negativeScenarios++;
    }
    private static CultureMap.Region region(int index) {
        String style = STYLES[Math.floorMod(index, STYLES.length)];
        return new CultureMap.Region("verification-region",style,style,0,0,0,7168);
    }
    public static void main(String[] args) throws Exception {
        long started = System.nanoTime();
        Set<Integer> rotations = new HashSet<>();
        Map<Integer,Integer> rotationSeeds = new TreeMap<>();
        Set<String> signatures = new HashSet<>();
        for (int seed = 0; seed < 16; seed++) {
            Plan plan = plan(seed,0,0,FLAT,region(seed));
            accepted++;
            check(plan.equals(plan(seed,0,0,FLAT,region(seed))),"repeat planning is deterministic, including model and order");
            check(plan.region().equals(region(seed)),"source region metadata retained");
            rotations.add(plan.quarterTurns());
            rotationSeeds.putIfAbsent(plan.quarterTurns(),seed);
            signatures.add(digest(plan.blocks()));
            verifyBlocks(plan, FLAT);
            verifyModel(plan);
            verifyApproach(plan, FLAT);
        }
        check(rotations.size() == 4,"representative seeds exercise all four rotations");
        check(signatures.size() > 1,"seed affects reference output");
        for (int x : new int[]{-29_999_800,-7168,-1,29_999_800}) {
            Plan plan = plan(42,x,-x,FLAT,region(0));
            accepted++;
            check(plan.equals(plan(42,x,-x,FLAT,region(0))),"negative/world-edge position deterministic");
            verifyBlocks(plan, FLAT);
        }
        for(CastlePlanner.Ground ground:List.<CastlePlanner.Ground>of((x,z)->x<0?64:65,(x,z)->64+Math.floorMod(x,2))) for(int seed:rotationSeeds.values()) {
            Plan plan=plan(seed,0,0,ground,region(0));
            accepted++;
            check(plan.relief()==1,"small ground variation preserved in diagnostics");
            verifyBlocks(plan,ground);
            verifyModel(plan);
            verifyApproach(plan,ground);
        }
        AtomicInteger reads = new AtomicInteger();
        Plan reference = plan(42,0,0,(x,z) -> { reads.incrementAndGet(); return 64; },region(0));
        accepted++;
        check(reads.get() == (2*RADIUS+1)*(2*RADIUS+1),"each terrain column is captured once; no hidden planning resampling");
        Plan otherCulture=plan(42,0,0,FLAT,region(5));accepted++;
        check(reference.blocks().equals(otherCulture.blocks()),"reference geometry stays explicitly central-European across metadata regions");
        check(otherCulture.region().equals(region(5)),"fixed reference design does not replace culture metadata");
        rejects(() -> plan(42,29_999_999,0,FLAT,region(0)),"unsafe world-edge coordinate rejected");
        rejects(() -> plan(42,Integer.MIN_VALUE,0,FLAT,region(0)),"negative coordinate overflow rejected");
        rejects(() -> plan(42,0,Integer.MAX_VALUE,FLAT,region(0)),"positive coordinate overflow rejected");
        rejects(() -> plan(42,0,0,(x,z) -> Integer.MIN_VALUE,region(0)),"unusable terrain rejected");
        rejects(() -> plan(42,0,0,(x,z) -> Integer.MAX_VALUE,region(0)),"overflow terrain rejected");
        rejects(() -> plan(42,0,0,(x,z) -> x < 0 ? 64 : 100,region(0)),"cliff rejected rather than excavated");
        verifyMutations(reference);
        verifyArchitecturalDetails(reference);
        verifyCancellation();
        System.out.println("Reference SHA-256: " + digest(reference.blocks()));
        System.out.printf(Locale.ROOT,"PASS reference: %d accepted sites, %d rejected invalid scenarios, 1 cancellation scenario, %d invariant assertions; %.2f s.%n",
            accepted,negativeScenarios,assertions,(System.nanoTime()-started)/1e9);
    }
    private static void verifyArchitecturalDetails(Plan plan) {
        var model=plan.architecture();var local=plan.localBlocks();
        Map<Pos,ReferenceBlock> blocks=new HashMap<>();for(var b:local)blocks.put(b.pos(),b);
        check(model.partitions().size()==9,"three actual dividing walls per residential floor");
        check(model.facadeBays().size()==22,"hall and east residential elevations use declared facade bays");
        check(model.dormers().size()==2,"two explicitly joined roof lights break the long roof plane");
        check(model.courtFeatures().size()==3,"court has three declared uses, not random clutter");
        for(Room room:model.rooms())if(room.id().startsWith("chamber-")||room.id().startsWith("study-")) {
            check(room.interior().width()<=10&&room.interior().depth()<=8,"chamber has human-scale dimensions");
            check(model.circulation().contains(room.target()),"each actual chamber has its own route");
        }
        Pos wall=new Pos(25,10,-5),portal=new Pos(25,10,-8);
        rejects(()->QualityChecks.verify(model,replace(local,wall,Kind.AIR,Facing.NONE)),"removing a real dividing wall fails final validation");
        rejects(()->QualityChecks.verify(model,replace(local,portal,Kind.PLASTER,Facing.NONE)),"blocking a chamber doorway fails final validation");
        var bay=model.facadeBays().getFirst();Box a=bay.window().opening();
        Pos reveal=new Pos(a.minX()-1,a.minY(),a.minZ()-1);
        rejects(()->QualityChecks.verify(model,replace(local,reveal,Kind.AIR,Facing.NONE)),"missing projected window jamb fails final validation");
        Pos insideWindow=new Pos(a.minX(),a.minY(),a.minZ()+1);
        rejects(()->QualityChecks.verify(model,replace(local,insideWindow,Kind.BOOKSHELF,Facing.NONE)),"furniture behind glazing fails final validation");
        check(blocks.get(new Pos(-7,1,-24)).kind()==Kind.TABLE_TOP,"hall table uses a one-block-high top instead of a 2.5-block platform");
        check(!new ReferenceBlock(new Pos(0,0,0),Kind.TABLE_TOP).fullCube(),"top slab is not a full navigation support");
        for(Dormer d:model.dormers()) {
            check(blocks.get(new Pos(d.frontX(),d.baseY()+1,d.centerZ())).kind()==Kind.GLASS,"dormer front contains glazing");
            check(blocks.get(new Pos(d.frontX()-1,d.baseY()+1,d.centerZ())).kind()==Kind.AIR,"dormer glazing has an interior air pocket");
        }
        Dormer first=model.dormers().getFirst();
        rejects(()->QualityChecks.verify(model,replace(local,new Pos(first.frontX(),first.baseY()+6,first.centerZ()),Kind.AIR,Facing.NONE)),"missing dormer cap fails final validation");
    }
    private static void verifyCancellation() {
        boolean cancelled=false, interruptPreserved;
        Thread.currentThread().interrupt();
        try { plan(42,0,0,FLAT,region(0)); }
        catch(CancellationException expected) { cancelled=true; }
        finally { interruptPreserved=Thread.interrupted(); }
        check(cancelled,"preinterrupted pure planning aborts with CancellationException");
        check(interruptPreserved&&!Thread.currentThread().isInterrupted(),"caller observes interruption and test clears it in finally");
    }
    /** Sample both stair halves: a full-cube jump must not masquerade as a walkable main entrance. */
    private static void verifyApproach(Plan plan,CastlePlanner.Ground ground) {
        Map<Pos,ReferenceBlock> blocks=new HashMap<>();
        for(var block:plan.localBlocks())blocks.put(block.pos(),block);
        for(int x=-1;x<=1;x++) {
            Integer previousHalfHeight=null;
            for(int z=53;z>=40;z--) for(boolean northHalf:new boolean[]{false,true}) {
                Pos world=plan.toWorld(new Pos(x,0,z));
                int halfHeight=2*(ground.surfaceY(world.x(),world.z())-plan.floorY());
                for(int y=-MAX_RELIEF-1;y<=0;y++) {
                    var b=blocks.get(new Pos(x,y,z));
                    if(b==null||b.kind()==Kind.AIR)continue;
                    if(b.fullCube())halfHeight=Math.max(halfHeight,2*(y+1));
                    else if(b.kind()==Kind.STONE_STAIRS) {
                        if(b.facing()!=Facing.NORTH)throw new AssertionError("approach stair faces away from gate: "+b);
                        halfHeight=Math.max(halfHeight,2*y+(northHalf?2:1));
                    } else throw new AssertionError("unexpected approach obstruction: "+b);
                }
                if(previousHalfHeight!=null&&Math.abs(halfHeight-previousHalfHeight)>1)
                    throw new AssertionError("entrance requires full-block jump/drop at "+x+","+z+" half="+northHalf+
                        " surface="+previousHalfHeight+" -> "+halfHeight+" rotation="+plan.quarterTurns());
                int clearY=Math.floorDiv(halfHeight+1,2);
                for(int y=clearY;y<clearY+3;y++) {
                    var b=blocks.get(new Pos(x,y,z));
                    if(b!=null&&b.kind()!=Kind.AIR)throw new AssertionError("approach headroom obstructed: "+b);
                }
                previousHalfHeight=halfHeight;
            }
        }
        check(true,"three-wide outside-terrain route reaches gate through half-block stairs without jumping");
    }
    private static void verifyModel(Plan plan) {
        var model=plan.architecture();
        check(!model.variant().isBlank(),"versioned architecture identity");
        check(model.modules().size()>=6,"separate authored building masses");
        check(model.rooms().size()>=8,"meaningful room program");
        check(model.stairs().size()>=4,"connected multi-storey stair program");
        check(!model.connections().isEmpty(),"explicit module connections");
        Set<ModuleType> moduleTypes=new HashSet<>();
        for(var module:model.modules()) moduleTypes.add(module.type());
        check(moduleTypes.containsAll(Set.of(ModuleType.KEEP,ModuleType.GREAT_HALL,ModuleType.RESIDENCE,ModuleType.GATEHOUSE)),
            "keep, great hall, residence and gatehouse are distinct modules");
        Set<RoomType> roomTypes=new HashSet<>();
        for(var room:model.rooms()) {
            roomTypes.add(room.type());
            var module=model.module(room.moduleId());
            check(module.floors().contains(room.floorY()),"room belongs to declared storey: "+room.id());
            check(room.interior().contains(room.target()),"room access target lies inside room: "+room.id());
        }
        check(roomTypes.containsAll(Set.of(RoomType.GREAT_HALL,RoomType.KITCHEN,RoomType.BEDCHAMBER,RoomType.LIBRARY,RoomType.STORE,RoomType.GUARDROOM)),
            "functional hall, kitchen, living, study, storage and defence rooms");
        var local=plan.localBlocks();
        Map<Pos,ReferenceBlock> map=new HashMap<>();
        for(var block:local) map.put(block.pos(),block);
        Set<ReferenceBlock> worldBlocks=new HashSet<>(plan.blocks());
        for(var block:local) {
            int x=block.pos().x(),z=block.pos().z();
            for(int q=0;q<plan.quarterTurns();q++){int nextX=-z;z=x;x=nextX;}
            var expected=new ReferenceBlock(new Pos(plan.originX()+x,plan.floorY()+block.pos().y(),plan.originZ()+z),
                block.kind(),block.facing().rotate(plan.quarterTurns()));
            if(!worldBlocks.contains(expected))throw new AssertionError("position/facing rotation mismatch: "+expected);
        }
        check(local.size()==plan.blocks().size(),"every local block transforms to world coordinates and facing");
        for(Pos pos:model.clearance()) {
            var block=map.get(pos);
            if(block!=null&&block.kind()!=Kind.AIR)throw new AssertionError("furnishing obstructs reserved route: "+pos);
        }
        check(true,"all reserved route volumes remain clear after furnishing");
        var report=QualityChecks.verify(model,local);
        check(report.reachableRooms()==model.rooms().size(),"final voxel navigation reaches every declared room");
        check(report.equals(plan.report()),"stored quality report independently reproduces");
        verifyWallWalk(plan,map);
    }
    /** Independent authored circuit: no ground-level shortcut can stand in for a missing high link. */
    private static void verifyWallWalk(Plan plan,Map<Pos,ReferenceBlock> blocks){
        var model=plan.architecture();
        check(model.wallWalks().size()==11,"five curtains, four tower platforms, gate and court-stair are modeled");
        List<Pos> corners=List.of(new Pos(-47,7,-38),new Pos(47,7,-38),new Pos(47,7,38),
            new Pos(14,7,38),new Pos(13,8,38),new Pos(12,9,38),new Pos(-12,9,38),
            new Pos(-13,8,38),new Pos(-14,7,38),new Pos(-47,7,38),new Pos(-47,7,-38));
        int length=0;
        for(int i=1;i<corners.size();i++){
            Pos a=corners.get(i-1),b=corners.get(i);
            int count=Math.max(Math.abs(b.x()-a.x()),Math.abs(b.z()-a.z()));
            int dx=Integer.signum(b.x()-a.x()),dz=Integer.signum(b.z()-a.z()),dy=Integer.signum(b.y()-a.y());
            for(int n=0;n<=count;n++){
                Pos p=new Pos(a.x()+n*dx,a.y()+n*dy,a.z()+n*dz);
                if(!model.circulation().contains(p))throw new AssertionError("missing high circuit cell "+p);
                var floor=blocks.get(new Pos(p.x(),p.y()-1,p.z()));
                if(floor==null||!(floor.fullCube()||floor.stair()))throw new AssertionError("circuit floor gap "+p);
                for(int h=0;h<3;h++){
                    var air=blocks.get(new Pos(p.x(),p.y()+h,p.z()));
                    if(air==null||air.kind()!=Kind.AIR)throw new AssertionError("circuit headroom "+p);
                }
            }
            length+=count;
        }
        check(length==340,"closed elevated centreline is 340 blocks, through four towers and the upper gate");
        for(var flight:model.stairs())if(flight.id().equals("court-wall-stair")||flight.id().endsWith("gate-rise")){
            for(int lane=0;lane<3;lane++){
                Pos first=flight.treads().getFirst().get(lane),last=flight.treads().getLast().get(lane);
                Pos low=new Pos(first.x()-flight.facing().dx,first.y()-1,first.z()-flight.facing().dz);
                Pos high=new Pos(last.x()+flight.facing().dx,last.y(),last.z()+flight.facing().dz);
                if(!blocks.containsKey(low)||!blocks.get(low).fullCube()||!blocks.containsKey(high)||!blocks.get(high).fullCube())
                    throw new AssertionError("missing full-width landing "+flight.id()+" lane "+lane);
                int surface=2*(low.y()+1);
                for(var row:flight.treads()){
                    Pos p=row.get(lane);ReferenceBlock stair=blocks.get(p);
                    if(stair==null||!stair.stair()||stair.facing()!=flight.facing())throw new AssertionError("invalid stair primitive "+p);
                    for(int half:new int[]{1,2}){
                        int next=2*p.y()+half;
                        if(next-surface!=1)throw new AssertionError("full-block jump in stair "+flight.id()+" at "+p);
                        surface=next;
                    }
                }
                check(surface==2*(high.y()+1),"three-wide half-block stair surfaces meet both landings: "+flight.id());
            }
        }
    }
    private static void verifyMutations(Plan reference) {
        ArchitectureModel model=reference.architecture();
        List<ReferenceBlock> local=reference.localBlocks();
        var duplicate=new ArrayList<>(local); duplicate.add(local.getFirst());
        rejects(()->QualityChecks.verify(model,duplicate),"duplicate coordinates rejected before map deduplication");
        Pos corridor=model.clearance().iterator().next();
        rejects(()->QualityChecks.verify(model,replace(local,corridor,Kind.STONE,Facing.NONE)),"obstructed reserved route rejected");
        var lantern=local.stream().filter(b->b.kind()==Kind.LANTERN).findFirst().orElseThrow();
        Pos support=new Pos(lantern.pos().x(),lantern.pos().y()+1,lantern.pos().z());
        rejects(()->QualityChecks.verify(model,replace(local,support,Kind.AIR,Facing.NONE)),"missing hanging lamp support rejected");
        var wrongOrder=new ArrayList<>(local);wrongOrder.remove(lantern);wrongOrder.addFirst(lantern);
        rejects(()->QualityChecks.verify(model,wrongOrder),"attachment before supporting block rejected");
        var flight=model.stairs().getFirst();
        Pos tread=flight.treads().getFirst().getFirst();
        var actual=local.stream().filter(b->b.pos().equals(tread)).findFirst().orElseThrow();
        rejects(()->QualityChecks.verify(model,replace(local,tread,actual.kind(),actual.facing().rotate(2))),"backwards primary stair rejected");
        rejects(()->QualityChecks.verify(model,replace(local,tread,Kind.AIR,Facing.NONE)),"missing primary stair tread rejected");
        var window=model.windows().getFirst();
        Box opening=window.opening();
        Pos outsideWindow=new Pos(opening.minX()+window.outward().dx,opening.minY(),opening.minZ()+window.outward().dz);
        rejects(()->QualityChecks.verify(model,replace(local,outsideWindow,Kind.STONE,Facing.NONE)),"accidentally blocked exterior window rejected");
        Pos floatingRoof=new Pos(RADIUS,80,RADIUS);
        rejects(()->QualityChecks.verify(model,replace(local,floatingRoof,Kind.ROOF,Facing.NONE)),"unanchored roof component rejected");
        var blockedRoom=model.rooms().getFirst();
        rejects(()->QualityChecks.verify(model,replace(local,blockedRoom.target(),Kind.BOOKSHELF,Facing.NONE)),"furniture cannot block room access target");
        var modules=new ArrayList<>(model.modules());modules.add(modules.getFirst());
        ArchitectureModel duplicatedModule=copyModel(model,modules,model.rooms(),model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.verify(duplicatedModule,local),"duplicate module id rejected");
        var first=model.modules().getFirst();
        modules=new ArrayList<>(model.modules());
        modules.add(new ArchitectureModel.Module("intruding-module",first.type(),first.bounds(),first.floors(),first.roof(),first.chamfer()));
        ArchitectureModel overlap=copyModel(model,modules,model.rooms(),model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.verify(overlap,local),"distinct modules cannot silently own the same volume");
        rejects(()->new Box(1,0,0,0,1,1),"inverted volume rejected by constructor");
        modules=new ArrayList<>(model.modules());
        modules.set(0,new ArchitectureModel.Module(first.id(),first.type(),first.bounds(),List.of(first.bounds().maxY()+1),first.roof(),first.chamfer()));
        ArchitectureModel invalidFloor=copyModel(model,modules,model.rooms(),model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.verify(invalidFloor,local),"storey outside module bounds rejected");
        modules=new ArrayList<>(model.modules());
        modules.set(0,new ArchitectureModel.Module(first.id(),first.type(),first.bounds(),List.of(first.floors().getFirst(),first.floors().getFirst()),first.roof(),first.chamfer()));
        ArchitectureModel duplicateFloors=copyModel(model,modules,model.rooms(),model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.validateModel(duplicateFloors),"duplicate storey heights rejected");
        modules=new ArrayList<>(model.modules());
        modules.set(0,new ArchitectureModel.Module(first.id(),first.type(),first.bounds(),first.floors(),first.roof(),-1));
        ArchitectureModel invalidChamfer=copyModel(model,modules,model.rooms(),model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.validateModel(invalidChamfer),"negative tower chamfer rejected");
        var rooms=new ArrayList<>(model.rooms());
        Room firstRoom=rooms.getFirst();
        rooms.set(0,new Room(firstRoom.id(),"unknown-module",firstRoom.type(),firstRoom.interior(),firstRoom.floorY(),firstRoom.target()));
        ArchitectureModel orphan=copyModel(model,model.modules(),rooms,model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.verify(orphan,local),"room with nonexistent owner rejected");
        rooms=new ArrayList<>(model.rooms());
        rooms.add(new Room("overlapping-room",firstRoom.moduleId(),firstRoom.type(),firstRoom.interior(),firstRoom.floorY(),firstRoom.target()));
        ArchitectureModel overlappingRooms=copyModel(model,model.modules(),rooms,model.connections(),model.stairs(),model.windows());
        rejects(()->QualityChecks.verify(overlappingRooms,local),"two logical rooms cannot silently overlap");
        var connections=new ArrayList<>(model.connections());
        var connection=connections.getFirst();
        connections.set(0,new Connection(connection.id(),connection.from(),connection.from(),connection.aperture()));
        ArchitectureModel selfConnection=copyModel(model,model.modules(),model.rooms(),connections,model.stairs(),model.windows());
        rejects(()->QualityChecks.validateModel(selfConnection),"self-connected module port rejected");
        WallWalk wall=model.wallWalks().getFirst();
        Pos deck=wall.deck().iterator().next(),guard=wall.guards().iterator().next();
        rejects(()->QualityChecks.verify(model,replace(local,deck,Kind.AIR,Facing.NONE)),"missing wall-walk deck rejected");
        rejects(()->QualityChecks.verify(model,replace(local,guard,Kind.AIR,Facing.NONE)),"removed parapet rejected");
        for(String id:List.of("court-wall-stair","west-gate-rise","east-gate-rise")){
            StairFlight s=model.stairs().stream().filter(f->f.id().equals(id)).findFirst().orElseThrow();
            Pos p=s.treads().getFirst().getFirst();
            rejects(()->QualityChecks.verify(model,replace(local,p,Kind.STONE_STAIRS,s.facing().rotate(2))),"wrong new stair facing rejected: "+id);
        }
        rejects(()->QualityChecks.verify(model,replace(local,new Pos(-40,7,-38),Kind.STONE,Facing.NONE)),"blocked tower crossing rejected");
        rejects(()->QualityChecks.verify(model,replace(local,new Pos(0,9,38),Kind.STONE,Facing.NONE)),"blocked elevated gate passage rejected");
    }
    private static ArchitectureModel copyModel(ArchitectureModel model,List<ArchitectureModel.Module> modules,List<Room> rooms,
            List<Connection> connections,List<StairFlight> stairs,List<Window> windows) {
        return new ArchitectureModel(model.variant(),modules,rooms,connections,stairs,windows,
            model.clearance(),model.circulation(),model.entrance(),model.partitions(),model.facadeBays(),model.courtFeatures(),model.dormers(),model.wallWalks());
    }
    private static List<ReferenceBlock> replace(List<ReferenceBlock> input,Pos pos,Kind kind,Facing facing) {
        var output=new ArrayList<ReferenceBlock>(input.size()+1);
        boolean found=false;
        for(var block:input) {
            if(block.pos().equals(pos)) { output.add(new ReferenceBlock(pos,kind,facing));found=true; }
            else output.add(block);
        }
        if(!found)output.add(new ReferenceBlock(pos,kind,facing));
        return List.copyOf(output);
    }
    private static void verifyBlocks(Plan plan, CastlePlanner.Ground ground) {
        check(!plan.blocks().isEmpty() && plan.blocks().size() <= MAX_BLOCKS,"nonempty bounded write set");
        Map<Pos,ReferenceBlock> blocks = new HashMap<>();
        Map<Pos,Integer> order = new HashMap<>();
        for (int i=0; i<plan.blocks().size(); i++) {
            var block=plan.blocks().get(i);
            if (blocks.put(block.pos(),block) != null) throw new AssertionError("duplicate block coordinate: " + block.pos());
            order.put(block.pos(),i);
            if (Math.abs((long)block.pos().x()-plan.originX())>RADIUS || Math.abs((long)block.pos().z()-plan.originZ())>RADIUS)
                throw new AssertionError("outside survey bounds: "+block.pos());
            if (block.pos().y() < ground.surfaceY(block.pos().x(),block.pos().z()))
                throw new AssertionError("terrain excavation at " + block.pos());
        }
        check(blocks.size() == plan.blocks().size(),"all coordinates unique and within surveyed non-excavating envelope");
        long lights=0,stairs=0,books=0;
        for (var block:plan.blocks()) {
            if (block.kind()==Kind.LANTERN) {
                lights++;
                Pos above=new Pos(block.pos().x(),block.pos().y()+1,block.pos().z());
                ReferenceBlock support=blocks.get(above);
                if (support==null || support.kind()==Kind.AIR || support.kind()==Kind.LANTERN)
                    throw new AssertionError("unsupported hanging lantern: "+block.pos());
                if (order.get(above)>=order.get(block.pos())) throw new AssertionError("lantern before support: "+block.pos());
            }
            if (block.kind()==Kind.STONE_STAIRS || block.kind()==Kind.WOOD_STAIRS) stairs++;
            if (block.kind()==Kind.BOOKSHELF) books++;
        }
        check(lights>=6,"functional spaces contain supported lighting");
        check(stairs>=20,"substantial actual stair geometry");
        check(books>0,"furnished study/library");
    }
    private static String digest(List<ReferenceBlock> blocks) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for (var block:blocks) digest.update((block.toString()+"\n").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest.digest());
    }
    private ReferenceVerification() {}
}
