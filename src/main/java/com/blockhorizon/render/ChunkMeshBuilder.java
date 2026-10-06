package com.blockhorizon.render;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.blockhorizon.GameConfig;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.VoxelWorld;

import java.util.function.Predicate;

public final class ChunkMeshBuilder {
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates;
    private final BlockAtlas atlas;
    private final Material opaqueMaterial;
    private final Material leavesMaterial;
    private final Material waterMaterial;
    private final Material emissiveMaterial;

    public ChunkMeshBuilder(BlockAtlas atlas) {
        this.atlas = atlas;
        opaqueMaterial = new Material(TextureAttribute.createDiffuse(atlas.texture()));
        leavesMaterial = new Material(TextureAttribute.createDiffuse(atlas.texture()), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.92f));
        waterMaterial = new Material(TextureAttribute.createDiffuse(atlas.texture()), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.67f));
        emissiveMaterial = new Material(TextureAttribute.createDiffuse(atlas.texture()), ColorAttribute.createEmissive(0.48f, 0.42f, 0.28f, 1f));
    }

    public Model build(VoxelWorld world, int chunkX, int chunkZ) {
        ModelBuilder models = new ModelBuilder();
        models.begin();
        addPart(models, world, chunkX, chunkZ, "opaque", opaqueMaterial,
                type -> type != BlockType.WATER && type != BlockType.LEAVES && !type.emissive());
        addPart(models, world, chunkX, chunkZ, "leaves", leavesMaterial, type -> type == BlockType.LEAVES);
        addPart(models, world, chunkX, chunkZ, "emissive", emissiveMaterial, BlockType::emissive);
        addPart(models, world, chunkX, chunkZ, "water", waterMaterial, type -> type == BlockType.WATER);
        return models.end();
    }

    private void addPart(ModelBuilder models, VoxelWorld world, int chunkX, int chunkZ,
                         String id, Material material, Predicate<BlockType> accepts) {
        MeshPartBuilder builder = models.part(id, GL20.GL_TRIANGLES, ATTRIBUTES, material);
        int minX = -GameConfig.WORLD_HALF + chunkX * GameConfig.CHUNK_SIZE;
        int minZ = -GameConfig.WORLD_HALF + chunkZ * GameConfig.CHUNK_SIZE;
        int maxX = Math.min(minX + GameConfig.CHUNK_SIZE, GameConfig.WORLD_HALF);
        int maxZ = Math.min(minZ + GameConfig.CHUNK_SIZE, GameConfig.WORLD_HALF);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                for (int y = 0; y < GameConfig.MAX_BUILD_HEIGHT; y++) {
                    BlockType type = world.get(x, y, z);
                    if (type == null || !accepts.test(type)) continue;
                    if (world.isFaceVisible(x, y, z, x + 1, y, z)) face(builder, x, y, z, Face.POS_X, type.sideTile());
                    if (world.isFaceVisible(x, y, z, x - 1, y, z)) face(builder, x, y, z, Face.NEG_X, type.sideTile());
                    if (world.isFaceVisible(x, y, z, x, y + 1, z)) face(builder, x, y, z, Face.POS_Y, type.topTile());
                    if (world.isFaceVisible(x, y, z, x, y - 1, z)) face(builder, x, y, z, Face.NEG_Y, type.bottomTile());
                    if (world.isFaceVisible(x, y, z, x, y, z + 1)) face(builder, x, y, z, Face.POS_Z, type.sideTile());
                    if (world.isFaceVisible(x, y, z, x, y, z - 1)) face(builder, x, y, z, Face.NEG_Z, type.sideTile());
                }
            }
        }
    }

    private enum Face { POS_X, NEG_X, POS_Y, NEG_Y, POS_Z, NEG_Z }

    private void face(MeshPartBuilder builder, float x, float y, float z, Face face, int tile) {
        float waterDrop = tile == BlockType.WATER.topTile() && face == Face.POS_Y ? 0.12f : 0f;
        float top = y + 1f - waterDrop;
        Vector3 normal;
        Vector3 a;
        Vector3 b;
        Vector3 c;
        Vector3 d;
        switch (face) {
            case POS_X -> { normal = Vector3.X; a = new Vector3(x + 1, y, z); b = new Vector3(x + 1, top, z); c = new Vector3(x + 1, top, z + 1); d = new Vector3(x + 1, y, z + 1); }
            case NEG_X -> { normal = new Vector3(-1, 0, 0); a = new Vector3(x, y, z + 1); b = new Vector3(x, top, z + 1); c = new Vector3(x, top, z); d = new Vector3(x, y, z); }
            case POS_Y -> { normal = Vector3.Y; a = new Vector3(x, top, z + 1); b = new Vector3(x + 1, top, z + 1); c = new Vector3(x + 1, top, z); d = new Vector3(x, top, z); }
            case NEG_Y -> { normal = new Vector3(0, -1, 0); a = new Vector3(x, y, z); b = new Vector3(x + 1, y, z); c = new Vector3(x + 1, y, z + 1); d = new Vector3(x, y, z + 1); }
            case POS_Z -> { normal = Vector3.Z; a = new Vector3(x + 1, y, z + 1); b = new Vector3(x + 1, top, z + 1); c = new Vector3(x, top, z + 1); d = new Vector3(x, y, z + 1); }
            case NEG_Z -> { normal = new Vector3(0, 0, -1); a = new Vector3(x, y, z); b = new Vector3(x, top, z); c = new Vector3(x + 1, top, z); d = new Vector3(x + 1, y, z); }
            default -> throw new IllegalStateException();
        }
        float u0 = atlas.u0(tile), u1 = atlas.u1(tile), v0 = atlas.v0(tile), v1 = atlas.v1(tile);
        MeshPartBuilder.VertexInfo va = vertex(a, normal, u0, v1);
        MeshPartBuilder.VertexInfo vb = vertex(b, normal, u0, v0);
        MeshPartBuilder.VertexInfo vc = vertex(c, normal, u1, v0);
        MeshPartBuilder.VertexInfo vd = vertex(d, normal, u1, v1);
        builder.rect(va, vb, vc, vd);
    }

    private MeshPartBuilder.VertexInfo vertex(Vector3 pos, Vector3 normal, float u, float v) {
        return new MeshPartBuilder.VertexInfo().setPos(pos).setNor(normal).setUV(u, v);
    }
}
