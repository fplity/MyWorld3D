package com.blockhorizon.world;

import com.badlogic.gdx.math.Vector3;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VoxelRaycasterTest {
    @Test public void positiveRayAtIntegerBoundaryHasCorrectDistance() {
        VoxelWorld world = new VoxelWorld("boundary");
        world.setGenerated(2, 1, 0, BlockType.STONE);
        Vector3 origin = new Vector3(0, 1.5f, 0.5f);
        assertNull(VoxelRaycaster.cast(world, origin, Vector3.X, 1.99f));
        VoxelRaycaster.Hit hit = VoxelRaycaster.cast(world, origin, Vector3.X, 2f);
        assertEquals(2f, hit.distance(), 0.0001f);
        assertEquals(new GridPos(-1, 0, 0), hit.normal());
    }
    @Test public void negativeIntegerBoundaryAndInsideBlockAreCorrect() {
        VoxelWorld world = new VoxelWorld("boundary");
        world.setGenerated(-1, 1, 0, BlockType.STONE);
        assertEquals(0f, VoxelRaycaster.cast(world, new Vector3(0, 1.5f, 0.5f), new Vector3(-1, 0, 0), 1).distance(), 0f);
        assertEquals(new GridPos(0, 0, 0), VoxelRaycaster.cast(world, new Vector3(-0.5f, 1.5f, 0.5f), Vector3.X, 1).normal());
    }
    @Test public void invalidRaysTerminateWithoutHits() {
        VoxelWorld world = new VoxelWorld("invalid");
        assertNull(VoxelRaycaster.cast(world, Vector3.Zero, Vector3.Zero, 8));
        assertNull(VoxelRaycaster.cast(world, new Vector3(Float.NaN, 0, 0), Vector3.X, 8));
        assertNull(VoxelRaycaster.cast(world, Vector3.Zero, Vector3.X, Float.POSITIVE_INFINITY));
        assertNull(VoxelRaycaster.cast(world, Vector3.Zero, Vector3.X, -1));
    }
    @Test
    public void returnsNearestBlockAndPlacementNormal() {
        VoxelWorld world = new VoxelWorld("ray");
        world.setGenerated(0, 1, -3, BlockType.STONE);
        world.setGenerated(0, 1, -5, BlockType.DIRT);
        VoxelRaycaster.Hit hit = VoxelRaycaster.cast(world, new Vector3(0.5f, 1.5f, 0.5f), new Vector3(0, 0, -1), 8f);
        assertEquals(new GridPos(0, 1, -3), hit.block());
        assertEquals(new GridPos(0, 0, 1), hit.normal());
        assertEquals(new GridPos(0, 1, -2), hit.placementPosition());
        assertEquals(BlockType.STONE, hit.type());
    }

    @Test
    public void ignoresWaterAndHonorsReach() {
        VoxelWorld world = new VoxelWorld("ray-water");
        world.setGenerated(0, 1, -1, BlockType.WATER);
        world.setGenerated(0, 1, -8, BlockType.STONE);
        assertNull(VoxelRaycaster.cast(world, new Vector3(0.5f, 1.5f, 0.5f), new Vector3(0, 0, -1), 5f));
    }
}
