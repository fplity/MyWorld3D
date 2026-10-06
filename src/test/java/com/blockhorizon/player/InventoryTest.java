package com.blockhorizon.player;

import com.blockhorizon.world.BlockType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InventoryTest {
    @Test
    public void craftingConsumesInputsAndCreatesOutput() {
        Inventory inventory = new Inventory();
        inventory.add(BlockType.WOOD, 2);
        Recipe planks = RecipeBook.RECIPES.get(0);
        assertTrue(inventory.craft(planks));
        assertEquals(1, inventory.count(BlockType.WOOD));
        assertEquals(4, inventory.count(BlockType.PLANKS));
    }

    @Test
    public void craftingFailsWithoutMaterialsAndCreativeHasInfinitePlacement() {
        Inventory inventory = new Inventory();
        assertFalse(inventory.craft(RecipeBook.RECIPES.get(1)));
        inventory.setCreative(true);
        assertTrue(inventory.remove(BlockType.GLOWSTONE, 999));
        assertTrue(inventory.craft(RecipeBook.RECIPES.get(2)));
        assertEquals(2, inventory.count(BlockType.GLOWSTONE));
    }

    @Test
    public void hotbarWrapsInBothDirections() {
        Inventory inventory = new Inventory();
        inventory.select(-1);
        assertEquals(Inventory.HOTBAR.size() - 1, inventory.selectedIndex());
        inventory.scroll(1);
        assertEquals(0, inventory.selectedIndex());
    }
}
