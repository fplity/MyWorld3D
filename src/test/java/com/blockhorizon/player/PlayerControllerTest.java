package com.blockhorizon.player;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.blockhorizon.world.*;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerControllerTest {
    @Test public void respawningClearsFlightAndVerticalSpeed() {
        PlayerController p = player(new PerspectiveCamera(), 4);
        p.setFlying(true);
        p.velocity().y = 10;
        p.spawn(new GridPos(0, 1, 0));
        assertFalse(p.flying());
        assertEquals(0f, p.velocity().y, 0f);
    }
    @BeforeClass public static void natives() { GdxNativesLoader.load(); }
    private VoxelWorld floor() {
        VoxelWorld world = new VoxelWorld("physics");
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) world.setGenerated(x, 0, z, BlockType.STONE);
        return world;
    }
    private PlayerController player(PerspectiveCamera camera, int height) {
        PlayerController player = new PlayerController(camera);
        player.spawn(new GridPos(0, height, 0));
        return player;
    }
    @Test public void fastFallCannotTunnelThroughOneBlockFloor() {
        PlayerController p = player(new PerspectiveCamera(), 12);
        VoxelWorld world = floor();
        p.velocity().y = -180;
        for (int i = 0; i < 5; i++) p.update(0.05f, world, new PlayerStats(), false, false);
        assertEquals(1f, p.position().y, 0.002f);
        assertTrue(p.onGround());
        assertFalse(p.collides(world, p.position().x, p.position().y, p.position().z));
        assertTrue(p.consumeLandingImpact() > 100);
        assertEquals(0f, p.consumeLandingImpact(), 0f);
    }
    @Test public void fastHorizontalMovementCannotSkipWall() {
        VoxelWorld world = floor();
        for (int y = 1; y <= 3; y++) world.setGenerated(1, y, 0, BlockType.STONE);
        PlayerController p = player(new PerspectiveCamera(), 1);
        p.velocity().x = 90;
        p.update(0.05f, world, new PlayerStats(), false, false);
        assertTrue(p.position().x <= 1f - PlayerController.RADIUS + 0.002f);
        assertEquals(0f, p.velocity().x, 0f);
    }
    @Test public void mouseRightTurnsCameraRight() {
        PerspectiveCamera camera = new PerspectiveCamera();
        PlayerController p = player(camera, 1);
        p.look(100, 0);
        p.update(0.01f, floor(), new PlayerStats(), false, false);
        assertTrue(camera.direction.x > 0);
    }
    @Test public void flightStopsVerticalDriftWhenControlsAreSuspended() {
        PlayerController p = player(new PerspectiveCamera(), 4);
        p.setFlying(true);
        p.velocity().y = 9;
        for (int i = 0; i < 10; i++) p.update(0.05f, floor(), new PlayerStats(), false, true);
        assertEquals(0f, p.velocity().y, 0f);
    }
    @Test public void enablingFlightClearsFallSpeed() {
        PlayerController p = player(new PerspectiveCamera(), 4);
        p.velocity().y = -40;
        p.setFlying(true);
        assertEquals(0f, p.velocity().y, 0f);
        assertFalse(p.onGround());
    }
}
