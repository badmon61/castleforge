package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import java.util.*;
import static dev.castleforge.core.architecture.ArchitectureModel.*;
import static dev.castleforge.core.architecture.ReferenceBlock.*;

/** Emits the modeled defensive circuit under the existing module ownership rules. */
final class WallWalkLibrary {
    static void build(ArchitectureModel model,OwnedBlockBuffer b){
        Set<Pos> treads=new HashSet<>();
        for(StairFlight stair:model.stairs())for(var row:stair.treads())treads.addAll(row);
        for(WallWalk walk:model.wallWalks()){
            var module=model.module(walk.moduleId());
            boolean masonry=module.type()==ModuleType.CURTAIN||module.type()==ModuleType.WALL_STAIR;
            for(Pos p:walk.deck()){
                if(masonry)for(int y=1;y<p.y();y++)
                    if(!model.clearance().contains(new Pos(p.x(),y,p.z())))b.put(module.id(),p.x(),y,p.z(),Kind.STONE);
                if(!treads.contains(p))b.put(module.id(),p.x(),p.y(),p.z(),masonry?Kind.TRIM:Kind.PLANKS);
            }
            for(Pos p:walk.guards()){
                if(masonry)for(int y=1;y<p.y();y++)
                    if(!model.clearance().contains(new Pos(p.x(),y,p.z())))b.put(module.id(),p.x(),y,p.z(),Kind.STONE);
                // At a chamfered tower portal the projecting guard has a one-block corbel,
                // tied laterally into the existing floor instead of a floating corner post.
                Pos below=new Pos(p.x(),p.y()-1,p.z());
                if(!masonry&&(b.at(below)==null||b.at(below).kind()==Kind.AIR))
                    b.put(module.id(),below.x(),below.y(),below.z(),Kind.TRIM);
                b.put(module.id(),p.x(),p.y(),p.z(),Kind.STONE);
            }
        }
    }
    private WallWalkLibrary(){}
}
