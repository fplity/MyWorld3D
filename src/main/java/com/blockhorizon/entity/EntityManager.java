package com.blockhorizon.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.blockhorizon.GameConfig;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.VoxelWorld;

import java.util.Random;

public final class EntityManager implements Disposable {
    public enum MobType { MOSS_MOP, SHADE }

    public record UpdateResult(float damage, String message) {
        public static final UpdateResult NONE = new UpdateResult(0, null);
    }

    public record AttackResult(boolean hit, boolean killed, MobType type, Vector3 position) {
        public static final AttackResult MISS = new AttackResult(false, false, null, null);
    }

    private static final class Mob {
        final MobType type;
        final Vector3 position = new Vector3();
        final Vector3 direction = new Vector3();
        final ModelInstance instance;
        float health;
        float phase;
        float wanderTimer;
        float hitCooldown;

        Mob(MobType type, Model model) {
            this.type = type;
            this.instance = new ModelInstance(model);
            this.health = type == MobType.SHADE ? 42f : 28f;
        }
    }

    private static final long ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private final VoxelWorld world;
    private final Random random;
    private final Model mossModel;
    private final Model shadeModel;
    private final Model companionModel;
    private final Array<Mob> mobs = new Array<>();
    private final ModelInstance companion;
    private final PointLight companionLight = new PointLight();
    private final Vector3 companionPosition = new Vector3();
    private boolean companionUnlocked;
    private float hostileSpawnTimer = 3f;
    private float companionPulse;

    public EntityManager(VoxelWorld world, Environment environment, long seed) {
        this.world = world;
        this.random = new Random(seed ^ 0x5130F3L);
        mossModel = createMossModel();
        shadeModel = createShadeModel();
        companionModel = createCompanionModel();
        companion = new ModelInstance(companionModel);
        companionLight.set(0.5f, 0.36f, 1f, 0, 0, 0, 0f);
        environment.add(companionLight);
        spawnInitialMobs();
    }

