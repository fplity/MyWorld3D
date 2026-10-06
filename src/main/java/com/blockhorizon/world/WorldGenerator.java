package com.blockhorizon.world;

import com.blockhorizon.GameConfig;

import java.util.Random;

public final class WorldGenerator {
    private final String seedText;
    private final long seed;
    private final Random random;

    public WorldGenerator(String seedText) {
        this.seedText = seedText == null || seedText.isBlank() ? randomSeed() : seedText.trim();
        this.seed = Noise.seedHash(this.seedText);
        this.random = new Random(seed);
    }

    public VoxelWorld generate() {
        VoxelWorld world = new VoxelWorld(seedText);
        generateTerrain(world);
        growVegetation(world);
        buildCabin(world);
        buildEchoShrine(world);
        seedCaveCrystals(world);
        world.markAllDirty();
        return world;
    }

    private void generateTerrain(VoxelWorld world) {
        for (int x = -GameConfig.WORLD_HALF; x < GameConfig.WORLD_HALF; x++) {
            for (int z = -GameConfig.WORLD_HALF; z < GameConfig.WORLD_HALF; z++) {
                double continental = Noise.fbm2(x * 0.026, z * 0.026, seed, 5);
                double detail = Noise.fbm2(x * 0.075, z * 0.075, seed + 2048, 3);
                double ridges = 1.0 - Math.abs(Noise.fbm2(x * 0.018, z * 0.018, seed + 8192, 4));
                int height = clamp((int) Math.round(6 + continental * 5.4 + detail * 1.8 + Math.max(0, ridges - 0.68) * 15), 2, 20);
                Biome biome = selectBiome(x, z, height);
                world.setBiome(x, z, biome);

                for (int y = 0; y <= height; y++) {
                    BlockType block;
                    if (y == 0) block = BlockType.BEDROCK;
                    else if (y < height - 3) block = BlockType.STONE;
                    else if (y < height) block = biome == Biome.DESERT || biome == Biome.COAST ? BlockType.SAND : BlockType.DIRT;
                    else block = surfaceFor(biome, height);

                    boolean cave = y > 2 && y < height - 2
                            && Noise.value3(x * 0.105, y * 0.14, z * 0.105, seed + 7777) > 0.59
                            && Noise.value3(x * 0.045, y * 0.08, z * 0.045, seed + 3333) > -0.1;
                    if (!cave) world.setGenerated(x, y, z, block);
                }

                for (int y = height + 1; y <= GameConfig.SEA_LEVEL; y++) {
                    world.setGenerated(x, y, z, BlockType.WATER);
                }
            }
        }
    }

    private Biome selectBiome(int x, int z, int height) {
        double temperature = Noise.fbm2(x * 0.018, z * 0.018, seed + 100, 3) - z / 115.0;
        double moisture = Noise.fbm2(x * 0.022, z * 0.022, seed + 200, 3);
        if (height <= GameConfig.SEA_LEVEL + 1) return Biome.COAST;
        if (height >= 15) return temperature < 0.15 ? Biome.TUNDRA : Biome.HIGHLANDS;
        if (temperature < -0.38) return Biome.TUNDRA;
        if (temperature > 0.33 && moisture < 0.05) return Biome.DESERT;
        if (moisture > 0.18) return Biome.FOREST;
        return Biome.MEADOW;
    }

    private BlockType surfaceFor(Biome biome, int height) {
        return switch (biome) {
            case DESERT, COAST -> BlockType.SAND;
            case TUNDRA -> BlockType.SNOW;
            case HIGHLANDS -> height > 17 ? BlockType.SNOW : BlockType.STONE;
            default -> BlockType.GRASS;
        };
    }

    private void growVegetation(VoxelWorld world) {
        for (int x = -GameConfig.WORLD_HALF + 3; x < GameConfig.WORLD_HALF - 3; x++) {
            for (int z = -GameConfig.WORLD_HALF + 3; z < GameConfig.WORLD_HALF - 3; z++) {
                Biome biome = world.biomeAt(x, z);
                int y = world.highestSolidY(x, z);
                BlockType ground = world.get(x, y, z);
                double chance = biome == Biome.FOREST ? 0.078 : biome == Biome.MEADOW ? 0.017 : 0.0;
                if (ground == BlockType.GRASS && random.nextDouble() < chance && clearForTree(world, x, y + 1, z)) {
                    createTree(world, x, y + 1, z, 3 + random.nextInt(3));
                }
            }
        }
    }

