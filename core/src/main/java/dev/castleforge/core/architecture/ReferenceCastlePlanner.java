package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner;
import dev.castleforge.core.CastlePlanner.Pos;
import dev.castleforge.core.CultureMap;
import java.util.*;
import java.util.concurrent.CancellationException;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Bounded, immutable model -> owned geometry -> furnishings -> independent quality validation. */
public final class ReferenceCastlePlanner {
    public static final int VERSION=3,RADIUS=54,MAX_BLOCKS=200_000,MAX_RELIEF=3;
    public record Plan(List<ReferenceBlock> blocks,ArchitectureModel architecture,QualityReport report,
            int floorY,int relief,int quarterTurns,CultureMap.Region region,int originX,int originZ) {
        public Plan { blocks=List.copyOf(blocks); }
        public Pos toWorld(Pos p){Pos r=rotate(p,quarterTurns);return new Pos(originX+r.x(),floorY+r.y(),originZ+r.z());}
        public Pos toLocal(Pos p){return rotate(new Pos(p.x()-originX,p.y()-floorY,p.z()-originZ),-quarterTurns);}
        public List<ReferenceBlock> localBlocks(){return sort(blocks.stream().map(b->new ReferenceBlock(toLocal(b.pos()),b.kind(),b.facing().rotate(-quarterTurns))).toList());}
    }
    public static Plan plan(long seed,int x,int z,CastlePlanner.Ground ground,CultureMap.Region region){
        Objects.requireNonNull(ground);Objects.requireNonNull(region);
        if(Math.abs((long)x)>29_999_800||Math.abs((long)z)>29_999_800)throw new IllegalArgumentException("Reference site outside supported coordinates");
        long detailSeed=CultureMap.hash(seed^0x5245464552454e31L,x,z);
        int turns=Math.floorMod(detailSeed,4),side=RADIUS*2+1;
        int[][] heights=new int[side][side];int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int dx=-RADIUS;dx<=RADIUS;dx++)for(int dz=-RADIUS;dz<=RADIUS;dz++){
            interrupted();Pos r=rotate(new Pos(dx,0,dz),turns);
            int height=ground.surfaceY(x+r.x(),z+r.z());
            if(height< -1024||height>4096)throw new IllegalArgumentException("Reference unsuitable ground at "+(x+r.x())+","+(z+r.z())+" (dry known surface required)");
            heights[dx+RADIUS][dz+RADIUS]=height;min=Math.min(min,height);max=Math.max(max,height);
        }
        if(max-min>MAX_RELIEF)throw new IllegalArgumentException("Reference terrain relief "+(max-min)+" exceeds "+MAX_RELIEF+" blocks; use a flat or gentle test site");
        ArchitectureModel model=ReferenceLayout.create();QualityChecks.validateModel(model);
        OwnedBlockBuffer b=new OwnedBlockBuffer(model.clearance());
        int floor=max;
        for(int dx=-49;dx<=49;dx++)for(int dz=-40;dz<=40;dz++){
            final int px=dx,pz=dz;
            boolean court=dx>=-46&&dx<=46&&dz>=-37&&dz<=37;
            if(!court&&model.modules().stream().noneMatch(m->m.footprint(px,pz)))continue;
            int start=heights[dx+RADIUS][dz+RADIUS]-floor;
            for(int y=start;y<0;y++)b.put("site",dx,y,dz,Kind.FOUNDATION);
            boolean processional=Math.abs(dx)<=3||Math.abs(dz-15)<=2;
            b.put("site",dx,0,dz,processional?Kind.TRIM:Math.floorMod(dx,7)==0||Math.floorMod(dz,7)==0?Kind.STONE:Kind.PAVING);
        }
        ModuleLibrary.build(model,b);
        InteriorDesigner.furnish(model,b,detailSeed);
        // The approach drops at most one tread per row and never excavates sampled terrain.
        int previous=0;
        for(int dz=41;dz<=52;dz++){
            int topGround=Integer.MIN_VALUE;
            for(int dx=-2;dx<=2;dx++)topGround=Math.max(topGround,heights[dx+RADIUS][dz+RADIUS]-floor-1);
            int top=Math.max(topGround,previous-1);
            for(int dx=-2;dx<=2;dx++){
                int start=heights[dx+RADIUS][dz+RADIUS]-floor;
                // Descending stairs occupy the HIGH row's block layer: two half-block drops.
                int tread=top<previous?top+1:top;
                boolean descent=top<previous;
                // A common-width landing may stand above one outer lane's terrain. Taper that lane
                // into the natural outside row, without replacing its existing ground block.
                int outside=heights[dx+RADIUS][53+RADIUS]-floor-1;
                if(dz==52&&top==previous&&outside==top-1&&tread>=start)descent=true;
                for(int y=start;y<tread;y++)b.put("approach",dx,y,dz,Kind.FOUNDATION);
                if(tread>=start)b.put("approach",dx,tread,dz,descent?Kind.STONE_STAIRS:Kind.STONE,descent?Facing.NORTH:Facing.NONE);
                for(int y=top+1;y<=Math.max(3,top+3);y++)b.airIfEmpty("approach",dx,y,dz);
            }
            previous=top;
        }
        for(int dx=-2;dx<=2;dx++)for(int y=heights[dx+RADIUS][53+RADIUS]-floor;y<=3;y++)b.airIfEmpty("approach",dx,y,53);
        verifyApproach(b,heights,floor);
        List<ReferenceBlock> local=b.finish();QualityReport report=QualityChecks.verify(model,local);
        final int base=floor;
        List<ReferenceBlock> world=sort(local.stream().map(block->{Pos r=rotate(block.pos(),turns);
            return new ReferenceBlock(new Pos(x+r.x(),base+r.y(),z+r.z()),block.kind(),block.facing().rotate(turns));}).toList());
        return new Plan(world,model,report,floor,max-min,turns,region,x,z);
    }
    private static void verifyApproach(OwnedBlockBuffer b,int[][] heights,int floor){
        for(int x=-2;x<=2;x++){
            Integer previous=null;
            for(int z=53;z>=40;z--)for(boolean north:new boolean[]{false,true}){
                int surface=2*(heights[x+RADIUS][z+RADIUS]-floor);
                for(int y=-MAX_RELIEF-1;y<=0;y++){
                    ReferenceBlock block=b.at(new Pos(x,y,z));
                    if(block==null||block.kind()==Kind.AIR)continue;
                    if(block.kind()==Kind.STONE_STAIRS)surface=Math.max(surface,2*y+(north?2:1));
                    else if(block.fullCube())surface=Math.max(surface,2*(y+1));
                }
                if(previous!=null&&Math.abs(surface-previous)>1)
                    throw new IllegalArgumentException("Reference entrance needs a terrain step larger than half a block at local "+x+","+z+"; choose a gentler approach");
                previous=surface;
            }
        }
    }
    static List<ReferenceBlock> sort(List<ReferenceBlock> blocks){return blocks.stream().sorted(Comparator
        .comparingInt((ReferenceBlock b)->b.kind()==Kind.LANTERN?1:0).thenComparingInt(b->b.pos().y())
        .thenComparingInt(b->b.pos().z()).thenComparingInt(b->b.pos().x())).toList();}
    static Pos rotate(Pos p,int turns){return switch(Math.floorMod(turns,4)){
        case 0->p;case 1->new Pos(-p.z(),p.y(),p.x());case 2->new Pos(-p.x(),p.y(),-p.z());default->new Pos(p.z(),p.y(),-p.x());};}
    static void interrupted(){if(Thread.currentThread().isInterrupted())throw new CancellationException("Reference planning cancelled");}
    private ReferenceCastlePlanner(){}
}
