package com.blockhorizon.render;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.blockhorizon.GameConfig;
import com.blockhorizon.world.VoxelWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class WorldRenderer implements Disposable {
    private final VoxelWorld world;
    private final BlockAtlas atlas = new BlockAtlas();
    private final ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder(atlas);
    private final Map<Long, Model> models = new HashMap<>();
    private final Map<Long, ModelInstance> instances = new HashMap<>();
    private final Map<Long, Vector3> centers = new HashMap<>();
    private final Vector3 halfExtents = new Vector3(GameConfig.CHUNK_SIZE / 2f,
            GameConfig.MAX_BUILD_HEIGHT / 2f, GameConfig.CHUNK_SIZE / 2f);
    private int visibleChunks;

    public WorldRenderer(VoxelWorld world) {
        this.world = world;
        rebuildAll();
    }

    public void rebuildAll() {
        models.values().forEach(Model::dispose);
        models.clear();
        instances.clear();
        centers.clear();
        int count = (int) Math.ceil(GameConfig.WORLD_SIZE / (float) GameConfig.CHUNK_SIZE);
        for (int x = 0; x < count; x++) {
            for (int z = 0; z < count; z++) rebuildChunk(x, z);
        }
        world.clearDirtyChunks();
    }

    public void rebuildDirty() {
        Set<Long> dirty = world.consumeDirtyChunks();
        int count = (int) Math.ceil(GameConfig.WORLD_SIZE / (float) GameConfig.CHUNK_SIZE);
        for (long key : dirty) {
            int x = VoxelWorld.chunkX(key);
            int z = VoxelWorld.chunkZ(key);
            if (x >= 0 && z >= 0 && x < count && z < count) rebuildChunk(x, z);
        }
    }

    private void rebuildChunk(int x, int z) {
        long key = VoxelWorld.packChunk(x, z);
        Model old = models.remove(key);
        if (old != null) old.dispose();
        Model model = meshBuilder.build(world, x, z);
        models.put(key, model);
        instances.put(key, new ModelInstance(model));
        centers.put(key, new Vector3(x * GameConfig.CHUNK_SIZE - GameConfig.WORLD_HALF + halfExtents.x,
                halfExtents.y, z * GameConfig.CHUNK_SIZE - GameConfig.WORLD_HALF + halfExtents.z));
    }

    public void render(ModelBatch batch, PerspectiveCamera camera, Environment environment) {
        visibleChunks = 0;
        for (Map.Entry<Long, ModelInstance> entry : instances.entrySet()) {
            if (!camera.frustum.boundsInFrustum(centers.get(entry.getKey()), halfExtents)) continue;
            batch.render(entry.getValue(), environment);
            visibleChunks++;
        }
    }

    public int visibleChunks() { return visibleChunks; }
    public int totalChunks() { return instances.size(); }

    @Override
    public void dispose() {
        models.values().forEach(Model::dispose);
        models.clear();
        instances.clear();
        centers.clear();
        atlas.close();
    }
}
