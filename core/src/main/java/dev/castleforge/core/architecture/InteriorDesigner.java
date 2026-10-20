package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import java.util.*;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Authored furniture groups. They are decorative solids, never inventories, beds or live fire. */
final class InteriorDesigner {
    private record Detail(int x,int y,int z,Kind kind,Facing facing) {}

    static void furnish(ArchitectureModel model,OwnedBlockBuffer blocks,long seed) {
        Designer d=new Designer(model,blocks);
        for(Room room:model.rooms()) {
            if(room.type()==RoomType.GREAT_HALL) d.greatHall(room);
            else d.room(room);
            d.requireFunction(room);
        }
    }

    private static final class Designer {
        private final ArchitectureModel model;
        private final OwnedBlockBuffer blocks;
        private final Map<String,EnumMap<Kind,Integer>> furniture=new HashMap<>();
        Designer(ArchitectureModel model,OwnedBlockBuffer blocks){this.model=model;this.blocks=blocks;}

        void greatHall(Room r) {
            int f=r.floorY();
            // Long-axis dining arrangement leaves the southern processional aisle completely free.
            table(r,-12,f+1,-24,8,-22);
            bench(r,-12,f+1,-26,8,Facing.NORTH);
            bench(r,-12,f+1,-20,8,Facing.SOUTH);
            fireplace(r,11,f+1,-29);
            List<Detail> dais=new ArrayList<>();
            box(dais,12,f+1,-24,15,f+1,-18,Kind.PLANKS);
            for(int z=-23;z<=-19;z++) {
                add(dais,12,f+2,z,Kind.TIMBER);
                add(dais,12,f+3,z,Kind.SLAB);
            }
            for(int z:new int[]{-22,-20}) add(dais,14,f+2,z,Kind.WOOD_STAIRS,Facing.EAST);
            place(r,dais);
            // Visible transverse roof structure, with suspended lighting below each beam.
            for(int x:new int[]{-15,-7,1,9}) {
                List<Detail> truss=new ArrayList<>();
                for(int z=-29;z<=-12;z++) add(truss,x,f+12,z,Kind.TIMBER,Facing.NORTH);
                for(int y=f+8;y<f+12;y++) {
                    add(truss,x,y,-29,Kind.TIMBER);
                    add(truss,x,y,-12,Kind.TIMBER);
                }
                add(truss,x,f+11,-23,Kind.LANTERN);
                place(r,truss);
            }
        }

