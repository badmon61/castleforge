package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Original masonry/joinery vocabulary applied to declared bays, partitions and court ensembles. */
final class ArchitecturalDetails {
    static void build(ArchitectureModel model,OwnedBlockBuffer b) {
        for(Partition p:model.partitions()) {
            Box a=p.wall();
            for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++)for(int y=a.minY();y<=a.maxY();y++) {
                Pos pos=new Pos(x,y,z);
                if(p.portals().stream().anyMatch(portal->portal.contains(pos))) continue;
                boolean post=a.width()==1?Math.floorMod(z-a.minZ(),4)==0:Math.floorMod(x-a.minX(),4)==0;
                b.put(p.moduleId(),x,y,z,post||y==a.maxY()?Kind.TIMBER:Kind.PLASTER);
            }
        }
        for(FacadeBay bay:model.facadeBays())bay(bay,b);
        gableJoinery(model,b);
        hallStructure(model,b);
        for(Dormer d:model.dormers())dormer(d,b);
        for(CourtFeature f:model.courtFeatures())court(f,b);
    }
    private static void gableJoinery(ArchitectureModel model,OwnedBlockBuffer b) {
        for(String id:new String[]{"residence","service"}) {
            var m=model.module(id);Box a=m.bounds();int mid=(a.minX()+a.maxX())/2;
            for(int z:new int[]{a.minZ(),a.maxZ()})for(int x=a.minX();x<=a.maxX();x++)for(int y=a.maxY()+1;y<=a.maxY()+12;y++) {
                Pos p=new Pos(x,y,z);ReferenceBlock old=b.at(p);
                if(old==null||old.kind()!=Kind.STONE)continue;
                boolean frame=x==mid||y==a.maxY()+2||Math.abs(x-mid)==y-a.maxY()-2;
                b.put(id,x,y,z,frame?Kind.TIMBER:Kind.PLASTER);
            }
        }
    }
    private static void dormer(Dormer d,OwnedBlockBuffer b) {
        int front=d.frontX(),mid=d.centerZ(),base=d.baseY();String id=d.moduleId();
        // The rear edge meets the existing roof at its own height. No cross-owner crop or overwrite.
        for(int x=front-4;x<=front;x++)for(int z=mid-2;z<=mid+2;z++) {
            int oldRoof=base-1+front-x;
            for(int y=oldRoof+1;y<=base+3;y++) {
                boolean frontWall=x==front,side=Math.abs(z-mid)==2;
                b.put(id,x,y,z,frontWall?(side||y==base?Kind.TIMBER:Kind.GLASS):side?Kind.TIMBER:Kind.AIR);
            }
        }
        for(int x=front-4;x<=front+1;x++)for(int z=mid-3;z<=mid+3;z++) {
            int y=base+3+(3-Math.abs(z-mid));
            // Preserve the dominant roof where it rises above the subordinate eave.
            int parentRoof=base-1+front-x;
            if(y<=parentRoof)continue;
            if(x==front)for(int fill=base+4;fill<y-1;fill++)b.put(id,x,fill,z,Kind.PLASTER);
            b.put(id,x,y-1,z,Kind.TIMBER,Facing.EAST);
            b.put(id,x,y,z,z==mid?Kind.ROOF_SLAB:Kind.ROOF_STAIRS,z==mid?Facing.NONE:z<mid?Facing.SOUTH:Facing.NORTH);
        }
    }
    private static void bay(FacadeBay bay,OwnedBlockBuffer b) {
        Window w=bay.window();Box a=w.opening();Facing out=w.outward();String id=w.moduleId();
        int tx=out.dz==0?0:1,tz=out.dx==0?0:1;
        int cx=(a.minX()+a.maxX())/2,cz=(a.minZ()+a.maxZ())/2;
        // One-block reveal with jambs, continuous sill and a stepped hood. Glazing remains recessed.
        for(int side:new int[]{-2,2})for(int y=a.minY()-1;y<=a.maxY();y++)
            b.put(id,cx+tx*side+out.dx,y,cz+tz*side+out.dz,Kind.TRIM);
        for(int t=-2;t<=2;t++) {
            b.put(id,cx+tx*t+out.dx,a.minY()-1,cz+tz*t+out.dz,Kind.STONE_SLAB);
            int h=a.maxY()+1+(bay.style()==BayStyle.HALL_LANCET&&Math.abs(t)<2?1:0);
            b.put(id,cx+tx*t+out.dx,h,cz+tz*t+out.dz,Kind.TRIM);
            if(h>a.maxY()+1)b.put(id,cx+tx*t,a.maxY()+1,cz+tz*t,Kind.TRIM);
        }
    }
    private static void hallStructure(ArchitectureModel model,OwnedBlockBuffer b) {
        var hall=model.module("hall");Box a=hall.bounds();
        // Buttresses are between the window bays; capitals align with the cornice and roof trusses.
        for(int x:new int[]{-18,-12,-6,0,6,12})for(int z:new int[]{a.minZ()-1,a.maxZ()+1}) {
            if(x==0&&z==a.maxZ()+1)continue; // principal portal
            for(int y=0;y<=12;y++)b.put("hall",x,y,z,y==0||y==4||y==11?Kind.TRIM:Kind.STONE);
            b.put("hall",x,13,z,Kind.STONE_SLAB);
        }
        for(int x=a.minX()+1;x<a.maxX();x++)for(int z:new int[]{a.minZ()-1,a.maxZ()+1})
            b.put("hall",x,14,z,Kind.TIMBER,Facing.EAST);
        // A clearly framed main portal, recessed into the facade, with a small stone canopy.
        for(int x:new int[]{-3,3})for(int y=0;y<=6;y++)b.put("hall",x,y,-10,Kind.TRIM);
        for(int x=-3;x<=3;x++)b.put("hall",x,7,-10,Kind.STONE_SLAB);
        for(int x=-2;x<=2;x++)b.put("hall",x,6,-11,Kind.TIMBER,Facing.EAST);
        // Hearth flue crosses the roof as a deliberately owned masonry junction.
        for(int x=12;x<=13;x++)for(int z=-27;z<=-26;z++) {
            for(int y=6;y<=22;y++)b.put("hall",x,y,z,Kind.STONE);
            b.put("hall",x,23,z,Kind.TRIM);
        }
        for(int x:new int[]{12,13})b.put("hall",x,24,-27,Kind.STONE_SLAB);
    }
    private static void court(CourtFeature f,OwnedBlockBuffer b) {
        String id="court-"+f.id();Box a=f.bounds();int cx=(a.minX()+a.maxX())/2,cz=(a.minZ()+a.maxZ())/2;
        for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++)for(int y=1;y<=a.maxY();y++)b.airIfEmpty(id,x,y,z);
        b.box(id,a.minX(),0,a.minZ(),a.maxX(),0,a.maxZ(),Kind.PAVING);
        switch(f.use()) {
            case COVERED_WELL -> {
                for(int x=cx-1;x<=cx+1;x++)for(int z=cz-1;z<=cz+1;z++)
                    b.put(id,x,1,z,x==cx&&z==cz?Kind.ROOF:Kind.TRIM);
                for(int x:new int[]{cx-2,cx+2})for(int y=1;y<=5;y++)b.put(id,x,y,cz,Kind.TIMBER);
                for(int x=cx-2;x<=cx+2;x++)b.put(id,x,5,cz,Kind.TIMBER,Facing.EAST);
                b.put(id,cx,4,cz,Kind.TIMBER); // stylized covered winch, no fluid or moving block
                for(int x=a.minX();x<=a.maxX();x++)for(int z=cz-2;z<=cz+2;z++) {
                    int h=5+(2-Math.abs(z-cz));
                    b.put(id,x,h-1,z,Kind.ROOF);
                    b.put(id,x,h,z,z==cz?Kind.ROOF_SLAB:Kind.ROOF_STAIRS,z==cz?Facing.NONE:z<cz?Facing.SOUTH:Facing.NORTH);
                }
            }
            case HERB_GARDEN -> {
                for(int x=a.minX();x<=a.maxX();x++)for(int z=a.minZ();z<=a.maxZ();z++) {
                    boolean edge=x==a.minX()||x==a.maxX()||z==a.minZ()||z==a.maxZ();
                    if(edge)b.put(id,x,1,z,Kind.STONE_SLAB);
                    else if(x!=cx&&z!=cz)b.put(id,x,0,z,Kind.GARDEN);
                }
                for(int x:new int[]{a.minX()+2,a.maxX()-2})b.put(id,x,1,cz,Kind.WOOD_STAIRS,Facing.SOUTH);
            }
            case SEATING_ARBOR -> {
                for(int x:new int[]{a.minX(),a.maxX()})for(int z:new int[]{a.minZ(),a.maxZ()})
                    b.box(id,x,1,z,x,5,z,Kind.TIMBER);
                for(int x=a.minX();x<=a.maxX();x++)for(int z:new int[]{a.minZ(),a.maxZ()})b.put(id,x,5,z,Kind.TIMBER,Facing.EAST);
                for(int x=a.minX();x<=a.maxX();x+=2)for(int z=a.minZ();z<=a.maxZ();z++)b.put(id,x,6,z,Kind.TIMBER,Facing.NORTH);
                for(int x=a.minX()+1;x<a.maxX();x++)b.put(id,x,1,a.maxZ()-1,Kind.WOOD_STAIRS,Facing.SOUTH);
                for(int x:new int[]{a.minX()+2,a.maxX()-2})b.put(id,x,5,cz,Kind.LANTERN);
            }
        }
    }
    private ArchitecturalDetails() {}
}
