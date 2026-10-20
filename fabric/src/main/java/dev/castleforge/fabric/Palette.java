package dev.castleforge.fabric;

import dev.castleforge.core.CastlePlanner.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
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
}
