package com.blockhorizon.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.blockhorizon.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VoxelWorld {
    public static final String AIR_MARKER = "__air__";

    private final Map<Long, BlockType> blocks = new HashMap<>();
    private final Map<String, String> edits = new HashMap<>();
    private final Set<String> openedChests = new HashSet<>();
    private final Map<Long, Biome> biomes = new HashMap<>();
    private final Set<Long> dirtyChunks = new HashSet<>();
    private String seed;
    private GridPos shrineCenter;
    private GridPos cabinCenter;

    public VoxelWorld(String seed) {
        this.seed = seed;
    }

    public String seed() { return seed; }
    public GridPos shrineCenter() { return shrineCenter; }
    public GridPos cabinCenter() { return cabinCenter; }
    public void setShrineCenter(GridPos position) { shrineCenter = position; }
    public void setCabinCenter(GridPos position) { cabinCenter = position; }

    public BlockType get(int x, int y, int z) {
        if (!insideHorizontalBounds(x, z) || y < 0 || y >= GameConfig.MAX_BUILD_HEIGHT) return null;
        return blocks.get(pack(x, y, z));
    }

    public BlockType get(GridPos position) {
        return get(position.x(), position.y(), position.z());
    }

    public boolean isSolid(int x, int y, int z) {
        BlockType block = get(x, y, z);
        return block != null && block.solid();
    }

    public boolean isLiquid(int x, int y, int z) {
        BlockType block = get(x, y, z);
        return block != null && block.liquid();
    }

    public void setGenerated(int x, int y, int z, BlockType block) {
        setInternal(x, y, z, block, false);
    }

    public void set(int x, int y, int z, BlockType block) {
        setInternal(x, y, z, block, true);
    }

    public void set(GridPos position, BlockType block) {
        set(position.x(), position.y(), position.z(), block);
    }

    private void setInternal(int x, int y, int z, BlockType block, boolean trackEdit) {
        if (!insideHorizontalBounds(x, z) || y < 0 || y >= GameConfig.MAX_BUILD_HEIGHT) return;
        long key = pack(x, y, z);
        if (block == null) blocks.remove(key);
        else blocks.put(key, block);
        if (trackEdit) edits.put(new GridPos(x, y, z).serialized(), block == null ? AIR_MARKER : block.id());
        markDirtyAround(x, z);
    }

    public void applyEdit(GridPos position, String blockId) {
        if (!insideHorizontalBounds(position.x(), position.z()) || position.y() < 0
                || position.y() >= GameConfig.MAX_BUILD_HEIGHT) return;
        BlockType block = AIR_MARKER.equals(blockId) ? null : BlockType.byId(blockId);
        setInternal(position.x(), position.y(), position.z(), block, false);
        edits.put(position.serialized(), blockId);
    }

    public Map<String, String> edits() {
        return Collections.unmodifiableMap(edits);
    }

    public void clearDirtyChunks() {
        dirtyChunks.clear();
    }

    public Set<Long> consumeDirtyChunks() {
        Set<Long> result = new HashSet<>(dirtyChunks);
        dirtyChunks.clear();
        return result;
    }

    public void markAllDirty() {
        int chunks = GameConfig.WORLD_SIZE / GameConfig.CHUNK_SIZE;
        for (int cx = 0; cx < chunks; cx++) {
            for (int cz = 0; cz < chunks; cz++) dirtyChunks.add(packChunk(cx, cz));
        }
    }

    public int highestSolidY(int x, int z) {
        for (int y = GameConfig.MAX_BUILD_HEIGHT - 1; y >= 0; y--) {
            BlockType type = get(x, y, z);
            if (type != null && type.solid() && type != BlockType.LEAVES) return y;
        }
        return 0;
    }

    public GridPos findSpawn() {
        for (int radius = 0; radius < GameConfig.WORLD_HALF - 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    int y = highestSolidY(x, z);
                    BlockType ground = get(x, y, z);
                    if (ground != null && ground != BlockType.SAND && !isLiquid(x, y + 1, z)
                            && get(x, y + 1, z) == null && get(x, y + 2, z) == null) {
                        return new GridPos(x, y + 1, z);
                    }
                }
            }
        }
        return new GridPos(0, highestSolidY(0, 0) + 1, 0);
    }

    public boolean insideHorizontalBounds(int x, int z) {
        return x >= -GameConfig.WORLD_HALF && x < GameConfig.WORLD_HALF
                && z >= -GameConfig.WORLD_HALF && z < GameConfig.WORLD_HALF;
    }

    public boolean isFaceVisible(int x, int y, int z, int nx, int ny, int nz) {
        BlockType self = get(x, y, z);
        BlockType neighbor = get(nx, ny, nz);
        if (neighbor == null) return true;
        if (self == BlockType.WATER && neighbor == BlockType.WATER) return false;
        if (self != null && self.transparent() && neighbor == self) return false;
        return neighbor.transparent() || neighbor.liquid();
    }

    public Iterable<Map.Entry<Long, BlockType>> allBlocks() {
        return blocks.entrySet();
    }

    public int blockCount() { return blocks.size(); }

    public void setBiome(int x, int z, Biome biome) {
        biomes.put(packColumn(x, z), biome);
    }

    public Biome biomeAt(int x, int z) {
        return biomes.getOrDefault(packColumn(x, z), Biome.MEADOW);
    }

    public boolean openChest(GridPos position) {
        return openedChests.add(position.serialized());
    }

    public boolean isChestOpened(GridPos position) {
        return openedChests.contains(position.serialized());
    }

    public Set<String> openedChests() {
        return Collections.unmodifiableSet(openedChests);
    }

    public void restoreOpenedChests(Iterable<String> positions) {
        for (String position : positions) openedChests.add(position);
    }

    public List<GridPos> exposedEmissiveBlocks() {
        List<GridPos> result = new ArrayList<>();
        for (Map.Entry<Long, BlockType> entry : blocks.entrySet()) {
            if (entry.getValue().emissive()) result.add(unpack(entry.getKey()));
        }
        return result;
    }

    public static long pack(int x, int y, int z) {
        long px = (x + 512L) & 0x3ffL;
        long py = y & 0xffL;
        long pz = (z + 512L) & 0x3ffL;
        return px | (py << 10) | (pz << 18);
    }

    public static GridPos unpack(long packed) {
        int x = (int) (packed & 0x3ffL) - 512;
        int y = (int) ((packed >>> 10) & 0xffL);
        int z = (int) ((packed >>> 18) & 0x3ffL) - 512;
        return new GridPos(x, y, z);
    }

    public static long packChunk(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    public static int chunkX(long packed) { return (int) (packed >> 32); }
    public static int chunkZ(long packed) { return (int) packed; }

    public static int worldToChunk(int coordinate) {
        return MathUtils.floor((coordinate + GameConfig.WORLD_HALF) / (float) GameConfig.CHUNK_SIZE);
    }

    private void markDirtyAround(int x, int z) {
        int cx = worldToChunk(x);
        int cz = worldToChunk(z);
        dirtyChunks.add(packChunk(cx, cz));
        int localX = Math.floorMod(x + GameConfig.WORLD_HALF, GameConfig.CHUNK_SIZE);
        int localZ = Math.floorMod(z + GameConfig.WORLD_HALF, GameConfig.CHUNK_SIZE);
        if (localX == 0) dirtyChunks.add(packChunk(cx - 1, cz));
        if (localX == GameConfig.CHUNK_SIZE - 1) dirtyChunks.add(packChunk(cx + 1, cz));
        if (localZ == 0) dirtyChunks.add(packChunk(cx, cz - 1));
        if (localZ == GameConfig.CHUNK_SIZE - 1) dirtyChunks.add(packChunk(cx, cz + 1));
    }

    private static long packColumn(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
