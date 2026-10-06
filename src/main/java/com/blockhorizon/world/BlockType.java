package com.blockhorizon.world;

import com.badlogic.gdx.graphics.Color;

public enum BlockType {
    GRASS("grass", "草方块", "▰", true, false, false, false, 0.34f, 1, 0, 2, new Color(0x66a64fff)),
    DIRT("dirt", "泥土", "■", true, false, false, false, 0.28f, 2, 2, 2, new Color(0x81583cff)),
    STONE("stone", "石头", "◆", true, false, false, false, 0.82f, 3, 3, 3, new Color(0x858b92ff)),
    SAND("sand", "沙子", "▦", true, false, false, false, 0.24f, 4, 4, 4, new Color(0xd8c27aff)),
    WOOD("wood", "原木", "▥", true, false, false, false, 0.58f, 5, 6, 6, new Color(0x8d6038ff)),
    LEAVES("leaves", "树叶", "♣", true, false, true, false, 0.17f, 7, 7, 7, new Color(0x3f8751ff)),
    SNOW("snow", "雪块", "❄", true, false, false, false, 0.26f, 8, 8, 8, new Color(0xeaf6f6ff)),
    WATER("water", "水", "≈", false, true, true, false, 0f, 9, 9, 9, new Color(0x348fd0b0)),
    PLANKS("planks", "木板", "▤", true, false, false, false, 0.48f, 10, 10, 10, new Color(0xad7848ff)),
    GLOWSTONE("glowstone", "星辉石", "✦", true, false, false, true, 0.5f, 11, 11, 11, new Color(0xf3c95aff)),
    CRYSTAL("crystal", "回响晶体", "♦", true, false, true, true, 0.68f, 12, 12, 12, new Color(0x8f78efff)),
    BRICK("brick", "石砖", "▩", true, false, false, false, 0.9f, 13, 13, 13, new Color(0x747b83ff)),
    BEDROCK("bedrock", "基岩", "▧", true, false, false, false, Float.POSITIVE_INFINITY, 14, 14, 14, new Color(0x292d33ff)),
    CHEST("chest", "古旧宝箱", "▣", true, false, false, false, 0.55f, 15, 15, 15, new Color(0xb77c32ff));

    private final String id;
    private final String displayName;
    private final String icon;
    private final boolean solid;
    private final boolean liquid;
    private final boolean transparent;
    private final boolean emissive;
    private final float hardness;
    private final int sideTile;
    private final int topTile;
    private final int bottomTile;
    private final Color color;

    BlockType(String id, String displayName, String icon, boolean solid, boolean liquid,
              boolean transparent, boolean emissive, float hardness,
              int sideTile, int topTile, int bottomTile, Color color) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.solid = solid;
        this.liquid = liquid;
        this.transparent = transparent;
        this.emissive = emissive;
        this.hardness = hardness;
        this.sideTile = sideTile;
        this.topTile = topTile;
        this.bottomTile = bottomTile;
        this.color = color;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public String icon() { return icon; }
    public boolean solid() { return solid; }
    public boolean liquid() { return liquid; }
    public boolean transparent() { return transparent; }
    public boolean emissive() { return emissive; }
    public float hardness() { return hardness; }
    public int sideTile() { return sideTile; }
    public int topTile() { return topTile; }
    public int bottomTile() { return bottomTile; }
    public Color color() { return color; }
    public boolean breakable() { return !Float.isInfinite(hardness); }

    public static BlockType byId(String id) {
        for (BlockType value : values()) {
            if (value.id.equals(id)) return value;
        }
        throw new IllegalArgumentException("Unknown block id: " + id);
    }
}
