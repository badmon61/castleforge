package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import dev.castleforge.core.architecture.ReferenceBlock.Facing;
import java.util.*;

/** Canonical local coordinates: model precedes all voxel placement. Floor Y denotes supporting block. */
public record ArchitectureModel(String variant, List<Module> modules, List<Room> rooms,
        List<Connection> connections,List<StairFlight> stairs,List<Window> windows,
        Set<Pos> clearance,Set<Pos> circulation,Pos entrance,
        List<Partition> partitions,List<FacadeBay> facadeBays,List<CourtFeature> courtFeatures,List<Dormer> dormers,
        List<WallWalk> wallWalks) {
    public ArchitectureModel {
        modules=List.copyOf(modules);rooms=List.copyOf(rooms);connections=List.copyOf(connections);
        stairs=List.copyOf(stairs);windows=List.copyOf(windows);
        clearance=Collections.unmodifiableSet(new LinkedHashSet<>(clearance));
        circulation=Collections.unmodifiableSet(new LinkedHashSet<>(circulation));
        partitions=List.copyOf(partitions);facadeBays=List.copyOf(facadeBays);courtFeatures=List.copyOf(courtFeatures);
        dormers=List.copyOf(dormers);
        wallWalks=List.copyOf(wallWalks);
    }
    public enum ModuleType { KEEP, GREAT_HALL, RESIDENCE, SERVICE_WING, GATEHOUSE, CORNER_TOWER, CURTAIN, WALL_STAIR }
    public enum RoomType { GREAT_HALL, KITCHEN, BEDCHAMBER, LIBRARY, STORE, GUARDROOM, STAIR_HALL, SOLAR }
    public enum RoofType { GABLE_X, GABLE_Z, HIP, BATTLEMENT }
    public record Box(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public Box { if(minX>maxX||minY>maxY||minZ>maxZ)throw new IllegalArgumentException("Inverted box"); }
        public boolean contains(Pos p) { return p.x()>=minX&&p.x()<=maxX&&p.y()>=minY&&p.y()<=maxY&&p.z()>=minZ&&p.z()<=maxZ; }
        public boolean overlaps(Box b) { return minX<=b.maxX&&maxX>=b.minX&&minY<=b.maxY&&maxY>=b.minY&&minZ<=b.maxZ&&maxZ>=b.minZ; }
        public int width(){return maxX-minX+1;} public int depth(){return maxZ-minZ+1;}
    }
    public record Module(String id,ModuleType type,Box bounds,List<Integer> floors,RoofType roof,int chamfer) {
        public Module { floors=List.copyOf(floors); }
        public boolean footprint(int x,int z) {
            if(x<bounds.minX||x>bounds.maxX||z<bounds.minZ||z>bounds.maxZ)return false;
            int edgeX=Math.min(x-bounds.minX,bounds.maxX-x),edgeZ=Math.min(z-bounds.minZ,bounds.maxZ-z);
            return edgeX+edgeZ>=chamfer;
        }
        public boolean inside(int x,int z) { return footprint(x,z)&&footprint(x-1,z)&&footprint(x+1,z)&&footprint(x,z-1)&&footprint(x,z+1); }
    }
    public record Room(String id,String moduleId,RoomType type,Box interior,int floorY,Pos target) {}
    /** Explicit owner pair and air aperture, including a two-block-thick shared junction where necessary. */
    public record Connection(String id,String from,String to,Box aperture) {}
    /** Treads are ordered bottom-to-top, each list contains the full usable tread width. */
    public record StairFlight(String id,String moduleId,Facing facing,List<List<Pos>> treads) {
        public StairFlight { treads=treads.stream().map(List::copyOf).toList(); }
    }
    public record Window(String moduleId,Box opening,Facing outward) {}
    /** A real interior wall; its open portals are reserved before furnishing, not carved afterwards. */
    public record Partition(String id,String moduleId,Box wall,List<Box> portals) {
        public Partition { portals=List.copyOf(portals); }
    }
    public enum BayStyle { HALL_LANCET, CHAMBER_CASEMENT }
    /** Planned facade depth and a declared window, independently inspectable before emission. */
    public record FacadeBay(String id,Window window,BayStyle style,Box envelope) {}
    public enum CourtUse { COVERED_WELL, HERB_GARDEN, SEATING_ARBOR }
    public record CourtFeature(String id,CourtUse use,Box bounds) {}
    /** Small east-facing roof light, with an explicit owned roof junction; not a declared habitable room. */
    public record Dormer(String id,String moduleId,int frontX,int centerZ,int baseY) {}
    /** Explicit deck/support cells and parapets/merlons. Routes remain part of global circulation. */
    public record WallWalk(String id,String moduleId,Set<Pos> deck,Set<Pos> guards) {
        public WallWalk {
            deck=Collections.unmodifiableSet(new LinkedHashSet<>(deck));
            guards=Collections.unmodifiableSet(new LinkedHashSet<>(guards));
        }
    }
    public Module module(String id) { return modules.stream().filter(m->m.id.equals(id)).findFirst().orElseThrow(); }
}
