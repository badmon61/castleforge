package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.TerracedCastlePlanner.*;

/** Bounded, immutable summary for operator previews and architecture regression checks. */
public final class PlanDiagnostics {
    public record Report(int width, int length, int height, int surveyedCells,
                         int plannedCells, int solids, int emptyCells, int foundationCells,
                         int weatheredCells, int stairs, int bridges, int modules,
                         int storeys, int navigationTargets, int terraceRelief) {
        public String compact() {
            return width + "x" + length + "x" + height + " blocks; "
                + modules + " buildings / " + storeys + " floors; "
                + bridges + " bridge links; " + stairs + " stairs; "
                + navigationTargets + " walkable targets; "
                + foundationCells + " foundation blocks";
        }
    }
    public static Report analyze(Plan plan) {
        Objects.requireNonNull(plan);
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        int air=0, foundations=0, weathered=0, stairs=0;
        Set<CastlePlanner.Pos> coordinates = new HashSet<>();
        for (Block b : plan.blocks()) {
            var p=b.pos();
            if(!coordinates.add(p)) throw new IllegalArgumentException("Duplicate architecture position");
            minX=Math.min(minX,p.x()); maxX=Math.max(maxX,p.x());
            minY=Math.min(minY,p.y()); maxY=Math.max(maxY,p.y());
            minZ=Math.min(minZ,p.z()); maxZ=Math.max(maxZ,p.z());
            switch (b.kind()) {
                case AIR -> air++;
                case FOUNDATION -> foundations++;
                case WEATHERED -> weathered++;
                case STAIR, ROOF_STAIR -> stairs++;
                default -> {}
            }
        }
        if (coordinates.isEmpty()) throw new IllegalArgumentException("No building geometry");
        int storeys=plan.rooms().stream().mapToInt(Room::storeys).sum();
        int bridges=(int)plan.links().stream().filter(Link::bridge).count();
        return new Report(maxX-minX+1,maxZ-minZ+1,maxY-minY+1,
            (2*TerracedCastlePlanner.RADIUS+1)*(2*TerracedCastlePlanner.RADIUS+1),
            plan.blocks().size(),plan.blocks().size()-air,air,foundations,weathered,
            stairs,bridges,plan.rooms().size(),storeys,plan.targets().size(),plan.relief());
    }
    private PlanDiagnostics() {}
}