        void room(Room r) {
            Box b=r.interior();int f=r.floorY(),x=b.minX()+1,z=b.minZ()+1;
            switch(r.type()) {
                case KITCHEN -> {
                    counter(r,x,f+1,z,Math.min(x+9,b.maxX()-1),z+1);
                    fireplace(r,b.maxX()-5,f+1,b.minZ()+1);
                    table(r,x,f+1,b.maxZ()-5,Math.min(x+6,b.maxX()-1),b.maxZ()-3);
                    rack(r,b.maxX()-3,f+1,b.maxZ()-5,3);
                }
                case STORE -> {
                    for(int dx:new int[]{0,4,8}) for(int dz:new int[]{0,5})
                        crate(r,x+dx,f+1,z+dz,1+Math.floorMod(dx+dz,2));
                    rack(r,b.maxX()-3,f+1,b.maxZ()-7,5);
                }
                case LIBRARY -> {
                    bookcase(r,x,f+1,z,Math.min(11,b.width()-4),true);
                    bookcase(r,x,f+1,z+4,Math.min(7,b.depth()-7),false);
                    table(r,b.maxX()-7,f+1,b.maxZ()-5,b.maxX()-3,b.maxZ()-3);
                    bench(r,b.maxX()-7,f+1,b.maxZ()-1,b.maxX()-3,Facing.SOUTH);
                }
                case BEDCHAMBER -> {
                    sleepingPlatform(r,x,f+1,z);
                    sleepingPlatform(r,b.maxX()-4,f+1,z);
                    table(r,b.maxX()-7,f+1,b.maxZ()-5,b.maxX()-3,b.maxZ()-4);
                    bookcase(r,x,f+1,b.maxZ()-2,5,true);
                }
                case GUARDROOM -> {
                    table(r,x,f+1,z,x+5,z+2);
                    bench(r,x,f+1,z+4,x+5,Facing.SOUTH);
                    crate(r,b.maxX()-3,f+1,b.maxZ()-3,2);
                    rack(r,b.maxX()-3,f+1,z+1,4);
                }
                case SOLAR -> {
                    bookcase(r,x,f+1,z,Math.min(7,b.width()-4),true);
                    table(r,b.maxX()-7,f+1,b.maxZ()-5,b.maxX()-3,b.maxZ()-3);
                    bench(r,x,f+1,b.maxZ()-2,Math.min(x+6,b.maxX()-1),Facing.SOUTH);
                }
                case STAIR_HALL -> bench(r,x,f+1,b.maxZ()-2,Math.min(x+4,b.maxX()-1),Facing.SOUTH);
                default -> {}
            }
            // Deliberately fitted groups in the asymmetric wings. These use the actual room's free bays,
            // not a generic corner that may be occupied by the circulation spine or switchback stair.
            switch(r.id()) {
                case "residence-0" -> {
                    table(r,27,f+1,-10,32,-8);
                    bench(r,27,f+1,-6,32,Facing.SOUTH);
                }
                case "residence-8" -> {
                    sleepingPlatform(r,28,f+1,-10);
                    sleepingPlatform(r,32,f+1,-4);
                }
                case "kitchen-room" -> counter(r,27,f+1,10,28,16);
                case "northwest-guard" -> crate(r,-47,f+1,-38,2);
                case "northeast-guard" -> crate(r,46,f+1,-38,2);
                default -> {}
            }
            // A short ceiling tie and two lamps. Skip the entire group if it crosses the stair clearance.
            int beamY=Math.min(f+6,b.maxY());
            if(beamY>=f+5) {
                int midZ=(b.minZ()+b.maxZ())/2;
                List<Detail> tie=new ArrayList<>();
                for(int bx=b.minX();bx<=b.maxX();bx++)add(tie,bx,beamY,midZ,Kind.TIMBER,Facing.EAST);
                add(tie,b.minX()+2,beamY-1,midZ,Kind.LANTERN);
                add(tie,b.maxX()-2,beamY-1,midZ,Kind.LANTERN);
                place(r,tie);
            }
            // Individual hanging lamps remain possible when a stairwell prevents the full tie beam.
            for(int lx:new int[]{b.minX()+2,b.maxX()-2})for(int lz:new int[]{b.minZ()+2,b.maxZ()-2}) {
                int y=Math.min(f+5,b.maxY()-1);
                if(y<f+4)continue;
                List<Detail> bracket=new ArrayList<>();
                int wallEnd=lx==b.minX()+2?b.minX():b.maxX();
                for(int bx=Math.min(lx,wallEnd);bx<=Math.max(lx,wallEnd);bx++)
                    add(bracket,bx,y+1,lz,Kind.TIMBER,Facing.EAST);
                add(bracket,lx,y,lz,Kind.LANTERN);
                place(r,bracket);
            }
        }

        void requireFunction(Room room) {
            EnumMap<Kind,Integer> counts=furniture.getOrDefault(room.id(),new EnumMap<>(Kind.class));
            int planks=counts.getOrDefault(Kind.PLANKS,0),slabs=counts.getOrDefault(Kind.SLAB,0);
            int seats=counts.getOrDefault(Kind.WOOD_STAIRS,0),books=counts.getOrDefault(Kind.BOOKSHELF,0);
            boolean fitted=switch(room.type()) {
                case GREAT_HALL -> slabs>=12&&seats>=8;
                case KITCHEN,STORE -> planks>=4;
                case BEDCHAMBER -> counts.getOrDefault(Kind.WOOL,0)>=6;
                case LIBRARY -> books>=6;
                case GUARDROOM -> planks>=4||seats>=2;
                case SOLAR -> books>=2||slabs>=3||seats>=3;
                case STAIR_HALL -> true;
            };
            if(!fitted)throw new IllegalArgumentException("Room lacks functional furnishing: "+room.id()+" ("+room.type()+") "+counts);
        }

