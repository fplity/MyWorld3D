package com.blockhorizon;

public final class GameConfig {
    public static final String TITLE = "方境 · BLOCKHORIZON";
    public static final int WINDOW_WIDTH = 1440;
    public static final int WINDOW_HEIGHT = 900;
    public static final int WORLD_SIZE = 64;
    public static final int WORLD_HALF = WORLD_SIZE / 2;
    public static final int MAX_BUILD_HEIGHT = 40;
    public static final int SEA_LEVEL = 6;
    public static final int CHUNK_SIZE = 8;
    public static final float DAY_LENGTH_SECONDS = 300f;
    public static final float REACH = 6.5f;

    private GameConfig() {
    }
}