    private Model createMossModel() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder body = builder.part("body", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0x72af58ff))));
        body.box(0, 0.4f, 0, 0.82f, 0.75f, 0.82f);
        MeshPartBuilder cap = builder.part("moss", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0x3f7e47ff))));
        cap.box(0, 0.8f, 0, 0.88f, 0.18f, 0.88f);
        MeshPartBuilder eye = builder.part("eyes", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0x142022ff))));
        eye.box(-0.2f, 0.52f, -0.425f, 0.1f, 0.14f, 0.04f);
        eye.box(0.2f, 0.52f, -0.425f, 0.1f, 0.14f, 0.04f);
        return builder.end();
    }

    private Model createShadeModel() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder body = builder.part("shade", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0x271d3ad9)), new BlendingAttribute(0.84f)));
        body.box(0, 0.75f, 0, 0.72f, 1.4f, 0.72f);
        MeshPartBuilder eyes = builder.part("eyes", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0xc56cffff)), ColorAttribute.createEmissive(0.52f, 0.16f, 0.8f, 1f)));
        eyes.box(-0.16f, 0.92f, -0.37f, 0.1f, 0.09f, 0.035f);
        eyes.box(0.16f, 0.92f, -0.37f, 0.1f, 0.09f, 0.035f);
        return builder.end();
    }

    private Model createCompanionModel() {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        Material coreMaterial = new Material(ColorAttribute.createDiffuse(new Color(0xc8bcffff)), ColorAttribute.createEmissive(0.62f, 0.45f, 1f, 1f));
        MeshPartBuilder core = builder.part("core", GL20.GL_TRIANGLES, ATTRS, coreMaterial);
        core.sphere(0.42f, 0.42f, 0.42f, 10, 7);
        MeshPartBuilder wings = builder.part("wings", GL20.GL_TRIANGLES, ATTRS,
                new Material(ColorAttribute.createDiffuse(new Color(0x8cdfffc0)), new BlendingAttribute(0.74f)));
        wings.box(-0.39f, 0, 0, 0.36f, 0.08f, 0.5f);
        wings.box(0.39f, 0, 0, 0.36f, 0.08f, 0.5f);
        return builder.end();
    }

    private void spawnInitialMobs() {
        for (int i = 0; i < 9; i++) spawn(MobType.MOSS_MOP, randomSurfacePosition(7f));
    }

    private Vector3 randomSurfacePosition(float minRadius) {
        for (int attempt = 0; attempt < 80; attempt++) {
            int x = -GameConfig.WORLD_HALF + 3 + random.nextInt(GameConfig.WORLD_SIZE - 6);
            int z = -GameConfig.WORLD_HALF + 3 + random.nextInt(GameConfig.WORLD_SIZE - 6);
            if (x * x + z * z < minRadius * minRadius) continue;
            int y = world.highestSolidY(x, z);
            BlockType ground = world.get(x, y, z);
            if (ground != null && ground != BlockType.WATER && ground != BlockType.LEAVES) return new Vector3(x + 0.5f, y + 1f, z + 0.5f);
        }
        return new Vector3(8, world.highestSolidY(8, 8) + 1f, 8);
    }

    private void spawn(MobType type, Vector3 position) {
        Mob mob = new Mob(type, type == MobType.SHADE ? shadeModel : mossModel);
        mob.position.set(position);
        mob.direction.setToRandomDirection().y = 0;
        mob.direction.nor();
        mob.wanderTimer = 1f + random.nextFloat() * 4f;
        mob.phase = random.nextFloat() * MathUtils.PI2;
        mobs.add(mob);
    }

    public UpdateResult update(float delta, Vector3 player, boolean night, boolean paused) {
        if (paused) return UpdateResult.NONE;
        float damage = 0;
        String message = null;
        int shades = 0;
        for (Mob mob : mobs) if (mob.type == MobType.SHADE) shades++;
        hostileSpawnTimer -= delta;
        if (night && shades < 5 && hostileSpawnTimer <= 0f) {
            Vector3 pos = randomSurfacePosition(13f);
            if (pos.dst2(player) > 100f) spawn(MobType.SHADE, pos);
            hostileSpawnTimer = 8f + random.nextFloat() * 8f;
        }

        for (int i = mobs.size - 1; i >= 0; i--) {
            Mob mob = mobs.get(i);
            if (mob.type == MobType.SHADE && !night) {
                mob.health -= delta * 5f;
                if (mob.health <= 0) { mobs.removeIndex(i); continue; }
            }
            mob.phase += delta * (mob.type == MobType.SHADE ? 5f : 3.2f);
            mob.wanderTimer -= delta;
            mob.hitCooldown -= delta;
            float distance = mob.position.dst(player);
            float speed;
            if (mob.type == MobType.SHADE && distance < 18f) {
                mob.direction.set(player).sub(mob.position).set(mob.direction.x, 0, mob.direction.z).nor();
                speed = 2.15f;
                if (distance < 1.25f && mob.hitCooldown <= 0f) {
                    damage += 7f;
                    mob.hitCooldown = 1.45f;
                    message = "暗影生物击中了你";
                }
            } else {
                speed = mob.type == MobType.SHADE ? 1.2f : 0.68f;
                if (mob.wanderTimer <= 0f) {
                    float angle = random.nextFloat() * MathUtils.PI2;
                    mob.direction.set(MathUtils.cos(angle), 0, MathUtils.sin(angle));
                    mob.wanderTimer = 2f + random.nextFloat() * 5f;
                }
            }
            float nx = mob.position.x + mob.direction.x * speed * delta;
            float nz = mob.position.z + mob.direction.z * speed * delta;
            if (world.insideHorizontalBounds(MathUtils.floor(nx), MathUtils.floor(nz))) {
                int ground = world.highestSolidY(MathUtils.floor(nx), MathUtils.floor(nz));
                if (Math.abs((ground + 1f) - mob.position.y) < 2.2f) {
                    mob.position.x = nx;
                    mob.position.z = nz;
                    mob.position.y = MathUtils.lerp(mob.position.y, ground + 1f, Math.min(1f, delta * 8f));
                } else mob.direction.scl(-1);
            } else mob.direction.scl(-1);
            float bounce = Math.abs(MathUtils.sin(mob.phase)) * (mob.type == MobType.SHADE ? 0.16f : 0.25f);
            float facing = MathUtils.atan2(mob.direction.x, -mob.direction.z) * MathUtils.radiansToDegrees;
            mob.instance.transform.setToTranslation(mob.position.x, mob.position.y + bounce, mob.position.z).rotate(Vector3.Y, facing);
        }

        if (companionUnlocked) {
            companionPulse += delta;
            float angle = companionPulse * 1.15f;
            companionPosition.set(player).add(MathUtils.cos(angle) * 1.8f, 1.75f + MathUtils.sin(angle * 1.8f) * 0.22f, MathUtils.sin(angle) * 1.8f);
            companion.transform.setToTranslation(companionPosition).rotate(Vector3.Y, companionPulse * 90f);
            companionLight.setPosition(companionPosition);
            companionLight.intensity = 6.5f;
        } else companionLight.intensity = 0f;
        return damage > 0 ? new UpdateResult(damage, message) : UpdateResult.NONE;
    }

    public AttackResult attack(Vector3 origin, Vector3 direction, float reach, float damage) {
        Mob nearest = null;
        float nearestT = reach;
        Vector3 ray = new Vector3(direction).nor();
        for (Mob mob : mobs) {
            Vector3 center = new Vector3(mob.position).add(0, 0.6f, 0);
            Vector3 to = center.sub(origin);
            float t = to.dot(ray);
            if (t < 0 || t > nearestT) continue;
            float radius = mob.type == MobType.SHADE ? 0.72f : 0.64f;
            if (to.len2() - t * t <= radius * radius) {
                nearest = mob;
                nearestT = t;
            }
        }
        if (nearest == null) return AttackResult.MISS;
        nearest.health -= damage;
        boolean killed = nearest.health <= 0;
        Vector3 position = new Vector3(nearest.position).add(0, 0.6f, 0);
        MobType type = nearest.type;
        nearest.direction.set(ray).scl(-1);
        if (killed) mobs.removeValue(nearest, true);
        return new AttackResult(true, killed, type, position);
    }

    public void render(ModelBatch batch, Environment environment) {
        for (Mob mob : mobs) batch.render(mob.instance, environment);
        if (companionUnlocked) batch.render(companion, environment);
    }

    public void unlockCompanion() { companionUnlocked = true; }
    public boolean companionUnlocked() { return companionUnlocked; }
    public Vector3 companionPosition() { return companionPosition; }
    public int mobCount() { return mobs.size; }

    @Override
    public void dispose() {
        mossModel.dispose();
        shadeModel.dispose();
        companionModel.dispose();
    }
}
