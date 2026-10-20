package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import java.util.*;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import dev.castleforge.core.architecture.ArchitectureModel.Module;
import static dev.castleforge.core.architecture.ReferenceBlock.Facing.*;

/** An authored composition, not randomized boxes. Canonical south entrance; rotations happen last. */
final class ReferenceLayout {
    private final List<Module> modules=new ArrayList<>();
    private final List<Room> rooms=new ArrayList<>();
    private final List<Connection> connections=new ArrayList<>();
    private final List<StairFlight> stairs=new ArrayList<>();
    private final List<Window> windows=new ArrayList<>();
    private final List<Partition> partitions=new ArrayList<>();
    private final List<FacadeBay> bays=new ArrayList<>();
    private final List<CourtFeature> courtFeatures=new ArrayList<>();
    private final Set<Pos> circulation=new LinkedHashSet<>(),clearance=new LinkedHashSet<>();
    static ArchitectureModel create(){return new ReferenceLayout().createModel();}
    private ArchitectureModel createModel(){
        module("keep",ModuleType.KEEP,-39,-30,-21,-12,32,List.of(0,8,16,24),RoofType.HIP,3);
        module("hall",ModuleType.GREAT_HALL,-20,-30,17,-11,14,List.of(0),RoofType.GABLE_X,0);
        module("residence",ModuleType.RESIDENCE,18,-30,36,3,22,List.of(0,8,16),RoofType.GABLE_Z,0);
        module("kitchen",ModuleType.SERVICE_WING,18,4,36,19,10,List.of(0),RoofType.HIP,0);
        module("service",ModuleType.SERVICE_WING,-39,-11,-21,12,16,List.of(0,8),RoofType.GABLE_Z,0);
        module("gate",ModuleType.GATEHOUSE,-11,28,11,40,19,List.of(0,8),RoofType.HIP,0);
        module("northwest",ModuleType.CORNER_TOWER,-49,-40,-40,-31,13,List.of(0),RoofType.HIP,2);
        module("northeast",ModuleType.CORNER_TOWER,40,-40,49,-31,17,List.of(0),RoofType.HIP,2);
        module("southwest",ModuleType.CORNER_TOWER,-49,30,-40,40,10,List.of(0),RoofType.BATTLEMENT,2);
        module("southeast",ModuleType.CORNER_TOWER,40,30,49,40,14,List.of(0),RoofType.HIP,2);
        module("northwall",ModuleType.CURTAIN,-39,-40,39,-38,6,List.of(0),RoofType.BATTLEMENT,0);
        module("westwall",ModuleType.CURTAIN,-49,-30,-47,29,6,List.of(0),RoofType.BATTLEMENT,0);
        module("eastwall",ModuleType.CURTAIN,47,-30,49,29,6,List.of(0),RoofType.BATTLEMENT,0);
        module("southwall-west",ModuleType.CURTAIN,-39,38,-12,40,6,List.of(0),RoofType.BATTLEMENT,0);
        module("southwall-east",ModuleType.CURTAIN,12,38,39,40,6,List.of(0),RoofType.BATTLEMENT,0);
        courtFeatures.add(new CourtFeature("well",CourtUse.COVERED_WELL,new Box(-10,0,3,-4,7,9)));
        courtFeatures.add(new CourtFeature("herbs",CourtUse.HERB_GARDEN,new Box(-35,0,20,-25,2,28)));
        courtFeatures.add(new CourtFeature("arbor",CourtUse.SEATING_ARBOR,new Box(25,0,23,35,6,28)));
        for(int x=-46;x<=46;x++)for(int z=-37;z<=37;z++) {
            final int px=x,pz=z;
            boolean facadeMargin=modules.stream().anyMatch(m->m.type()!=ModuleType.CURTAIN&&
                (m.footprint(px,pz)||m.footprint(px-1,pz)||m.footprint(px+1,pz)||m.footprint(px,pz-1)||m.footprint(px,pz+1)));
            boolean feature=courtFeatures.stream().anyMatch(c->c.bounds().contains(new Pos(px,0,pz)));
            if(!facadeMargin&&!feature&&modules.stream().noneMatch(m->m.footprint(px,pz)))walk(x,0,z);
        }
        for(int f:new int[]{0,8,16,24}) {
            corridor(-36,f,-17,-23,-15); corridor(-26,f,-16,-24,-13);
            room("keep-"+f,"keep",f==0?RoomType.STAIR_HALL:f==8?RoomType.LIBRARY:f==16?RoomType.BEDCHAMBER:RoomType.SOLAR,f,-24,-15);
        }
        staircase("keep",-36,-19,0);staircase("keep",-36,-19,8);staircase("keep",-36,-19,16);
        corridor(-19,0,-16,16,-14);corridor(-1,0,-14,1,-10);
        room("great-hall","hall",RoomType.GREAT_HALL,0,0,-15);
        for(int f:new int[]{0,8,16}) {
            corridor(21,f,-28,23,2);corridor(19,f,-17,34,-15);
            rooms.add(new Room("residence-"+f,"residence",RoomType.STAIR_HALL,new Box(19,f+1,-29,35,Math.min(f+6,21),-14),f,new Pos(22,f+1,-15)));
            int top=Math.min(f+7,21);
            partitions.add(new Partition("residence-cross-"+f,"residence",new Box(19,f+1,-13,35,top,-13),List.of(new Box(21,f+1,-13,23,f+3,-13))));
            partitions.add(new Partition("residence-spine-"+f,"residence",new Box(25,f+1,-12,25,top,2),List.of(new Box(25,f+1,-9,25,f+3,-7),new Box(25,f+1,-1,25,f+3,1))));
            partitions.add(new Partition("residence-divider-"+f,"residence",new Box(26,f+1,-4,35,top,-4),List.of()));
            rooms.add(new Room("chamber-"+f,"residence",f==0?RoomType.GUARDROOM:f==8?RoomType.BEDCHAMBER:RoomType.LIBRARY,new Box(26,f+1,-12,35,Math.min(f+6,21),-5),f,new Pos(28,f+1,-8)));
            rooms.add(new Room("study-"+f,"residence",f==0?RoomType.STORE:RoomType.SOLAR,new Box(26,f+1,-3,35,Math.min(f+6,21),2),f,new Pos(28,f+1,0)));
            corridor(23,f,-9,28,-7);corridor(23,f,-1,28,1);
        }
        staircase("residence",26,-19,0);staircase("residence",26,-19,8);
        corridor(21,0,3,23,17);corridor(18,0,9,23,11);
        room("kitchen-room","kitchen",RoomType.KITCHEN,0,22,10);
        for(int f:new int[]{0,8}) {
            corridor(-26,f,-10,-24,10);corridor(-36,f,7,-24,9);
            room("service-"+f,"service",f==0?RoomType.STORE:RoomType.LIBRARY,f,-25,8);
        }
        staircase("service",-36,5,0);
        corridor(-2,0,27,2,40);corridor(-10,0,38,9,39);
        corridor(-10,8,38,9,39);corridor(4,8,30,6,39);
        room("gate-guard","gate",RoomType.GUARDROOM,0,5,38);
        room("gate-watch","gate",RoomType.GUARDROOM,8,5,38);
        staircase("gate",-10,37,0);
        connection("court-hall","courtyard","hall",-1,1,-11,1,3,-11);
        connection("hall-keep","hall","keep",-21,1,-17,-20,3,-15);
        connection("hall-residence","hall","residence",17,1,-17,18,3,-15);
        connection("keep-service","keep","service",-26,1,-12,-24,3,-11);
        connection("court-service","courtyard","service",-21,1,7,-21,3,9);
        corridor(-25,0,7,-20,9);
        connection("residence-kitchen","residence","kitchen",21,1,3,23,3,4);
        connection("court-kitchen","courtyard","kitchen",18,1,9,18,3,11);
        connection("court-gate","courtyard","gate",-2,1,28,2,5,28);
        connection("gate-outside","gate","outside",-2,1,40,2,5,40);
        for(String id:List.of("northwest","northeast","southwest","southeast")) {
            Module m=find(id);int cx=(m.bounds().minX()+m.bounds().maxX())/2;
            int cz=(m.bounds().minZ()+m.bounds().maxZ())/2;
            boolean north=m.bounds().minZ()<0;int wall=north?m.bounds().maxZ():m.bounds().minZ();
            corridor(cx-1,0,Math.min(cz,wall),cx+1,Math.max(cz,wall));
            connection("court-"+id,"courtyard",id,cx-1,1,wall,cx+1,3,wall);
            room(id+"-guard",id,RoomType.GUARDROOM,0,cx,cz);
        }
        // Articulated bays on exposed walls only. Adjacent wings deliberately receive no blind glazing.
        for(int x:new int[]{-15,-9,-3,3,9,15})bay("hall",x-1,4,-30,x+1,8,-30,NORTH,BayStyle.HALL_LANCET);
        for(int x:new int[]{-15,-9,9,15})bay("hall",x-1,4,-11,x+1,8,-11,SOUTH,BayStyle.HALL_LANCET);
        for(int f:new int[]{0,8,16,24})for(int z:new int[]{-25,-19})window("keep",-39,f+3,z,-39,f+5,z,WEST);
        for(int f:new int[]{0,8,16,24})for(int x:new int[]{-34,-28})window("keep",x,f+3,-30,x,f+5,-30,NORTH);
        for(int z:new int[]{-25,-19})window("keep",-21,28,z,-21,30,z,EAST);
        for(int x:new int[]{-34,-28})window("keep",x,28,-12,x,30,-12,SOUTH);
        for(int f:new int[]{0,8,16})for(int z:new int[]{-25,-19,-8,0})bay("residence",36,f+3,z-1,36,f+5,z+1,EAST,BayStyle.CHAMBER_CASEMENT);
        for(int f:new int[]{0,8,16})for(int z:new int[]{-7,-1})window("residence",18,f+3,z,18,f+5,z,WEST);
        for(int f:new int[]{0,8,16})for(int x:new int[]{23,30})window("residence",x,f+3,-30,x,f+5,-30,NORTH);
        for(int f:new int[]{0,8})for(int z:new int[]{-6,0,6})window("service",-39,f+3,z,-39,f+5,z,WEST);
        for(int f:new int[]{0,8})for(int z:new int[]{-6,0,6})window("service",-21,f+3,z,-21,f+5,z,EAST);
        for(int x:new int[]{23,30})window("kitchen",x,3,19,x,5,19,SOUTH);
        for(int z:new int[]{9,15})window("kitchen",36,3,z,36,5,z,EAST);
        for(int x:new int[]{-7,7})window("gate",x,11,40,x,13,40,SOUTH);
        corridor(29,0,22,31,26); // a reserved seated-court approach, through the arbor's open front
        for(Pos p:circulation)for(int h=0;h<3;h++)clearance.add(new Pos(p.x(),p.y()+h,p.z()));
        // Taller arch openings are protected beyond the minimum route headroom.
        for(Connection c:connections)for(int x=c.aperture().minX();x<=c.aperture().maxX();x++)
            for(int z=c.aperture().minZ();z<=c.aperture().maxZ();z++)for(int y=c.aperture().minY();y<=c.aperture().maxY();y++)clearance.add(new Pos(x,y,z));
        return new ArchitectureModel("falkenwacht-v2-study",modules,rooms,connections,stairs,windows,clearance,circulation,new Pos(0,1,40),partitions,bays,courtFeatures,
            List.of(new Dormer("residence-roof-north","residence",36,-23,25),new Dormer("residence-roof-south","residence",36,-5,25)));
    }
    private void module(String id,ModuleType type,int x1,int z1,int x2,int z2,int height,List<Integer> floors,RoofType roof,int chamfer){
        modules.add(new Module(id,type,new Box(x1,0,z1,x2,height,z2),floors,roof,chamfer));
    }
    private Module find(String id){return modules.stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();}
    private void room(String id,String owner,RoomType type,int f,int x,int z){
        Module m=find(owner);Box b=m.bounds();
        int inset=m.chamfer()/2+1;
        rooms.add(new Room(id,owner,type,new Box(b.minX()+inset,f+1,b.minZ()+inset,b.maxX()-inset,Math.min(f+6,b.maxY()-1),b.maxZ()-inset),f,new Pos(x,f+1,z)));
    }
    private void walk(int x,int floor,int z){circulation.add(new Pos(x,floor+1,z));}
    private void corridor(int x1,int floor,int z1,int x2,int z2){for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)walk(x,floor,z);}
    private void connection(String id,String from,String to,int x1,int y1,int z1,int x2,int y2,int z2){
        connections.add(new Connection(id,from,to,new Box(x1,y1,z1,x2,y2,z2)));
        corridor(x1,y1-1,z1,x2,z2);
        // Cross the reserved facade margin at an actual portal, without widening its jambs.
        if(!from.equals("outside")&&!to.equals("outside")) {
            if(x1==x2)corridor(x1-2,y1-1,z1,x2+2,z2);
            if(z1==z2)corridor(x1,y1-1,z1-2,x2,z2+2);
        }
    }
    private void window(String owner,int x1,int y1,int z1,int x2,int y2,int z2,ReferenceBlock.Facing out){windows.add(new Window(owner,new Box(x1,y1,z1,x2,y2,z2),out));}
    private void bay(String owner,int x1,int y1,int z1,int x2,int y2,int z2,ReferenceBlock.Facing out,BayStyle style){
        Window w=new Window(owner,new Box(x1,y1,z1,x2,y2,z2),out);windows.add(w);
        int tx=out.dz==0?0:1,tz=out.dx==0?0:1;
        Box e=new Box(Math.min(x1-tx,x1+out.dx-tx),y1-1,Math.min(z1-tz,z1+out.dz-tz),Math.max(x2+tx,x2+out.dx+tx),y2+2,Math.max(z2+tz,z2+out.dz+tz));
        bays.add(new FacadeBay(owner+"-bay-"+bays.size(),w,style,e));
    }
    private void staircase(String owner,int x,int z,int floor){
        List<List<Pos>> up=new ArrayList<>(),back=new ArrayList<>();
        for(int n=1;n<=4;n++){
            List<Pos> a=new ArrayList<>(),b=new ArrayList<>();
            for(int w=0;w<3;w++){
                Pos p=new Pos(x+w,floor+n,z-n);a.add(p);walk(p.x(),p.y(),p.z());
                Pos q=new Pos(x+5+w,floor+4+n,z-5+n);b.add(q);walk(q.x(),q.y(),q.z());
            }
            up.add(a);back.add(b);
        }
        stairs.add(new StairFlight(owner+"-"+floor+"-out",owner,NORTH,up));
        stairs.add(new StairFlight(owner+"-"+floor+"-back",owner,SOUTH,back));
        corridor(x,floor+4,z-6,x+7,z-5);
        corridor(x,floor,z,x+2,z+2);
        corridor(x+5,floor+8,z,x+7,z+2);
    }
}
