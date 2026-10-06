package com.blockhorizon.world;

public enum Biome {
    MEADOW("繁花草甸"),
    FOREST("青翠林地"),
    DESERT("琥珀沙海"),
    TUNDRA("寂静雪原"),
    HIGHLANDS("风语高地"),
    COAST("潮汐海岸");

    private final String displayName;

    Biome(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
