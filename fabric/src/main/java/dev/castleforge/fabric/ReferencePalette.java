package dev.castleforge.fabric;

import dev.castleforge.core.architecture.ReferenceBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/** One authored Central European material study; culture geography is deliberately independent. */
final class ReferencePalette {
    static BlockState resolve(ReferenceBlock block) {
        return switch (block.kind()) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case FOUNDATION -> Blocks.COBBLESTONE.defaultBlockState();
            case STONE -> Blocks.STONE_BRICKS.defaultBlockState();
            case TRIM -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case TIMBER -> Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,
                switch (block.facing()) {
                    case EAST, WEST -> Direction.Axis.X;
                    case NORTH, SOUTH -> Direction.Axis.Z;
                    case NONE -> Direction.Axis.Y;
                });
            case PLANKS -> Blocks.SPRUCE_PLANKS.defaultBlockState();
            case ROOF -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case GLASS -> Blocks.GLASS.defaultBlockState();
            case BOOKSHELF -> Blocks.BOOKSHELF.defaultBlockState();
            case WOOL -> Blocks.RED_WOOL.defaultBlockState();
            case PLASTER -> Blocks.CALCITE.defaultBlockState();
            case PAVING -> Blocks.ANDESITE.defaultBlockState();
            case GARDEN -> Blocks.MOSS_BLOCK.defaultBlockState();
            case TABLE_TOP -> Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
            case STONE_SLAB -> Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case STONE_STAIRS -> Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing(block));
            case WOOD_STAIRS -> Blocks.SPRUCE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing(block));
            case ROOF_STAIRS -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing(block));
            case SLAB -> Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case ROOF_SLAB -> Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            case LANTERN -> Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        };
    }

    private static Direction facing(ReferenceBlock block) {
        if (block.facing() == ReferenceBlock.Facing.NONE)
            throw new IllegalArgumentException("Directional reference block has no facing: " + block);
        return Direction.valueOf(block.facing().name());
    }

    private ReferencePalette() {}
}
