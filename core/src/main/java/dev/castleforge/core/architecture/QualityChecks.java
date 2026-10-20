package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import dev.castleforge.core.architecture.ArchitectureModel.*;
import dev.castleforge.core.architecture.ReferenceBlock.Facing;
import dev.castleforge.core.architecture.ReferenceBlock.Kind;
import java.util.*;

/**
 * Conservative checks on the final, furnished local voxel plan. The navigation domain is the
 * deliberately declared circulation graph: roofs and furniture cannot accidentally become shortcuts.
 * This is an architecture contract, not an emulation of Minecraft collision shapes or entity AI.
 */
public final class QualityChecks {
    private static final int MAX_CELLS = 200_000;
    private QualityChecks() {}

    public static QualityReport verify(ArchitectureModel model, List<ReferenceBlock> localBlocks) {
        Objects.requireNonNull(model, "model"); Objects.requireNonNull(localBlocks, "blocks");
        validateModel(model);
        require(!localBlocks.isEmpty() && localBlocks.size() <= MAX_CELLS, "block budget", "count=" + localBlocks.size());
        Map<Pos, ReferenceBlock> blocks = new HashMap<>();
        Map<Pos, Integer> placementIndex = new HashMap<>();
        int index = 0;
        for (ReferenceBlock b : localBlocks) {
            require(b != null, "null block", "plan");
            require(blocks.putIfAbsent(b.pos(), b) == null, "duplicate block", b.pos());
            require(!b.stair() || b.facing() != Facing.NONE, "stair facing", b.pos());
            placementIndex.put(b.pos(), index++);
        }
        for (Pos p : model.clearance()) air(blocks, p, "reserved clearance");
        for(Partition p:model.partitions()) {
            Box a=p.wall();
            for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++)for(int y=a.minY();y<=a.maxY();y++) {
                Pos pos=new Pos(x,y,z);
                if(p.portals().stream().anyMatch(portal->portal.contains(pos)))air(blocks,pos,"partition portal "+p.id());
                else {
                    ReferenceBlock block=blocks.get(pos);
                    require(block!=null&&(block.kind()==Kind.TIMBER||block.kind()==Kind.PLASTER),"missing partition "+p.id(),pos);
                }
            }
        }
        for(FacadeBay bay:model.facadeBays()) {
            Window w=bay.window();Box a=w.opening();int cx=(a.minX()+a.maxX())/2,cz=(a.minZ()+a.maxZ())/2;
            int tx=w.outward().dz==0?0:1,tz=w.outward().dx==0?0:1;
            for(int side:new int[]{-2,2}) {
                Pos p=new Pos(cx+tx*side+w.outward().dx,a.minY(),cz+tz*side+w.outward().dz);
                require(blocks.containsKey(p)&&blocks.get(p).kind()==Kind.TRIM,"missing facade reveal "+bay.id(),p);
            }
        }
        for(Dormer d:model.dormers()) {
            Pos glazing=new Pos(d.frontX(),d.baseY()+1,d.centerZ());
            require(blocks.containsKey(glazing)&&blocks.get(glazing).kind()==Kind.GLASS,"missing dormer glazing",d.id());
            air(blocks,offset(glazing,-1,0,0),"dormer interior "+d.id());
            Pos ridge=new Pos(d.frontX(),d.baseY()+6,d.centerZ());
            require(blocks.containsKey(ridge)&&blocks.get(ridge).kind()==Kind.ROOF_SLAB,"missing dormer ridge",d.id());
        }
        for (Connection c : model.connections()) {
            Box a = c.aperture();
            for (int x=a.minX(); x<=a.maxX(); x++) for (int z=a.minZ(); z<=a.maxZ(); z++) {
                supported(blocks, new Pos(x,a.minY(),z), "connection floor " + c.id());
                for (int y=a.minY(); y<=a.maxY(); y++) air(blocks,new Pos(x,y,z),"connection " + c.id());
            }
        }
        for (StairFlight s : model.stairs()) verifyStair(model, blocks, s);
        for (Window w : model.windows()) {
            Box a=w.opening();
            for (int x=a.minX(); x<=a.maxX(); x++) for (int z=a.minZ(); z<=a.maxZ(); z++) for (int y=a.minY(); y<=a.maxY(); y++) {
                Pos p=new Pos(x,y,z); ReferenceBlock b=blocks.get(p);
                require(b != null && b.kind()==Kind.GLASS,"window glazing",p);
                air(blocks,offset(p,w.outward().dx,0,w.outward().dz),"window exterior " + w.moduleId());
                air(blocks,offset(p,-w.outward().dx,0,-w.outward().dz),"window interior " + w.moduleId());
            }
        }
        for (ReferenceBlock b : localBlocks) if (b.kind()==Kind.LANTERN) {
            ReferenceBlock above=blocks.get(offset(b.pos(),0,1,0));
            require(above != null && above.fullCube(),"hanging lantern support",b.pos());
            require(placementIndex.get(above.pos()) < placementIndex.get(b.pos()),"lantern placed before support",b.pos());
        }
        verifyRoofs(blocks);
        for (ArchitectureModel.Module m : model.modules()) if (m.roof()!=RoofType.BATTLEMENT) {
            boolean present=localBlocks.stream().anyMatch(b -> isRoof(b.kind())
                && b.pos().y()>m.bounds().maxY() && m.footprint(b.pos().x(),b.pos().z()));
            require(present,"missing module roof",m.id());
        }
        for (Pos p : model.circulation()) {
            supported(blocks,p,"circulation support");
            for (int y=0;y<3;y++) air(blocks,offset(p,0,y,0),"circulation headroom");
        }
        require(model.circulation().contains(model.entrance()),"entrance outside circulation",model.entrance());
        Set<Pos> reached=flood(model.circulation(),blocks,model.entrance());
        for (Room r : model.rooms()) require(reached.contains(r.target()),"unreachable room " + r.id(),r.target());
        require(reached.size()==model.circulation().size(),"disconnected circulation",
            model.circulation().stream().filter(p->!reached.contains(p)).findFirst().orElse(null));
        return new QualityReport(model.modules().size(),model.rooms().size(),model.stairs().size(),
            model.windows().size(),localBlocks.size(),model.rooms().size(),model.clearance().size());
    }

    /** Checks the semantic model independently of block emission. */
    public static void validateModel(ArchitectureModel model) {
        require(model.variant()!=null && !model.variant().isBlank(),"missing variant","model");
        require(!model.modules().isEmpty(),"missing modules","model");
        Map<String, ArchitectureModel.Module> modules=new LinkedHashMap<>();
        for (ArchitectureModel.Module m : model.modules()) {
            require(m != null && m.id()!=null && !m.id().isBlank(),"module id","model");
            require(!m.id().equals("courtyard") && !m.id().equals("outside"),"reserved module id",m.id());
            require(modules.putIfAbsent(m.id(),m)==null,"duplicate module id",m.id());
            require(m.type()!=null && m.roof()!=null,"module type/roof",m.id());
            Box b=m.bounds();
            require(b!=null && b.width()>=3 && b.depth()>=3 && b.maxY()-b.minY()>=3,"module dimensions",m.id());
            require(m.chamfer()>=0 && m.chamfer()<Math.min(b.width(),b.depth())/2,"invalid chamfer",m.id());
            require(!m.floors().isEmpty(),"missing module floors",m.id());
            int previous=Integer.MIN_VALUE;
            for (int floor : m.floors()) {
                require(floor>=b.minY() && floor+3<=b.maxY(),"floor outside module",m.id()+" floor="+floor);
                require(floor>previous,"unordered or duplicate floors",m.id()); previous=floor;
            }
        }
        for (int i=0;i<model.modules().size();i++) for (int j=i+1;j<model.modules().size();j++) {
            var a=model.modules().get(i);var b=model.modules().get(j);
            require(!a.bounds().overlaps(b.bounds()),"module collision",a.id()+" / "+b.id());
        }
        Set<String> roomIds=new HashSet<>();
        for (Room r : model.rooms()) {
            require(r != null && r.id()!=null && !r.id().isBlank() && roomIds.add(r.id()),"duplicate/invalid room id",r);
            var m=modules.get(r.moduleId());
            require(m!=null,"unknown room owner",r.id());
            require(r.type()!=null && r.interior()!=null && r.target()!=null,"incomplete room",r.id());
            require(m.floors().contains(r.floorY()),"undeclared room floor",r.id());
            Box a=r.interior();Box b=m.bounds();
            require(a.minY()>=r.floorY()+1 && a.maxY()<=b.maxY(),"room vertical bounds",r.id());
            require(a.minY()<=r.floorY()+1 && a.maxY()>=r.floorY()+3,"room clear height",r.id());
            for (int x : new int[]{a.minX(),a.maxX()}) for (int z : new int[]{a.minZ(),a.maxZ()})
                require(m.inside(x,z),"room outside module interior",r.id());
            require(a.contains(r.target()) && r.target().y()==r.floorY()+1,"room target outside interior",r.id());
        }
        for (int i=0;i<model.rooms().size();i++) for (int j=i+1;j<model.rooms().size();j++) {
            Room a=model.rooms().get(i),b=model.rooms().get(j);
            require(!a.interior().overlaps(b.interior()),"room collision",a.id()+" / "+b.id());
        }
        Set<String> connectionIds=new HashSet<>();
        Map<String,Set<String>> graph=new HashMap<>();
        for (Connection c : model.connections()) {
            require(c!=null && c.id()!=null && connectionIds.add(c.id()),"duplicate/invalid connection id",c);
            require(endpoint(modules,c.from()) && endpoint(modules,c.to()) && !c.from().equals(c.to()),"connection owners",c.id());
            Box a=c.aperture();
            require(a!=null && a.maxY()-a.minY()>=2 && Math.max(a.width(),a.depth())>=2,"connection clearance dimensions",c.id());
            for (String owner : List.of(c.from(),c.to())) if (modules.containsKey(owner))
                require(modules.get(owner).bounds().overlaps(a),"connection misses owner",c.id()+" / "+owner);
            graph.computeIfAbsent(c.from(),k->new HashSet<>()).add(c.to());
            graph.computeIfAbsent(c.to(),k->new HashSet<>()).add(c.from());
        }
        Set<String> connected=new HashSet<>(); ArrayDeque<String> queue=new ArrayDeque<>();
        connected.add("courtyard");queue.add("courtyard");
        while(!queue.isEmpty()) for(String next:graph.getOrDefault(queue.removeFirst(),Set.of())) if(connected.add(next)) queue.addLast(next);
        for(Room r:model.rooms()) require(connected.contains(r.moduleId()),"disconnected module",r.moduleId());
        Set<String> stairIds=new HashSet<>();
        Set<String> detailIds=new HashSet<>();
        for(Partition p:model.partitions()) {
            require(p!=null&&p.id()!=null&&detailIds.add(p.id()),"partition id",p);
            var owner=modules.get(p.moduleId());Box a=p.wall();
            require(owner!=null&&a!=null&&(a.width()==1||a.depth()==1),"partition owner/plane",p.id());
            require(a.minY()>owner.bounds().minY()&&a.maxY()<owner.bounds().maxY(),"partition vertical bounds",p.id());
            for(Room room:model.rooms())if(room.moduleId().equals(p.moduleId()))
                require(!a.overlaps(room.interior()),"partition intersects declared room",p.id()+" / "+room.id());
            for(int x:new int[]{a.minX(),a.maxX()})for(int z:new int[]{a.minZ(),a.maxZ()})require(owner.inside(x,z),"partition outside owner",p.id());
            for(Box portal:p.portals()) {
                require(a.contains(new Pos(portal.minX(),portal.minY(),portal.minZ()))&&a.contains(new Pos(portal.maxX(),portal.maxY(),portal.maxZ())),"partition portal outside wall",p.id());
                require(portal.minY()==a.minY()&&portal.maxY()-portal.minY()>=2,"partition portal height",p.id());
                for(int x=portal.minX();x<=portal.maxX();x++)for(int z=portal.minZ();z<=portal.maxZ();z++)
                    require(model.circulation().contains(new Pos(x,portal.minY(),z)),"partition portal missing route",p.id());
            }
        }
        for(FacadeBay bay:model.facadeBays()) {
            require(bay!=null&&bay.id()!=null&&detailIds.add(bay.id())&&bay.style()!=null,"facade bay identity",bay);
            require(model.windows().contains(bay.window()),"facade bay lacks declared window",bay.id());
            Box a=bay.window().opening();
            require(Math.max(a.width(),a.depth())==3&&Math.min(a.width(),a.depth())==1,"facade bay opening width",bay.id());
            require(bay.envelope().contains(new Pos(a.minX(),a.minY(),a.minZ()))&&bay.envelope().contains(new Pos(a.maxX(),a.maxY(),a.maxZ())),"facade envelope misses glazing",bay.id());
        }
        for(CourtFeature f:model.courtFeatures()) {
            require(f!=null&&f.id()!=null&&detailIds.add(f.id())&&f.use()!=null,"court feature identity",f);
            for(var m:model.modules())require(!f.bounds().overlaps(m.bounds()),"court feature collides with module",f.id()+" / "+m.id());
        }
        for(Dormer d:model.dormers()) {
            require(d!=null&&d.id()!=null&&detailIds.add(d.id()),"dormer identity",d);
            var owner=modules.get(d.moduleId());
            require(owner!=null&&owner.roof()==RoofType.GABLE_Z,"dormer requires longitudinal roof",d.id());
            Box a=owner.bounds();
            require(d.frontX()==a.maxX()&&d.baseY()==a.maxY()+3&&d.centerZ()-3>a.minZ()&&d.centerZ()+3<a.maxZ(),"dormer roof junction",d.id());
        }
        for(int i=0;i<model.courtFeatures().size();i++)for(int j=i+1;j<model.courtFeatures().size();j++)
            require(!model.courtFeatures().get(i).bounds().overlaps(model.courtFeatures().get(j).bounds()),"court feature overlap",i+" / "+j);
        for (StairFlight s : model.stairs()) {
            require(s!=null && s.id()!=null && stairIds.add(s.id()),"duplicate/invalid stair id",s);
            require(modules.containsKey(s.moduleId()) && s.facing()!=null && s.facing()!=Facing.NONE,"stair owner/facing",s.id());
            require(!s.treads().isEmpty(),"empty stair flight",s.id());
        }
        for (Window w : model.windows()) {
            require(w!=null && modules.containsKey(w.moduleId()) && w.outward()!=null && w.outward()!=Facing.NONE,"window owner/facing",w);
            require(w.opening()!=null && modules.get(w.moduleId()).bounds().overlaps(w.opening()),"window outside owner",w);
        }
        require(model.entrance()!=null && !model.circulation().isEmpty(),"empty circulation","model");
    }

    private static boolean endpoint(Map<String,ArchitectureModel.Module> modules,String id) {
        return id!=null && (modules.containsKey(id)||id.equals("courtyard")||id.equals("outside"));
    }
    private static void verifyStair(ArchitectureModel model,Map<Pos,ReferenceBlock> blocks,StairFlight s) {
        var module=model.module(s.moduleId());
        Set<Pos> previous=null; int width=-1;
        for (List<Pos> tread:s.treads()) {
            Set<Pos> cells=new HashSet<>(tread);
            require(cells.size()==tread.size() && cells.size()>=2,"stair width",s.id());
            if(width<0)width=cells.size();
            require(cells.size()==width,"changing stair width",s.id());
            Pos first=tread.getFirst();
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            for(Pos p:tread) {
                require(p.y()==first.y() && (s.facing().dx==0?p.z()==first.z():p.x()==first.x()),"nonlinear tread",s.id());
                int across=s.facing().dx==0?p.x():p.z();min=Math.min(min,across);max=Math.max(max,across);
                require(module.footprint(p.x(),p.z()) && p.y()>=module.bounds().minY() && p.y()<module.bounds().maxY(),"stair outside owner",p);
                ReferenceBlock b=blocks.get(p);
                require(b!=null && (b.kind()==Kind.STONE_STAIRS||b.kind()==Kind.WOOD_STAIRS) && b.facing()==s.facing(),"stair tread/facing",p);
                ReferenceBlock below=blocks.get(offset(p,0,-1,0));
                require(below!=null && (below.fullCube()||below.stair()),"stair support",p);
                for(int y=1;y<=3;y++)air(blocks,offset(p,0,y,0),"stair headroom");
                require(model.circulation().contains(offset(p,0,1,0)),"stair missing from circulation",p);
            }
            require(max-min+1==width,"noncontiguous tread",s.id());
            if(previous!=null)for(Pos p:previous)
                require(cells.contains(offset(p,s.facing().dx,1,s.facing().dz)),"stair rise/run",s.id());
            previous=cells;
        }
    }

    private static Set<Pos> flood(Set<Pos> allowed,Map<Pos,ReferenceBlock> blocks,Pos start) {
        Set<Pos> seen=new HashSet<>();ArrayDeque<Pos> queue=new ArrayDeque<>();seen.add(start);queue.add(start);
        while(!queue.isEmpty()) {
            Pos p=queue.removeFirst();
            for(Facing d:List.of(Facing.NORTH,Facing.EAST,Facing.SOUTH,Facing.WEST))for(int dy=-1;dy<=1;dy++) {
                Pos n=offset(p,d.dx,dy,d.dz);
                if(!allowed.contains(n)||seen.contains(n))continue;
                if(dy==1) {
                    ReferenceBlock step=blocks.get(offset(n,0,-1,0));
                    if(step==null||!step.stair()||step.facing()!=d)continue;
                } else if(dy==-1) {
                    ReferenceBlock step=blocks.get(offset(p,0,-1,0));
                    if(step==null||!step.stair()||step.facing()!=d.rotate(2))continue;
                }
                seen.add(n);queue.addLast(n);
            }
        }
        return seen;
    }
    private static void verifyRoofs(Map<Pos,ReferenceBlock> blocks) {
        Set<Pos> roof=new HashSet<>();for(ReferenceBlock b:blocks.values())if(isRoof(b.kind()))roof.add(b.pos());
        Set<Pos> seen=new HashSet<>();
        for(Pos start:roof)if(seen.add(start)) {
            ArrayDeque<Pos> queue=new ArrayDeque<>();queue.add(start);boolean anchored=false;
            while(!queue.isEmpty()) {
                Pos p=queue.removeFirst();
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)for(int dy=-1;dy<=1;dy++) {
                    if(dx==0&&dy==0&&dz==0)continue;
                    Pos n=offset(p,dx,dy,dz);
                    if(roof.contains(n)) { if(seen.add(n))queue.addLast(n); }
                    else if(dy<=0) {
                        ReferenceBlock support=blocks.get(n);
                        if(support!=null && structural(support.kind()))anchored=true;
                    }
                }
            }
            require(anchored,"unsupported roof component",start);
        }
    }
    private static boolean isRoof(Kind k) {return k==Kind.ROOF||k==Kind.ROOF_STAIRS||k==Kind.ROOF_SLAB;}
    private static boolean structural(Kind k) {return k==Kind.FOUNDATION||k==Kind.STONE||k==Kind.TRIM||k==Kind.TIMBER||k==Kind.PLANKS;}
    private static void supported(Map<Pos,ReferenceBlock> blocks,Pos feet,String context) {
        ReferenceBlock below=blocks.get(offset(feet,0,-1,0));
        require(below!=null && (below.fullCube()||below.stair()),context,feet);
    }
    private static void air(Map<Pos,ReferenceBlock> blocks,Pos p,String context) {
        ReferenceBlock b=blocks.get(p);require(b!=null && b.kind()==Kind.AIR,context,p);
    }
    private static Pos offset(Pos p,int x,int y,int z){return new Pos(p.x()+x,p.y()+y,p.z()+z);}
    private static void require(boolean ok,String category,Object detail) {
        if(!ok)throw new IllegalArgumentException("Reference quality: "+category+" at "+detail);
    }
}
