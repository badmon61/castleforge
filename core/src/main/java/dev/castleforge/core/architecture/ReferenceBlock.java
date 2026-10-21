package dev.castleforge.core.architecture;

import dev.castleforge.core.CastlePlanner.Pos;
import java.util.Objects;

/** A deliberately small, block-entity-free palette contract. All stairs/slabs use bottom half. */
public record ReferenceBlock(Pos pos, Kind kind, Facing facing) {
    public enum Kind { AIR, FOUNDATION, STONE, TRIM, TIMBER, PLANKS, ROOF, GLASS, BOOKSHELF, WOOL,
        STONE_STAIRS, WOOD_STAIRS, ROOF_STAIRS, SLAB, ROOF_SLAB, LANTERN,
        PLASTER, PAVING, GARDEN, TABLE_TOP, STONE_SLAB }
    public enum Facing {
        NONE(0,0), NORTH(0,-1), EAST(1,0), SOUTH(0,1), WEST(-1,0);
        public final int dx, dz;
        Facing(int dx,int dz) { this.dx=dx;this.dz=dz; }
        public Facing rotate(int turns) { return this==NONE ? this : values()[1+Math.floorMod(ordinal()-1+turns,4)]; }
    }
    public ReferenceBlock { Objects.requireNonNull(pos); Objects.requireNonNull(kind); Objects.requireNonNull(facing); }
    public ReferenceBlock(Pos pos,Kind kind) { this(pos,kind,Facing.NONE); }
    public boolean stair() { return kind==Kind.STONE_STAIRS || kind==Kind.WOOD_STAIRS || kind==Kind.ROOF_STAIRS; }
    public boolean fullCube() { return switch(kind) {
        case AIR,STONE_STAIRS,WOOD_STAIRS,ROOF_STAIRS,SLAB,ROOF_SLAB,LANTERN,TABLE_TOP,STONE_SLAB -> false;
        default -> true;
    }; }
}
