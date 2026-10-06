package com.blockhorizon.player;

import com.blockhorizon.world.BlockType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class Inventory {
    public static final List<BlockType> HOTBAR = List.of(
            BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.WOOD,
            BlockType.PLANKS, BlockType.SAND, BlockType.LEAVES, BlockType.GLOWSTONE,
            BlockType.BRICK
    );

    private final EnumMap<BlockType, Integer> blocks = new EnumMap<>(BlockType.class);
    private int berries;
    private int selectedIndex;
    private boolean creative;

    public Inventory() {
        for (BlockType type : BlockType.values()) blocks.put(type, 0);
    }

    public void giveStarterKit() {
        add(BlockType.DIRT, 16);
        add(BlockType.GLOWSTONE, 1);
        berries = 3;
    }

    public int count(BlockType type) { return blocks.getOrDefault(type, 0); }

    public void add(BlockType type, int amount) {
        if (type == null || amount <= 0 || type == BlockType.BEDROCK || type == BlockType.WATER) return;
        blocks.merge(type, amount, Integer::sum);
    }

    public boolean remove(BlockType type, int amount) {
        if (creative) return true;
        int current = count(type);
        if (current < amount) return false;
        blocks.put(type, current - amount);
        return true;
    }

    public boolean canCraft(Recipe recipe) {
        if (creative) return true;
        return recipe.cost().entrySet().stream().allMatch(entry -> count(entry.getKey()) >= entry.getValue());
    }

    public boolean craft(Recipe recipe) {
        if (!canCraft(recipe)) return false;
        if (!creative) recipe.cost().forEach((type, count) -> remove(type, count));
        recipe.output().forEach(this::add);
        return true;
    }

    public BlockType selected() { return HOTBAR.get(selectedIndex); }
    public int selectedIndex() { return selectedIndex; }

    public void select(int index) {
        selectedIndex = Math.floorMod(index, HOTBAR.size());
    }

    public void scroll(int amount) {
        select(selectedIndex + amount);
    }

    public int berries() { return berries; }
    public void addBerries(int amount) { berries = Math.max(0, berries + amount); }

    public boolean eatBerry() {
        if (berries <= 0) return false;
        berries--;
        return true;
    }

    public boolean creative() { return creative; }
    public void setCreative(boolean creative) { this.creative = creative; }
    public void toggleCreative() { creative = !creative; }

    public Map<BlockType, Integer> snapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(blocks));
    }

    public void restore(Map<String, Integer> values, int berries, int selectedIndex, boolean creative) {
        blocks.replaceAll((key, value) -> 0);
        values.forEach((id, count) -> {
            try { blocks.put(BlockType.byId(id), Math.max(0, count)); }
            catch (IllegalArgumentException ignored) { }
        });
        this.berries = Math.max(0, berries);
        select(selectedIndex);
        this.creative = creative;
    }
}
