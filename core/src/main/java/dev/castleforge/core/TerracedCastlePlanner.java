package dev.castleforge.core;

import java.util.*;
import static dev.castleforge.core.CastlePlanner.Pos;
import static dev.castleforge.core.CastlePlanner.Condition;

/** Bounded, opt-in architecture laboratory. Does not load chunks or mutate a game world. */
public final class TerracedCastlePlanner {
    public static final int VERSION = 1;
    public static final int RADIUS = 52;
    public static final int MAX_BLOCKS = 180_000;
    public static final int MAX_FOUNDATION = 8;
    public static final int MAX_PIER = 12;
    public static final int UNUSABLE = Integer.MIN_VALUE;
    public enum Kind { AIR, FOUNDATION, WALL, WEATHERED, TRIM, TIMBER, FLOOR, ROOF, GLASS, LIGHT, STAIR, ROOF_STAIR, LADDER }
    public enum Facing {
        NORTH(0,-1), EAST(1,0), SOUTH(0,1), WEST(-1,0);
        public final int dx, dz;
        Facing(int dx, int dz) { this.dx=dx; this.dz=dz; }
        public Facing rotate(int quarterTurns) { return values()[Math.floorMod(ordinal()+quarterTurns,4)]; }
    }
    public enum Roof { GABLE, HIP, SPIRE, BATTLEMENT }
    public record Cell(int x, int z) {}
    public record Block(Pos pos, Kind kind, Facing facing) {}
    public record Rect(int x, int z, int rx, int rz) {
        boolean contains(int px,int pz) {
            int dx=Math.abs(px-x), dz=Math.abs(pz-z);
            return dx<=rx && dz<=rz && dx+dz<=rx+rz-2;
        }
        boolean inset(int px,int pz,int margin) {
            for (int a : new int[]{-margin,margin}) for (int b : new int[]{-margin,margin})
                if (!contains(px+a,pz+b)) return false;
            return true;
        }
    }
    /** Rectangles and module positions are LOCAL; block positions and navigation targets are WORLD coordinates. */
    public record Terrace(String id, Rect footprint, int floorY, int maximumFill) {}
    public record Room(String id, String purpose, Rect footprint, int floorY, int storeys, Roof roof, int phase) {}
    public record Target(String id, Pos feet) {}
    public record Link(String id, boolean bridge, List<Pos> feet) {
        public Link { feet=List.copyOf(feet); }
    }
    public record Plan(List<Block> blocks, List<Terrace> terraces, List<Room> rooms, List<Link> links,
                       List<Target> targets, Pos entrance, int quarterTurns, Condition condition,
                       CultureMap.Region region, int originX, int originZ) {
        public Plan {
            blocks=List.copyOf(blocks); terraces=List.copyOf(terraces); rooms=List.copyOf(rooms);
            links=List.copyOf(links); targets=List.copyOf(targets);
        }
        public int relief() { return terraces.stream().mapToInt(Terrace::floorY).max().orElseThrow()
                - terraces.stream().mapToInt(Terrace::floorY).min().orElseThrow(); }
    }
    @FunctionalInterface public interface Ground { int surfaceY(int x,int z); }

