package com.blockhorizon.player;

import com.blockhorizon.world.BlockType;

import java.util.List;
import java.util.Map;

public final class RecipeBook {
    public static final List<Recipe> RECIPES = List.of(
            new Recipe("木板 ×4", "最常用的建造材料", Map.of(BlockType.WOOD, 1), Map.of(BlockType.PLANKS, 4)),
            new Recipe("石砖 ×2", "规整、坚固的墙体", Map.of(BlockType.STONE, 3), Map.of(BlockType.BRICK, 2)),
            new Recipe("星辉石 ×2", "让家园在夜里发光", Map.of(BlockType.CRYSTAL, 1, BlockType.STONE, 2), Map.of(BlockType.GLOWSTONE, 2)),
            new Recipe("树篱 ×3", "柔软的绿色装饰", Map.of(BlockType.WOOD, 1, BlockType.GRASS, 1), Map.of(BlockType.LEAVES, 3))
    );

    private RecipeBook() {
    }
}