    private boolean clearForTree(VoxelWorld world, int x, int y, int z) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (world.get(x + dx, y, z + dz) != null) return false;
            }
        }
        return true;
    }

    private void createTree(VoxelWorld world, int x, int y, int z, int trunkHeight) {
        for (int dy = 0; dy < trunkHeight; dy++) world.setGenerated(x, y + dy, z, BlockType.WOOD);
        int crownY = y + trunkHeight - 1;
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy == 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && random.nextBoolean()) continue;
                    if (dx == 0 && dz == 0 && dy <= 0) continue;
                    if (world.get(x + dx, crownY + dy, z + dz) == null) {
                        world.setGenerated(x + dx, crownY + dy, z + dz, BlockType.LEAVES);
                    }
                }
            }
        }
    }

    private void buildCabin(VoxelWorld world) {
        int x = -18 + random.nextInt(7);
        int z = 12 + random.nextInt(8);
        int y = world.highestSolidY(x, z) + 1;
        world.setCabinCenter(new GridPos(x, y, z));
        flatten(world, x, z, y - 1, 4);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.setGenerated(x + dx, y - 1, z + dz, BlockType.BRICK);
            }
        }
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                    boolean door = dz == -3 && dx == 0 && dy < 2;
                    boolean window = dy == 2 && ((Math.abs(dx) == 3 && dz == 0) || (Math.abs(dz) == 3 && dx == 0));
                    if (wall && !door && !window) world.setGenerated(x + dx, y + dy, z + dz, BlockType.PLANKS);
                }
            }
        }
        for (int layer = 0; layer < 3; layer++) {
            for (int dx = -4 + layer; dx <= 4 - layer; dx++) {
                for (int dz = -4 + layer; dz <= 4 - layer; dz++) {
                    if (Math.abs(dx) == 4 - layer || Math.abs(dz) == 4 - layer) {
                        world.setGenerated(x + dx, y + 4 + layer, z + dz, BlockType.WOOD);
                    }
                }
            }
        }
        world.setGenerated(x + 2, y, z + 2, BlockType.CHEST);
        world.setGenerated(x - 2, y + 1, z + 2, BlockType.GLOWSTONE);
    }

    private void buildEchoShrine(VoxelWorld world) {
        int x = 15 + random.nextInt(9);
        int z = -22 + random.nextInt(8);
        int y = world.highestSolidY(x, z) + 1;
        world.setShrineCenter(new GridPos(x, y, z));
        flatten(world, x, z, y - 1, 5);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 19) world.setGenerated(x + dx, y - 1, z + dz, BlockType.BRICK);
            }
        }
        int[][] pillars = {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}};
        for (int[] p : pillars) {
            for (int dy = 0; dy < 5; dy++) world.setGenerated(x + p[0], y + dy, z + p[1], BlockType.BRICK);
            world.setGenerated(x + p[0], y + 5, z + p[1], BlockType.GLOWSTONE);
        }
        world.setGenerated(x, y, z, BlockType.CRYSTAL);
        world.setGenerated(x, y - 1, z, BlockType.GLOWSTONE);
    }

    private void seedCaveCrystals(VoxelWorld world) {
        for (int i = 0; i < 70; i++) {
            int x = -GameConfig.WORLD_HALF + 2 + random.nextInt(GameConfig.WORLD_SIZE - 4);
            int z = -GameConfig.WORLD_HALF + 2 + random.nextInt(GameConfig.WORLD_SIZE - 4);
            int top = world.highestSolidY(x, z);
            if (top <= 5) continue;
            int y = 2 + random.nextInt(Math.max(1, top - 3));
            if (world.get(x, y, z) == null && world.get(x, y - 1, z) == BlockType.STONE) {
                world.setGenerated(x, y, z, random.nextFloat() < 0.25f ? BlockType.GLOWSTONE : BlockType.CRYSTAL);
            }
        }
    }

    private void flatten(VoxelWorld world, int cx, int cz, int targetY, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                for (int y = targetY + 1; y < GameConfig.MAX_BUILD_HEIGHT; y++) {
                    BlockType existing = world.get(x, y, z);
                    if (existing != null && existing != BlockType.BEDROCK) world.setGenerated(x, y, z, null);
                }
                for (int y = world.highestSolidY(x, z) + 1; y <= targetY; y++) world.setGenerated(x, y, z, BlockType.DIRT);
                if (world.get(x, targetY, z) == null || world.get(x, targetY, z) == BlockType.WATER) {
                    world.setGenerated(x, targetY, z, BlockType.GRASS);
                }
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String randomSeed() {
        String[] first = {"星落", "青苔", "远风", "琥珀", "月潮", "云鲸", "萤火", "霜原"};
        String[] second = {"山谷", "群岛", "密林", "原野", "秘境", "长夜", "晨曦", "回响"};
        Random r = new Random();
        return first[r.nextInt(first.length)] + second[r.nextInt(second.length)] + "-" + (100 + r.nextInt(900));
    }
}
