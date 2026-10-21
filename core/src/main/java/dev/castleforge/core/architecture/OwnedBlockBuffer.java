package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import java.util.*;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Construction phases retain ownership; no cross-module last-write-wins. */
final class OwnedBlockBuffer {
    private record Claim(String owner,ReferenceBlock block) {}
    private final Map<Pos,Claim> claims=new LinkedHashMap<>();
    private final Set<Pos> clearance;
    OwnedBlockBuffer(Set<Pos> clearance){this.clearance=clearance;}
    ReferenceBlock at(Pos p){Claim c=claims.get(p);return c==null?null:c.block;}
    boolean occupiedByOther(String owner,Pos p){Claim c=claims.get(p);return c!=null&&!c.owner.equals(owner)&&c.block.kind()!=Kind.AIR;}
    void put(String owner,int x,int y,int z,Kind kind){put(owner,x,y,z,kind,Facing.NONE);}
    void put(String owner,int x,int y,int z,Kind kind,Facing facing){
        ReferenceCastlePlanner.interrupted();
        Pos p=new Pos(x,y,z);
        if(kind!=Kind.AIR&&clearance.contains(p))throw new IllegalArgumentException("Reserved route collision "+owner+" at "+p);
        Claim old=claims.get(p);
        // Empty outside air is deliberately ownerless. The common courtyard slab is an explicit base interface.
        if(old!=null&&!old.owner.equals(owner)&&old.block.kind()!=Kind.AIR&&kind!=Kind.AIR
                &&!(old.owner.equals("site")&&y<=0))
            throw new IllegalArgumentException("Module collision "+old.owner+" / "+owner+" at "+p);
        if(old!=null&&!old.owner.equals(owner)&&old.block.kind()!=Kind.AIR&&kind==Kind.AIR)
            throw new IllegalArgumentException("Cross-owner carving "+owner+" at "+p+" owned by "+old.owner);
        claims.put(p,new Claim(owner,new ReferenceBlock(p,kind,facing)));
        if(claims.size()>ReferenceCastlePlanner.MAX_BLOCKS)throw new IllegalArgumentException("Reference block budget exceeded");
    }
    void airIfEmpty(String owner,int x,int y,int z){Pos p=new Pos(x,y,z);if(!claims.containsKey(p))put(owner,x,y,z,Kind.AIR);}
    void box(String owner,int x1,int y1,int z1,int x2,int y2,int z2,Kind kind){
        for(int x=x1;x<=x2;x++)for(int z=z1;z<=z2;z++)for(int y=y1;y<=y2;y++)put(owner,x,y,z,kind);
    }
    List<ReferenceBlock> finish(){return claims.values().stream().map(Claim::block).sorted(Comparator
        .comparingInt((ReferenceBlock b)->b.kind()==Kind.LANTERN?1:0)
        .thenComparingInt(b->b.pos().y()).thenComparingInt(b->b.pos().z()).thenComparingInt(b->b.pos().x())).toList();}
}