    public static Plan plan(long seed,int ox,int oz,Ground ground,CultureMap.Region region) {
        Objects.requireNonNull(ground); Objects.requireNonNull(region);
        if (Math.abs((long)ox)>29_999_800 || Math.abs((long)oz)>29_999_800)
            throw new IllegalArgumentException("Site too near world coordinate limit");
        Survey survey=new Survey(ox,oz,ground);
        long site=CultureMap.hash(seed ^ 0x5445525241434531L,ox,oz);
        Condition condition=Condition.values()[Math.floorMod(CultureMap.mix(site),3)];
        List<Candidate> candidates=new ArrayList<>();
        for (int q=0;q<4;q++) {
            try { candidates.add(candidate(site,survey,q)); }
            catch (IllegalArgumentException ignored) { /* Try other orientations without writes. */ }
        }
        candidates.sort(Comparator.comparingLong(Candidate::score).thenComparingInt(Candidate::turns));
        String reason="No orientation fits dry terrain within the foundation limit";
        for (Candidate candidate : candidates) {
            try {
                Builder b=new Builder(site,survey,candidate.turns,condition,region,ox,oz);
                Plan result=b.build(candidate.terraces);
                PlanNavigation.verify(result,survey::worldHeight);
                return result;
            } catch (IllegalArgumentException e) { reason=e.getMessage(); }
        }
        throw new IllegalArgumentException("Terraced site rejected: " + reason);
    }
    private record Candidate(int turns,List<Terrace> terraces,long score) {}
    private static Candidate candidate(long seed,Survey s,int q) {
        List<Terrace> terraces=new ArrayList<>(); long score=0;
        int[] centers={28,0,-28}; String[] names={"outer_bailey","residential_court","upper_keep"};
        int[] offsets={-4,5,-2};
        for (int i=0;i<3;i++) {
            Rect r=new Rect(offsets[i]+random(seed,100+i,5)-2,centers[i],21+random(seed,200+i,3),11+random(seed,300+i,2));
            int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
            for (int x=r.x-r.rx;x<=r.x+r.rx;x++) for (int z=r.z-r.rz;z<=r.z+r.rz;z++) if (r.contains(x,z)) {
                int h=s.local(q,x,z); if (h==UNUSABLE) throw new IllegalArgumentException("Unsuitable terrace ground");
                min=Math.min(min,h); max=Math.max(max,h);
            }
            if (max-min>MAX_FOUNDATION) throw new IllegalArgumentException("Local relief exceeds eight-block foundation limit");
            terraces.add(new Terrace(names[i],r,max,max-min));
            for (int x=r.x-r.rx;x<=r.x+r.rx;x++) for (int z=r.z-r.rz;z<=r.z+r.rz;z++)
                if (r.contains(x,z)) score+=max-s.local(q,x,z);
        }
        // Prefer a keep uphill of the entrance, without forcing a fixed compass direction.
        int rise=terraces.get(2).floorY-terraces.get(0).floorY;
        if (Math.abs(rise)>24) throw new IllegalArgumentException("Site relief exceeds the reference design budget");
        score+=Math.max(0,-rise)*10_000L;
        score+=random(seed,400+q,31);
        return new Candidate(q,List.copyOf(terraces),score);
    }
    private static int random(long seed,int salt,int bound) { return Math.floorMod(CultureMap.mix(seed ^ salt),bound); }
    private static Cell rotate(int q,int x,int z) {
        return switch(q) { case 0 -> new Cell(x,z); case 1 -> new Cell(-z,x);
            case 2 -> new Cell(-x,-z); case 3 -> new Cell(z,-x); default -> throw new IllegalArgumentException("Rotation"); };
    }
    private static final class Survey {
        final int ox,oz; final int[][] heights=new int[2*RADIUS+1][2*RADIUS+1];
        Survey(int ox,int oz,Ground ground) {
            this.ox=ox; this.oz=oz;
            for(int x=-RADIUS;x<=RADIUS;x++) for(int z=-RADIUS;z<=RADIUS;z++) {
                int h=ground.surfaceY(ox+x,oz+z);
                if(h!=UNUSABLE && (h< -1024 || h>4096)) throw new IllegalArgumentException("Unsupported terrain height");
                heights[x+RADIUS][z+RADIUS]=h;
            }
        }
        int worldHeight(int x,int z) {
            int dx=x-ox,dz=z-oz;
            if(Math.abs(dx)>RADIUS || Math.abs(dz)>RADIUS) return UNUSABLE;
            return heights[dx+RADIUS][dz+RADIUS];
        }
        int local(int q,int x,int z) { Cell p=rotate(q,x,z); return worldHeight(ox+p.x,oz+p.z); }
    }
    private static final class Builder {
        final long seed; final Survey survey; final int q,ox,oz; final Condition condition; final CultureMap.Region region;
        final Map<Pos,Block> blocks=new HashMap<>();
        final List<Room> rooms=new ArrayList<>(); final List<Target> targets=new ArrayList<>(); final List<Link> links=new ArrayList<>();
        Builder(long seed,Survey survey,int q,Condition condition,CultureMap.Region region,int ox,int oz) {
            this.seed=seed; this.survey=survey; this.q=q; this.condition=condition; this.region=region; this.ox=ox; this.oz=oz;
        }
        int ground(int x,int z) {
            int h=survey.local(q,x,z);
            if(h==UNUSABLE) throw new IllegalArgumentException("A used footprint contains unsuitable terrain");
            return h;
        }
        Pos world(int x,int y,int z) { Cell p=rotate(q,x,z); return new Pos(ox+p.x,y,oz+p.z); }
        void put(int x,int y,int z,Kind kind) { put(x,y,z,kind,Facing.NORTH); }
        void put(int x,int y,int z,Kind kind,Facing facing) {
            if(Math.abs(x)>RADIUS || Math.abs(z)>RADIUS) throw new IllegalArgumentException("Geometry exceeds survey");
            if(y<ground(x,z)) throw new IllegalArgumentException("Plan would excavate natural terrain");
            Pos p=world(x,y,z); blocks.put(p,new Block(p,kind,facing.rotate(q)));
            if(blocks.size()>MAX_BLOCKS) throw new IllegalArgumentException("Architecture block budget exceeded");
        }
        void stone(int x,int y,int z,int phase) {
            int weather=condition==Condition.RESTORED?8:condition==Condition.WEATHERED?36:65;
            weather=Math.max(0,weather-phase*9);
            long patch=CultureMap.hash(seed ^ Math.floorDiv(y,3),Math.floorDiv(x,4),Math.floorDiv(z,4));
            put(x,y,z,Math.floorMod(patch,100)<weather?Kind.WEATHERED:Kind.WALL);
        }
        void target(String id,int x,int y,int z) { targets.add(new Target(id,world(x,y,z))); }
        Plan build(List<Terrace> ts) {
            for(Terrace t:ts) terrace(t);
            Terrace lower=ts.get(0),middle=ts.get(1),upper=ts.get(2);
            Rect a=lower.footprint,b=middle.footprint,c=upper.footprint;
            room(new Room("service_hall","stores_and_kitchen",new Rect(a.x-11,a.z,5,4),lower.floorY,1,roof(false),1),false);
            room(new Room("outer_tower","watchtower",new Rect(a.x+11,a.z,4,4),lower.floorY,2,roof(true),1),true);
            room(new Room("great_hall","residence",new Rect(b.x-11,b.z,6,5),middle.floorY,2,roof(false),1),false);
            room(new Room("gallery","workshop",new Rect(b.x+11,b.z,4,5),middle.floorY,1,Roof.HIP,2),false);
            room(new Room("keep","oldest_core",new Rect(c.x,c.z-1,6,6),upper.floorY,3,roof(false),0),false);
            room(new Room("west_tower","watchtower",new Rect(c.x-14,c.z,3,3),upper.floorY,2,roof(true),0),true);
            room(new Room("east_tower","watchtower",new Rect(c.x+14,c.z,3,3),upper.floorY,2+random(seed,17,2),roof(true),0),true);
            gatehouse(lower);
            connect("lower_to_residence",lower,middle);
            connect("residence_to_keep",middle,upper);
            List<Cell> approach=line(new Cell(a.x,a.z+a.rz),new Cell(a.x,RADIUS-1));
            int end=ground(a.x,RADIUS-1)-1;
            path("entrance_approach",approach,lower.floorY,end);
            Pos entrance=world(a.x,ground(a.x,RADIUS),RADIUS);
            target("outside_entrance",a.x,ground(a.x,RADIUS),RADIUS);
            List<Block> ordered=blocks.values().stream().sorted(Comparator
                .comparingInt((Block v)->v.kind==Kind.LADDER?1:0)
                .thenComparingInt(v->v.pos.y()).thenComparingInt(v->v.pos.z()).thenComparingInt(v->v.pos.x())).toList();
            return new Plan(ordered,ts,rooms,links,targets,entrance,q,condition,region,ox,oz);
        }
        Roof roof(boolean tower) {
            if(region.style().equals("desert_citadel") || region.style().equals("mediterranean")) return Roof.BATTLEMENT;
            if(tower) return region.style().equals("maritime")?Roof.BATTLEMENT:Roof.SPIRE;
            return region.style().equals("high_fantasy")?Roof.HIP:Roof.GABLE;
        }
        void terrace(Terrace t) {
            Rect r=t.footprint; int f=t.floorY;
            for(int x=r.x-r.rx;x<=r.x+r.rx;x++) for(int z=r.z-r.rz;z<=r.z+r.rz;z++) if(r.contains(x,z)) {
                for(int y=ground(x,z);y<f;y++) put(x,y,z,Kind.FOUNDATION);
                put(x,f,z,Kind.FLOOR);
                for(int y=1;y<=6;y++) put(x,f+y,z,Kind.AIR);
                if(!r.inset(x,z,3)) {
                    for(int y=1;y<=4;y++) stone(x,f+y,z,1);
                    if(!r.inset(x,z,1)) {
                        put(x,f+5,z,Kind.TRIM);
                        if(Math.floorMod(x+z,2)==0 && !(condition==Condition.RUINED && random(seed,x*47+z,5)==0))
                            stone(x,f+6,z,0);
                    }
                }
            }
            // Three-wide north/south passages beneath the wall walks.
            for(int z=r.z-r.rz;z<=r.z+r.rz;z++) for(int x=r.x-1;x<=r.x+1;x++)
                for(int y=1;y<=3;y++) put(x,f+y,z,Kind.AIR);
            for(int y=1;y<=4;y++) {
                put(r.x+4,f+y,r.z-r.rz+2,Kind.WALL);
                put(r.x+4,f+y,r.z-r.rz+3,Kind.LADDER,Facing.SOUTH);
            }
            target(t.id+"/court",r.x,f+1,r.z+7);
            target(t.id+"/wall_walk_north",r.x+4,f+5,r.z-r.rz+2);
            target(t.id+"/wall_walk_west",r.x-r.rx+2,f+5,r.z);
            target(t.id+"/wall_walk_east",r.x+r.rx-2,f+5,r.z);
            target(t.id+"/wall_walk_south",r.x+7,f+5,r.z+r.rz-2);
        }
        void room(Room room,boolean octagonal) {
            rooms.add(room); Rect r=room.footprint; int f=room.floorY,h=room.storeys*5;
            for(int x=-r.rx;x<=r.rx;x++) for(int z=-r.rz;z<=r.rz;z++) {
                if(!insideRoom(r,x,z,octagonal)) continue;
                boolean wall=!insideRoom(r,x+1,z,octagonal)||!insideRoom(r,x-1,z,octagonal)
                    ||!insideRoom(r,x,z+1,octagonal)||!insideRoom(r,x,z-1,octagonal);
                for(int y=1;y<=h;y++) {
                    if(wall) {
                        if(y%5==0) put(r.x+x,f+y,r.z+z,Kind.TRIM); else stone(r.x+x,f+y,r.z+z,room.phase);
                    } else put(r.x+x,f+y,r.z+z,y%5==0?Kind.FLOOR:Kind.AIR);
                }
            }
            // Three-block-high doors, deliberately protected from the ageing pass.
            for(int dx=-1;dx<=1;dx++) for(int y=1;y<=3;y++) put(r.x+dx,f+y,r.z+r.rz,Kind.AIR);
            for(int level=0;level<room.storeys;level++) {
                int y=f+level*5;
                for(int z:new int[]{-2,2}) for(int dy=2;dy<=3;dy++) {
                    put(r.x-r.rx,y+dy,r.z+z,condition==Condition.RUINED?Kind.AIR:Kind.GLASS);
                    put(r.x+r.rx,y+dy,r.z+z,condition==Condition.RUINED?Kind.AIR:Kind.GLASS);
                }
                put(r.x+1,y+4,r.z-r.rz,Kind.LIGHT);
                target(room.id+"/floor_"+level,r.x+1,y+1,r.z+1);
            }
            roof(room,octagonal);
            // Backing is installed before all ladders, regardless of rotation/coordinate sort.
            int ladderTop=room.roof==Roof.BATTLEMENT?h+1:(room.storeys-1)*5+1;
            for(int y=1;y<=ladderTop;y++) {
                put(r.x,f+y,r.z-r.rz,Kind.WALL);
                put(r.x,f+y,r.z-r.rz+1,Kind.LADDER,Facing.SOUTH);
            }
            if(room.roof==Roof.BATTLEMENT) target(room.id+"/roof",r.x+1,f+h+2,r.z);
            // Vertical timber accents on the straight hall facades.
            if(!octagonal && room.roof==Roof.GABLE) for(int dx:new int[]{-r.rx,r.rx})
                for(int y=1;y<h;y++) put(r.x+dx,f+y,r.z,Kind.TIMBER);
        }
        boolean insideRoom(Rect r,int x,int z,boolean octagonal) {
            return Math.abs(x)<=r.rx && Math.abs(z)<=r.rz
                && (!octagonal || Math.abs(x)+Math.abs(z)<=r.rx+r.rz-2);
        }
        void roof(Room room,boolean octagonal) {
            Rect r=room.footprint; int base=room.floorY+room.storeys*5+1;
            if(room.roof==Roof.GABLE) {
                for(int dx=-r.rx-1;dx<=r.rx+1;dx++) for(int dz=-r.rz-1;dz<=r.rz+1;dz++) {
                    int rise=r.rx+1-Math.abs(dx);
                    if(Math.abs(dx)<=r.rx && Math.abs(dz)==r.rz)
                        for(int y=0;y<rise;y++) stone(r.x+dx,base+y,r.z+dz,room.phase);
                    put(r.x+dx,base+rise,r.z+dz,dx==0?Kind.ROOF:Kind.ROOF_STAIR,dx<0?Facing.EAST:Facing.WEST);
                }
            } else if(room.roof==Roof.BATTLEMENT) {
                for(int dx=-r.rx;dx<=r.rx;dx++) for(int dz=-r.rz;dz<=r.rz;dz++) if(insideRoom(r,dx,dz,octagonal)) {
                    put(r.x+dx,base,r.z+dz,Kind.FLOOR);
                    if(Math.abs(dx)==r.rx || Math.abs(dz)==r.rz) {
                        put(r.x+dx,base+1,r.z+dz,Kind.TRIM);
                        if(Math.floorMod(dx+dz,2)==0) stone(r.x+dx,base+2,r.z+dz,room.phase);
                    }
                }
            } else {
                int layers=Math.min(r.rx,r.rz)+1;
                int stretch=room.roof==Roof.SPIRE?2:1;
                for(int layer=0;layer<layers;layer++) for(int repeat=0;repeat<stretch;repeat++)
                    for(int dx=-r.rx+layer;dx<=r.rx-layer;dx++) for(int dz=-r.rz+layer;dz<=r.rz-layer;dz++)
                        put(r.x+dx,base+layer*stretch+repeat,r.z+dz,Kind.ROOF);
                if(room.roof==Roof.SPIRE) put(r.x,base+layers*stretch,r.z,Kind.TRIM);
            }
        }
        void gatehouse(Terrace t) {
            Rect r=t.footprint; int cx=r.x,cz=r.z+r.rz-2,f=t.floorY;
            Room gate=new Room("gatehouse","repaired_entrance",new Rect(cx,cz,4,2),f,2,Roof.BATTLEMENT,2);
            room(gate,false);
            // A continuous passage through both faces; side access reaches the upper chamber.
            for(int x=cx-1;x<=cx+1;x++) for(int z=cz-2;z<=cz+2;z++)
                for(int y=1;y<=3;y++) put(x,f+y,z,Kind.AIR);
            for(int y=1;y<=3;y++) put(cx-3,f+y,cz-2,Kind.AIR);
            // Move its access ladder out of the central passage and keep its backing intact.
            for(int y=1;y<=11;y++) {
                put(cx-2,f+y,cz-2,Kind.WALL);
                put(cx-2,f+y,cz-1,Kind.LADDER,Facing.SOUTH);
            }
            for(int y=4;y<=11;y++) put(cx,f+y,cz-1,y==5||y==10||y==11?Kind.FLOOR:Kind.AIR);
            targets.removeIf(tg->tg.id.startsWith("gatehouse/"));
            target("gatehouse/chamber",cx+1,f+6,cz);
            target("gatehouse/roof",cx+1,f+12,cz);
        }
        void connect(String id,Terrace from,Terrace to) {
            Cell start=new Cell(from.footprint.x,from.footprint.z-from.footprint.rz);
            Cell end=new Cell(to.footprint.x,to.footprint.z+to.footprint.rz);
            int middleZ=Math.floorDiv(start.z+end.z,2);
            List<Cell> path=new ArrayList<>();
            append(path,line(start,new Cell(start.x,middleZ)));
            append(path,line(new Cell(start.x,middleZ),new Cell(end.x,middleZ)));
            append(path,line(new Cell(end.x,middleZ),end));
            path(id,path,from.floorY,to.floorY);
        }
        void path(String id,List<Cell> route,int start,int end) {
            int n=route.size(); int[] levels=new int[n];
            for(int i=0;i<n;i++) {
                Cell p=route.get(i); int lower=Integer.MIN_VALUE;
                for(Cell cell:strip(route,i))
                    lower=Math.max(lower,ground(cell.x,cell.z)-(i==n-1 && id.equals("entrance_approach")?1:0));
                int line=(int)Math.round(start+(end-start)*(double)i/(n-1));
                levels[i]=Math.max(lower,line);
            }
            levels[0]=Math.max(levels[0],start); levels[n-1]=Math.max(levels[n-1],end);
            // Minimal slope-limited majorant. No step may rise more than one block.
            for(int i=1;i<n;i++) levels[i]=Math.max(levels[i],levels[i-1]-1);
            for(int i=n-2;i>=0;i--) levels[i]=Math.max(levels[i],levels[i+1]-1);
            if(levels[0]>start || levels[n-1]>end) throw new IllegalArgumentException("A connecting stair cannot meet both terrace gates");
            Map<Cell,Integer> deck=new LinkedHashMap<>();
            Map<Cell,Integer> station=new HashMap<>();
            for(int i=0;i<n;i++) {
                Cell p=route.get(i);
                for(Cell c:strip(route,i)) {
                    deck.merge(c,levels[i],Math::max);
                    station.merge(c,i,Math::min);
                }
            }
            boolean bridge=deck.entrySet().stream().anyMatch(e->e.getValue()-ground(e.getKey().x,e.getKey().z)>=4);
            Set<Cell> centerline=new HashSet<>(route);
            for(var e:deck.entrySet()) {
                Cell p=e.getKey(); int y=e.getValue(),h=ground(p.x,p.z),at=station.get(p);
                if(y-h>MAX_PIER) throw new IllegalArgumentException("Bridge or stair requires overlong supports");
                if(y<h-1) throw new IllegalArgumentException("Path intersects terrain");
                // Natural endpoint blocks are not rewritten. Short beams are supported every five stations.
                if(y>=h) {
                    boolean pier=!bridge || at%5==0 || at<=1 || at>=n-2;
                    if(pier) for(int fy=h;fy<y;fy++) put(p.x,fy,p.z,Kind.FOUNDATION);
                    else if(y-1>=h) put(p.x,y-1,p.z,Kind.TRIM);
                    put(p.x,y,p.z,Kind.FLOOR);
                }
                for(int dy=1;dy<=3;dy++) put(p.x,y+dy,p.z,Kind.AIR);
                // Low parapets only along exposed bridge edges, never across the centre route.
                boolean edge=Arrays.stream(Facing.values()).anyMatch(d->!deck.containsKey(new Cell(p.x+d.dx,p.z+d.dz)));
                if(bridge && edge && !centerline.contains(p) && y-h>=3 && at>2 && at<n-3) put(p.x,y+1,p.z,Kind.TRIM);
            }
            List<Pos> feet=new ArrayList<>();
            for(int i=0;i<n;i++) {
                Cell p=route.get(i); int y=deck.get(p); feet.add(world(p.x,y+1,p.z));
                if(i+1<n) {
                    Cell next=route.get(i+1); int ny=deck.get(next);
                    if(ny==y+1 && y>=ground(p.x,p.z)) {
                        Facing direction=direction(p,next);
                        put(p.x,y,p.z,Kind.STAIR,direction);
                    }
                }
            }
            links.add(new Link(id,bridge,feet));
        }
    }
    private static Set<Cell> strip(List<Cell> route,int index) {
        Cell p=route.get(index);
        Set<Cell> result=new LinkedHashSet<>(); result.add(p);
        if(index>0) addSides(result,p,direction(route.get(index-1),p));
        if(index+1<route.size()) addSides(result,p,direction(p,route.get(index+1)));
        return result;
    }
    private static void addSides(Set<Cell> result,Cell p,Facing tangent) {
        result.add(new Cell(p.x-tangent.dz,p.z+tangent.dx));
        result.add(new Cell(p.x+tangent.dz,p.z-tangent.dx));
    }
    private static Facing direction(Cell a,Cell b) {
        for(Facing d:Facing.values()) if(a.x+d.dx==b.x && a.z+d.dz==b.z) return d;
        throw new IllegalArgumentException("Non-adjacent path cells");
    }
    private static List<Cell> line(Cell a,Cell b) {
        if(a.x!=b.x && a.z!=b.z) throw new IllegalArgumentException("Path segment must be axis aligned");
        List<Cell> result=new ArrayList<>(); int dx=Integer.compare(b.x,a.x),dz=Integer.compare(b.z,a.z);
        for(Cell p=a;;p=new Cell(p.x+dx,p.z+dz)) { result.add(p); if(p.equals(b)) break; }
        return result;
    }
    private static void append(List<Cell> target,List<Cell> part) {
        for(Cell p:part) if(target.isEmpty() || !target.getLast().equals(p)) target.add(p);
    }
    private TerracedCastlePlanner() {}
}
