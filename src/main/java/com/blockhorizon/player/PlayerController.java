package com.blockhorizon.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.blockhorizon.GameConfig;
import com.blockhorizon.world.GridPos;
import com.blockhorizon.world.VoxelWorld;

public final class PlayerController {
    public static final float HEIGHT = 1.78f;
    public static final float EYE_HEIGHT = 1.61f;
    public static final float RADIUS = 0.31f;

    private final PerspectiveCamera camera;
    private final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();
    private final Vector3 wish = new Vector3();
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean flying;
    private boolean swimming;
    private boolean sprinting;
    private float bobTime;
    private float landingImpact;

    public PlayerController(PerspectiveCamera camera) {
        this.camera = camera;
    }

    public void spawn(GridPos spawn) {
        position.set(spawn.x() + 0.5f, spawn.y() + 0.05f, spawn.z() + 0.5f);
        velocity.setZero();
        flying = false;
        onGround = false;
        swimming = false;
        sprinting = false;
        landingImpact = 0;
        bobTime = 0;
        yaw = 0f;
        pitch = -5f;
        updateCamera(0f, false);
    }

    public void update(float delta, VoxelWorld world, PlayerStats stats, boolean acceptInput, boolean creative) {
        delta = Math.min(delta, 0.05f);
        wish.setZero();
        swimming = world.isLiquid(MathUtils.floor(position.x), MathUtils.floor(position.y + 0.7f), MathUtils.floor(position.z));
        boolean moving = false;
        if (acceptInput) {
            float forward = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1f : 0f) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1f : 0f);
            float strafe = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1f : 0f) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1f : 0f);
            moving = Math.abs(forward) + Math.abs(strafe) > 0.01f;
            float yawRad = yaw * MathUtils.degreesToRadians;
            wish.set(MathUtils.sin(yawRad) * forward + MathUtils.cos(yawRad) * strafe, 0,
                    -MathUtils.cos(yawRad) * forward + MathUtils.sin(yawRad) * strafe);
            if (wish.len2() > 1f) wish.nor();

            sprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) && forward > 0 && stats.canSprint();
            float speed = flying ? 9f : sprinting ? 7.6f : swimming ? 3.2f : 4.8f;
            wish.scl(speed);
            velocity.x = approach(velocity.x, wish.x, (onGround ? 30f : 12f) * delta);
            velocity.z = approach(velocity.z, wish.z, (onGround ? 30f : 12f) * delta);

            if (flying) {
                float vertical = (Gdx.input.isKeyPressed(Input.Keys.SPACE) ? 1f : 0f)
                        - (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ? 1f : 0f);
                velocity.y = approach(velocity.y, vertical * speed, 28f * delta);
            } else if ((swimming && Gdx.input.isKeyPressed(Input.Keys.SPACE))
                    || (onGround && Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
                velocity.y = swimming ? 4.2f : 7.2f;
                onGround = false;
            }
        } else {
            velocity.x = approach(velocity.x, 0, 25f * delta);
            velocity.z = approach(velocity.z, 0, 25f * delta);
            if (flying) velocity.y = approach(velocity.y, 0, 28f * delta);
            sprinting = false;
        }

        if (!flying) {
            velocity.y -= (swimming ? 5.5f : 20.5f) * delta;
            if (swimming) velocity.y = Math.max(velocity.y, -2.2f);
        }

        moveAxis(world, velocity.x * delta, 0, 0);
        moveAxis(world, 0, velocity.y * delta, 0);
        moveAxis(world, 0, 0, velocity.z * delta);
        position.x = MathUtils.clamp(position.x, -GameConfig.WORLD_HALF + RADIUS + 0.02f, GameConfig.WORLD_HALF - RADIUS - 0.02f);
        position.z = MathUtils.clamp(position.z, -GameConfig.WORLD_HALF + RADIUS + 0.02f, GameConfig.WORLD_HALF - RADIUS - 0.02f);
        position.y = Math.min(position.y, GameConfig.MAX_BUILD_HEIGHT + 80f);

        onGround = !flying && collides(world, position.x, position.y - 0.055f, position.z);
        if (onGround && velocity.y < 0) velocity.y = 0;
        if (position.y < -8f) landingImpact = 40f;
        if (moving && onGround) bobTime += delta * (sprinting ? 14f : 10f);
        updateCamera(delta, moving);
        stats.update(delta, sprinting, moving, creative);
    }

    private void moveAxis(VoxelWorld world, float dx, float dy, float dz) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) / 0.2f));
        for (int i = 0; i < steps; i++) {
            if (!moveStep(world, dx / steps, dy / steps, dz / steps)) break;
        }
    }

    private boolean moveStep(VoxelWorld world, float dx, float dy, float dz) {
        if (dx == 0 && dy == 0 && dz == 0) return true;
        float nx = position.x + dx;
        float ny = position.y + dy;
        float nz = position.z + dz;
        if (!flying && collides(world, nx, ny, nz)) {
            float low = 0f, high = 1f;
            for (int i = 0; i < 12; i++) {
                float mid = (low + high) * 0.5f;
                if (collides(world, position.x + dx * mid, position.y + dy * mid, position.z + dz * mid)) high = mid;
                else low = mid;
            }
            position.add(dx * low, dy * low, dz * low);
            if (dy < 0 && velocity.y < -9.5f) landingImpact = Math.max(landingImpact, -velocity.y);
            if (dx != 0) velocity.x = 0;
            if (dy != 0) velocity.y = 0;
            if (dz != 0) velocity.z = 0;
            return false;
        }
        position.set(nx, ny, nz);
        return true;
    }

    public boolean collides(VoxelWorld world, float px, float py, float pz) {
        int minX = MathUtils.floor(px - RADIUS);
        int maxX = MathUtils.floor(px + RADIUS - 0.001f);
        int minY = MathUtils.floor(py);
        int maxY = MathUtils.floor(py + HEIGHT - 0.001f);
        int minZ = MathUtils.floor(pz - RADIUS);
        int maxZ = MathUtils.floor(pz + RADIUS - 0.001f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.isSolid(x, y, z)) return true;
                }
            }
        }
        return false;
    }

    public boolean overlapsBlock(GridPos block) {
        return position.x + RADIUS > block.x() && position.x - RADIUS < block.x() + 1
                && position.y + HEIGHT > block.y() && position.y < block.y() + 1
                && position.z + RADIUS > block.z() && position.z - RADIUS < block.z() + 1;
    }

    public void look(float deltaX, float deltaY) {
        yaw = (yaw + deltaX * 0.13f) % 360f;
        pitch = MathUtils.clamp(pitch - deltaY * 0.13f, -89f, 89f);
    }

    private void updateCamera(float delta, boolean moving) {
        float bob = moving && onGround ? MathUtils.sin(bobTime) * 0.035f : 0f;
        camera.position.set(position.x, position.y + EYE_HEIGHT + bob, position.z);
        float yawRad = yaw * MathUtils.degreesToRadians;
        float pitchRad = pitch * MathUtils.degreesToRadians;
        camera.direction.set(
                MathUtils.sin(yawRad) * MathUtils.cos(pitchRad),
                MathUtils.sin(pitchRad),
                -MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)
        ).nor();
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private static float approach(float current, float target, float amount) {
        if (current < target) return Math.min(current + amount, target);
        return Math.max(current - amount, target);
    }

    public float consumeLandingImpact() {
        float value = landingImpact;
        landingImpact = 0f;
        return value;
    }

    public void toggleFlying() { setFlying(!flying); }
    public void setFlying(boolean flying) { this.flying = flying; velocity.y = 0; onGround = false; }
    public boolean flying() { return flying; }
    public boolean swimming() { return swimming; }
    public boolean sprinting() { return sprinting; }
    public boolean onGround() { return onGround; }
    public Vector3 position() { return position; }
    public Vector3 velocity() { return velocity; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    public void restore(Vector3 position, float yaw, float pitch, boolean flying) {
        this.position.set(position);
        this.yaw = yaw % 360f;
        this.pitch = MathUtils.clamp(pitch, -89f, 89f);
        this.flying = flying;
        velocity.setZero();
        updateCamera(0, false);
    }
}
