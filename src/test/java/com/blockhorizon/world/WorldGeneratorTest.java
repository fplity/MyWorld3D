package com.blockhorizon.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorldGeneratorTest {
    @Test public void outOfBoundsQueriesDoNotAliasPackedCoordinates() {
        VoxelWorld world = new VoxelWorld("bounds");
        world.setGenerated(0, 1, 0, BlockType.STONE);
        assertNull(world.get(1024, 1, 0));
        assertNull(world.get(0, 1, -1024));
        world.applyEdit(new GridPos(1024, 1, 0), "stone");
        assertTrue(world.edits().isEmpty());
    }
    @Test
    public void sameSeedProducesSameTerrainAndStructures() {
        VoxelWorld first = new WorldGenerator("固定种子-2048").generate();
        VoxelWorld second = new WorldGenerator("固定种子-2048").generate();
        assertEquals(first.blockCount(), second.blockCount());
        assertEquals(first.cabinCenter(), second.cabinCenter());
        assertEquals(first.shrineCenter(), second.shrineCenter());
        for (int x = -25; x <= 25; x += 5) {
            for (int z = -25; z <= 25; z += 5) {
                assertEquals(first.highestSolidY(x, z), second.highestSolidY(x, z));
                assertEquals(first.biomeAt(x, z), second.biomeAt(x, z));
            }
        }
    }

    @Test
    public void generatedWorldHasSafeSpawnAndDiscoverableLandmarks() {
        VoxelWorld world = new WorldGenerator("冒险世界-777").generate();
        GridPos spawn = world.findSpawn();
        assertTrue(world.insideHorizontalBounds(spawn.x(), spawn.z()));
        assertTrue(world.isSolid(spawn.x(), spawn.y() - 1, spawn.z()));
        assertNull(world.get(spawn));
        assertNull(world.get(spawn.add(0, 1, 0)));

        assertNotNull(world.cabinCenter());
        GridPos cabinChest = world.cabinCenter().add(2, 0, 2);
        assertEquals(BlockType.CHEST, world.get(cabinChest));
        assertNotNull(world.shrineCenter());
        assertEquals(BlockType.CRYSTAL, world.get(world.shrineCenter()));
    }

    @Test
    public void editsAreTrackedAndCanRepresentAir() {
        VoxelWorld world = new VoxelWorld("edits");
        world.setGenerated(1, 2, 3, BlockType.STONE);
        world.set(1, 2, 3, null);
        assertNull(world.get(1, 2, 3));
        assertEquals(VoxelWorld.AIR_MARKER, world.edits().get("1,2,3"));
        world.applyEdit(new GridPos(1, 2, 3), BlockType.BRICK.id());
        assertEquals(BlockType.BRICK, world.get(1, 2, 3));
    }
}