        void table(Room r,int x1,int y,int z1,int x2,int z2) {
            if(x2<x1||z2<z1)return;
            List<Detail> group=new ArrayList<>();
            box(group,x1,y+1,z1,x2,y+1,z2,Kind.SLAB);
            for(int x=x1;x<=x2;x+=Math.max(2,Math.min(5,x2-x1))) {
                add(group,x,y,z1,Kind.TIMBER);add(group,x,y,z2,Kind.TIMBER);
            }
            add(group,x2,y,z1,Kind.TIMBER);add(group,x2,y,z2,Kind.TIMBER);
            place(r,group);
        }
        void bench(Room r,int x1,int y,int z,int x2,Facing facing) {
            List<Detail> group=new ArrayList<>();
            for(int x=x1;x<=x2;x++)add(group,x,y,z,Kind.WOOD_STAIRS,facing);
            place(r,group);
        }
        void counter(Room r,int x1,int y,int z1,int x2,int z2) {
            List<Detail> group=new ArrayList<>();
            box(group,x1,y,z1,x2,y,z2,Kind.PLANKS);
            box(group,x1,y+1,z1,x2,y+1,z2,Kind.SLAB);
            place(r,group);
        }
        void crate(Room r,int x,int y,int z,int height) {
            List<Detail> group=new ArrayList<>();box(group,x,y,z,x+1,y+height-1,z+1,Kind.PLANKS);
            add(group,x,y+height,z,Kind.SLAB);add(group,x+1,y+height,z,Kind.SLAB);
            place(r,group);
        }
        void rack(Room r,int x,int y,int z,int length) {
            List<Detail> group=new ArrayList<>();
            box(group,x,y,z,x,y+3,z,Kind.TIMBER);
            box(group,x,y,z+length,x,y+3,z+length,Kind.TIMBER);
            for(int dz=1;dz<length;dz++)for(int dy:new int[]{0,2})add(group,x,y+dy,z+dz,Kind.SLAB);
            place(r,group);
        }
        void bookcase(Room r,int x,int y,int z,int length,boolean alongX) {
            List<Detail> group=new ArrayList<>();
            for(int i=0;i<length;i++)for(int dy=0;dy<3;dy++) {
                add(group,x+(alongX?i:0),y+dy,z+(alongX?0:i),
                    i==0||i==length-1?Kind.TIMBER:Kind.BOOKSHELF);
            }
            place(r,group);
        }
        void sleepingPlatform(Room r,int x,int y,int z) {
            List<Detail> group=new ArrayList<>();
            box(group,x,y,z,x+1,y,z+3,Kind.PLANKS);
            box(group,x,y+1,z+1,x+1,y+1,z+3,Kind.WOOL);
            box(group,x,y+1,z,x+1,y+2,z,Kind.TIMBER);
            place(r,group);
        }
        void fireplace(Room r,int x,int y,int z) {
            List<Detail> group=new ArrayList<>();
            // A safe glowing hearth niche; no fire tick, smoke entity or inventory.
            box(group,x,y,z,x+4,y+4,z,Kind.STONE);
            box(group,x,y,z+1,x,y+3,z+2,Kind.TRIM);
            box(group,x+4,y,z+1,x+4,y+3,z+2,Kind.TRIM);
            box(group,x+1,y,z+1,x+3,y,z+2,Kind.STONE);
            box(group,x+1,y+3,z+1,x+3,y+3,z+2,Kind.TRIM);
            add(group,x+2,y+2,z+1,Kind.LANTERN);
            place(r,group);
        }

        /** No partial furniture groups or untracked overwrites: reserve the whole ensemble bounding volume. */
        boolean place(Room room,List<Detail> group) {
            if(group.isEmpty())return false;
            var module=model.module(room.moduleId());
            int minX=Integer.MAX_VALUE,minY=minX,minZ=minX,maxX=Integer.MIN_VALUE,maxY=maxX,maxZ=maxX;
            Map<Pos,Detail> unique=new LinkedHashMap<>();
            for(Detail d:group) {
                Pos p=new Pos(d.x,d.y,d.z);unique.put(p,d);
                if(!module.inside(d.x,d.z)||d.y<=room.floorY()||d.y>module.bounds().maxY())return false;
                ReferenceBlock existing=blocks.at(p);
                if(existing==null||existing.kind()!=Kind.AIR)return false;
                minX=Math.min(minX,d.x);minY=Math.min(minY,d.y);minZ=Math.min(minZ,d.z);
                maxX=Math.max(maxX,d.x);maxY=Math.max(maxY,d.y);maxZ=Math.max(maxZ,d.z);
            }
            for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)for(int y=minY;y<=maxY;y++)
                if(model.clearance().contains(new Pos(x,y,z)))return false;
            // Full support above every hanging lantern is explicit in the ensemble or already structural.
            for(Detail d:unique.values())if(d.kind==Kind.LANTERN) {
                Pos support=new Pos(d.x,d.y+1,d.z);Detail planned=unique.get(support);
                ReferenceBlock actual=blocks.at(support);
                if(planned==null ? actual==null||!actual.fullCube() : !new ReferenceBlock(support,planned.kind,planned.facing).fullCube())return false;
            }
            for(Detail d:unique.values())blocks.put(room.moduleId(),d.x,d.y,d.z,d.kind,d.facing);
            EnumMap<Kind,Integer> counts=furniture.computeIfAbsent(room.id(),id->new EnumMap<>(Kind.class));
            for(Detail d:unique.values())counts.merge(d.kind,1,Integer::sum);
            return true;
        }
    }
    private static void add(List<Detail> g,int x,int y,int z,Kind k){add(g,x,y,z,k,Facing.NONE);}
    private static void add(List<Detail> g,int x,int y,int z,Kind k,Facing f){g.add(new Detail(x,y,z,k,f));}
    private static void box(List<Detail> g,int x1,int y1,int z1,int x2,int y2,int z2,Kind k) {
        for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=y1;y<=y2;y++)add(g,x,y,z,k);
    }
    private InteriorDesigner() {}
}
