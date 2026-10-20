package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.CastlePlanner.Pos;
import static dev.castleforge.core.TerracedCastlePlanner.*;

/** Conservative full-cube walking model, NOT Minecraft physics/entity pathfinding. */
public final class PlanNavigation {
    private static final int MAX_VISITED=100_000;
    public record Result(int reached,int targets) {}
    public static Result verify(Plan plan,Ground ground) {
        Map<Pos,Block> blocks=new HashMap<>();
        for(Block block:plan.blocks()) {
            if(blocks.put(block.pos(),block)!=null) throw new IllegalArgumentException("Duplicate planned position");
            if(block.pos().y()<ground.surfaceY(block.pos().x(),block.pos().z()))
                throw new IllegalArgumentException("Planned excavation");
        }
        Space space=new Space(plan,ground,blocks);
        for(Block block:plan.blocks()) if(block.kind()==Kind.LADDER) {
            Pos p=block.pos(); Facing d=block.facing();
            if(space.clear(new Pos(p.x()-d.dx,p.y(),p.z()-d.dz)))
                throw new IllegalArgumentException("Unsupported ladder at "+p);
        }
        if(!space.stand(plan.entrance())) throw new IllegalArgumentException("Entrance is not walkable");
        Set<Pos> remaining=new HashSet<>();
        for(Target target:plan.targets()) {
            if(!space.stand(target.feet())) throw new IllegalArgumentException("Blocked room target: "+target.id()+" at "+target.feet());
            remaining.add(target.feet());
        }
        ArrayDeque<Pos> todo=new ArrayDeque<>(); Set<Pos> seen=new HashSet<>();
        todo.add(plan.entrance()); seen.add(plan.entrance()); remaining.remove(plan.entrance());
        while(!todo.isEmpty() && !remaining.isEmpty()) {
            Pos p=todo.removeFirst();
            for(Facing d:Facing.values()) for(int dy=-1;dy<=1;dy++) {
                Pos next=new Pos(p.x()+d.dx,p.y()+dy,p.z()+d.dz);
                if(dy>0 && !space.clear(new Pos(p.x(),p.y()+2,p.z()))) continue;
                if(dy<0 && !space.clear(new Pos(next.x(),next.y()+2,next.z()))) continue;
                visit(space,next,todo,seen,remaining);
            }
            if(space.ladder(p) || space.ladder(new Pos(p.x(),p.y()-1,p.z())))
                for(int dy:new int[]{-1,1}) visit(space,new Pos(p.x(),p.y()+dy,p.z()),todo,seen,remaining);
            if(seen.size()>MAX_VISITED) throw new IllegalArgumentException("Navigation budget exceeded");
        }
        if(!remaining.isEmpty()) {
            String names=plan.targets().stream().filter(t->remaining.contains(t.feet())).map(Target::id).toList().toString();
            throw new IllegalArgumentException("Unreachable architecture targets: "+names);
        }
        return new Result(seen.size(),plan.targets().size());
    }
    private static void visit(Space s,Pos p,ArrayDeque<Pos> todo,Set<Pos> seen,Set<Pos> remaining) {
        if(s.inBounds(p) && !seen.contains(p) && s.stand(p)) { seen.add(p); todo.add(p); remaining.remove(p); }
    }
    private record Space(Plan plan,Ground ground,Map<Pos,Block> blocks) {
        boolean inBounds(Pos p) {
            return Math.abs((long)p.x()-plan.originX())<=RADIUS && Math.abs((long)p.z()-plan.originZ())<=RADIUS
                && p.y()>=-1024 && p.y()<=4200;
        }
        Kind kind(Pos p) {
            Block block=blocks.get(p); if(block!=null) return block.kind();
            int h=ground.surfaceY(p.x(),p.z());
            return h==UNUSABLE || p.y()<h?Kind.FOUNDATION:Kind.AIR;
        }
        boolean clear(Pos p) { Kind k=kind(p); return k==Kind.AIR || k==Kind.LADDER; }
        boolean ladder(Pos p) { return kind(p)==Kind.LADDER; }
        boolean stand(Pos p) {
            Pos below=new Pos(p.x(),p.y()-1,p.z());
            return clear(p) && clear(new Pos(p.x(),p.y()+1,p.z())) && (!clear(below)||ladder(p)||ladder(below));
        }
    }
    private PlanNavigation() {}
}
