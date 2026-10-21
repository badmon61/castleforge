package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import dev.castleforge.core.architecture.ArchitectureModel.Module;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Original code-built masonry, articulated bays, roof shells and broad switchback stairs. */
final class ModuleLibrary {
    static void build(ArchitectureModel model,OwnedBlockBuffer b){
        for(Module m:model.modules())shell(model,m,b);
        for(Module m:model.modules())roof(model,m,b);
        for(Window w:model.windows())window(model,w,b);
        ArchitecturalDetails.build(model,b);
        WallWalkLibrary.build(model,b);
        for(StairFlight f:model.stairs())flight(model,f,b);
        for(Pos p:model.clearance())b.airIfEmpty("clearance",p.x(),p.y(),p.z());
    }
    private static void shell(ArchitectureModel model,Module m,OwnedBlockBuffer b){
        if(m.type()==ModuleType.WALL_STAIR)return;
        Box a=m.bounds();String id=m.id();
        for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++){
            if(!m.footprint(x,z))continue;
            boolean wall=!m.inside(x,z);int height=wallHeight(m,x);
            for(int y=0;y<=height;y++){
                Pos p=new Pos(x,y,z);
                Kind k;
                if(model.clearance().contains(p))k=Kind.AIR;
                else if(m.type()==ModuleType.CURTAIN)k=y==0||y==height?Kind.TRIM:Kind.STONE;
                else if(wall)k=y<=1||y==height||y%8==7?Kind.TRIM:
                    m.type()==ModuleType.RESIDENCE&&y>=8?Kind.PLASTER:Kind.STONE;
                else if(m.floors().contains(y))k=Kind.PLANKS;
                else k=Kind.AIR;
                // Corner piers and restrained half-timber upper service/residential facades.
                if(wall&&(k==Kind.STONE||k==Kind.PLASTER)&&(m.type()==ModuleType.RESIDENCE||m.id().equals("service"))&&y>=8
                        &&(x==a.minX()||x==a.maxX())&&Math.floorMod(z-a.minZ(),6)==0)k=Kind.TIMBER;
                b.put(id,x,y,z,k);
            }
        }
        // Reinforced arch with a stepped intrados; this is an open passage, not an incomplete door block.
        if(m.type()==ModuleType.GATEHOUSE){
            for(int z:new int[]{a.minZ(),a.maxZ()}){
                for(int x=-3;x<=3;x++){
                    int h=8-Math.abs(x);
                    for(int y=1;y<h;y++)if(Math.abs(x)<=2)b.put(id,x,y,z,Kind.AIR);
                    if(!model.clearance().contains(new Pos(x,h,z)))b.put(id,x,h,z,Kind.TRIM);
                }
            }
        }
        // Crenels are purposeful bays, kept above lower rooms and out of main circulation.
        if(m.roof()==RoofType.BATTLEMENT&&m.type()!=ModuleType.CURTAIN){
            for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++)
                if(m.footprint(x,z)&&!m.inside(x,z)&&Math.floorMod(x+z,4)<2)
                    b.box(id,x,a.maxY()+1,z,x,a.maxY()+2,z,Kind.STONE);
        }
    }
    private static int wallHeight(Module m,int x){return m.type()==ModuleType.CURTAIN?6:m.type()==ModuleType.GATEHOUSE&&Math.abs(x)<=4?13:m.bounds().maxY();}
    private static void roof(ArchitectureModel model,Module m,OwnedBlockBuffer b){
        if(m.type()==ModuleType.WALL_STAIR)return;
        if(m.roof()==RoofType.BATTLEMENT){
            if(m.type()!=ModuleType.CURTAIN){Box a=m.bounds();for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++)
                if(m.inside(x,z))b.put(m.id(),x,a.maxY(),z,Kind.TRIM);}
            return;
        }
        Box a=m.bounds();
        if(m.type()==ModuleType.GATEHOUSE){
            roofPart(model,m,b,new Box(-11,0,28,-5,19,40),RoofType.HIP,0,false);
            roofPart(model,m,b,new Box(5,0,28,11,19,40),RoofType.HIP,0,false);
            roofPart(model,m,b,new Box(-4,0,28,4,13,40),RoofType.GABLE_Z,0,false);
        }else roofPart(model,m,b,a,m.roof(),m.chamfer(),true);
    }
    private static void roofPart(ArchitectureModel model,Module m,OwnedBlockBuffer b,Box a,RoofType roof,int chamfer,boolean overhang){
        int over=overhang?1:0,x1=a.minX()-over,x2=a.maxX()+over,z1=a.minZ()-over,z2=a.maxZ()+over;
        for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++){
            int dx=Math.min(x-x1,x2-x),dz=Math.min(z-z1,z2-z);
            if(dx+dz<chamfer)continue;
            final int px=x,pz=z;
            if(model.modules().stream().anyMatch(other->!other.id().equals(m.id())&&other.footprint(px,pz)))continue;
            int rise=roof==RoofType.GABLE_X?dz:roof==RoofType.GABLE_Z?dx:Math.min(dx,dz);
            int y=a.maxY()+1+rise;
            // Explicit roof-abutment rule: the earlier dominant mass owns a shared eave column.
            // Crop this subordinate overhang; never overwrite another module's roof or masonry.
            if(b.occupiedByOther(m.id(),new Pos(x,y,z))||b.occupiedByOther(m.id(),new Pos(x,y-1,z)))continue;
            Facing face=roof==RoofType.GABLE_X?(z<=(z1+z2)/2?Facing.SOUTH:Facing.NORTH)
                :roof==RoofType.GABLE_Z?(x<=(x1+x2)/2?Facing.EAST:Facing.WEST)
                :dx<dz?(x<=(x1+x2)/2?Facing.EAST:Facing.WEST):(z<=(z1+z2)/2?Facing.SOUTH:Facing.NORTH);
            // One course beneath each stair ties diagonal roof strips into one supported shell.
            b.put(m.id(),x,y-1,z,rise==0?Kind.TIMBER:Kind.ROOF);
            boolean ridge=roof==RoofType.GABLE_X?z==((z1+z2)/2):roof==RoofType.GABLE_Z?x==((x1+x2)/2):dx==dz&&dx>=Math.min(a.width(),a.depth())/2;
            b.put(m.id(),x,y,z,ridge?Kind.ROOF_SLAB:Kind.ROOF_STAIRS, ridge?Facing.NONE:face);
            // Gable masonry closes the end triangle; central timber expresses the ridge load.
            boolean end=roof==RoofType.GABLE_X?(x==a.minX()||x==a.maxX()):(roof==RoofType.GABLE_Z&&(z==a.minZ()||z==a.maxZ()));
            if(end)for(int fill=a.maxY()+1;fill<y-1;fill++)b.put(m.id(),x,fill,z,Kind.STONE);
        }
    }
    private static void flight(ArchitectureModel model,StairFlight f,OwnedBlockBuffer b){
        int startY=f.treads().getFirst().getFirst().y()-1;
        // Return flights begin above the common mid landing; supports start at the storey base.
        int base=f.id().endsWith("-back")?startY-4:startY;
        for(var row:f.treads())for(Pos p:row){
            for(int y=base+1;y<p.y();y++)if(!model.clearance().contains(new Pos(p.x(),y,p.z())))b.put(f.moduleId(),p.x(),y,p.z(),Kind.STONE);
            b.put(f.moduleId(),p.x(),p.y(),p.z(),Kind.STONE_STAIRS,f.facing());
        }
        if(f.id().endsWith("-out")){
            Pos first=f.treads().getFirst().getFirst();int x=first.x(),z=first.z()+1;
            for(int px=x;px<=x+7;px++)for(int pz=z-6;pz<=z-5;pz++){
                for(int y=base+1;y<=base+4;y++)if(!model.clearance().contains(new Pos(px,y,pz)))b.put(f.moduleId(),px,y,pz,Kind.STONE);
            }
            // A solid central stair spine guards the switchback well, including the upper landing edge.
            for(int px=x+3;px<=x+4;px++)for(int pz=z-4;pz<=z-1;pz++)for(int y=base+1;y<=base+9;y++)
                if(!model.clearance().contains(new Pos(px,y,pz)))b.put(f.moduleId(),px,y,pz,Kind.STONE);
        }
    }
    private static void window(ArchitectureModel model,Window w,OwnedBlockBuffer b){
        Box a=w.opening();String id=w.moduleId();Facing out=w.outward();
        for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++){
            for(int y=a.minY();y<=a.maxY();y++){
                b.put(id,x,y,z,Kind.GLASS);
                b.airIfEmpty("exterior",x+out.dx,y,z+out.dz);
            }
            b.put(id,x,a.minY()-1,z,Kind.TRIM);
            b.put(id,x,a.maxY()+1,z,Kind.TRIM);
            // A full-cube sill projects one block from the recessed glass without fragile attachments.
            Pos sill=new Pos(x+out.dx,a.minY()-1,z+out.dz);
            if(!model.clearance().contains(sill))b.put(id,sill.x(),sill.y(),sill.z(),Kind.TRIM);
        }
    }
}
