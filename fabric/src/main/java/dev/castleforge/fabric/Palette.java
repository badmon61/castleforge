package dev.castleforge.fabric;

import dev.castleforge.core.CastlePlanner.Material;
import dev.castleforge.core.TerracedCastlePlanner;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Vanilla-only prototype palettes. Full cultural building grammars are a later milestone. */
record Palette(Block masonry, Block weathered, Block floor, Block roof) {
    static Palette choose(String culture, boolean sandyGround) {
        if (sandyGround && !culture.equals("high_fantasy"))
            return new Palette(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.TERRACOTTA);
        return switch (culture) {
            case "nordic" -> new Palette(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_PLANKS);
            case "mediterranean", "desert_citadel" -> new Palette(Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.TERRACOTTA);
            case "east_asian" -> new Palette(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.RED_NETHER_BRICKS);
            case "gothic" -> new Palette(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES);
            case "high_fantasy" -> new Palette(Blocks.QUARTZ_BRICKS, Blocks.CALCITE, Blocks.SMOOTH_QUARTZ, Blocks.PURPUR_BLOCK);
            case "maritime" -> new Palette(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_PRISMARINE);
            default -> new Palette(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS);
        };
    }
    BlockState resolve(Material material) {
        return switch (material) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case FOUNDATION, MASONRY -> masonry.defaultBlockState();
            case WEATHERED -> weathered.defaultBlockState();
            case FLOOR -> floor.defaultBlockState();
            case ROOF -> roof.defaultBlockState();
            case GLASS -> Blocks.GLASS.defaultBlockState();
            case LIGHT -> Blocks.GLOWSTONE.defaultBlockState();
            case LADDER_SOUTH -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        };
    }
    BlockState resolve(TerracedCastlePlanner.Block block) {
        Direction facing = Direction.valueOf(block.facing().name());
        return switch (block.kind()) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case FOUNDATION, WALL -> masonry.defaultBlockState();
            case TRIM -> decorativeTrim(masonry).defaultBlockState();
            case WEATHERED -> weathered.defaultBlockState();
            case FLOOR -> floor.defaultBlockState();
            case ROOF -> roof.defaultBlockState();
            case TIMBER -> decorativeTimber(floor).defaultBlockState();
            case GLASS -> Blocks.GLASS.defaultBlockState();
            case LIGHT -> Blocks.GLOWSTONE.defaultBlockState();
            case STAIR -> stairs(masonry).defaultBlockState().setValue(StairBlock.FACING, facing);
            case ROOF_STAIR -> stairs(roof).defaultBlockState().setValue(StairBlock.FACING, facing);
            case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
        };
    }
    private static Block decorativeTrim(Block wall) {
        if (wall == Blocks.SANDSTONE || wall == Blocks.CUT_SANDSTONE) return Blocks.CHISELED_SANDSTONE;
        if (wall == Blocks.COBBLESTONE) return Blocks.POLISHED_ANDESITE;
        if (wall == Blocks.DEEPSLATE_BRICKS) return Blocks.POLISHED_DEEPSLATE;
        if (wall == Blocks.QUARTZ_BRICKS) return Blocks.QUARTZ_PILLAR;
        return Blocks.CHISELED_STONE_BRICKS;
    }
    private static Block decorativeTimber(Block flooring) {
        if (flooring == Blocks.SPRUCE_PLANKS) return Blocks.STRIPPED_SPRUCE_LOG;
        if (flooring == Blocks.DARK_OAK_PLANKS) return Blocks.STRIPPED_DARK_OAK_LOG;
        if (flooring == Blocks.OAK_PLANKS) return Blocks.STRIPPED_OAK_LOG;
        if (flooring == Blocks.SMOOTH_SANDSTONE) return Blocks.STRIPPED_ACACIA_LOG;
        return Blocks.STRIPPED_SPRUCE_LOG;
    }
    private static Block stairs(Block block) {
        if (block == Blocks.SANDSTONE || block == Blocks.TERRACOTTA) return Blocks.SANDSTONE_STAIRS;
        if (block == Blocks.COBBLESTONE) return Blocks.COBBLESTONE_STAIRS;
        if (block == Blocks.DARK_OAK_PLANKS) return Blocks.DARK_OAK_STAIRS;
        if (block == Blocks.SPRUCE_PLANKS) return Blocks.SPRUCE_STAIRS;
        if (block == Blocks.RED_NETHER_BRICKS) return Blocks.RED_NETHER_BRICK_STAIRS;
        if (block == Blocks.DEEPSLATE_TILES) return Blocks.DEEPSLATE_TILE_STAIRS;
        if (block == Blocks.DEEPSLATE_BRICKS) return Blocks.DEEPSLATE_BRICK_STAIRS;
        if (block == Blocks.QUARTZ_BRICKS) return Blocks.QUARTZ_STAIRS;
        if (block == Blocks.PURPUR_BLOCK) return Blocks.PURPUR_STAIRS;
        if (block == Blocks.DARK_PRISMARINE) return Blocks.DARK_PRISMARINE_STAIRS;
        return Blocks.STONE_BRICK_STAIRS;
    }

}
