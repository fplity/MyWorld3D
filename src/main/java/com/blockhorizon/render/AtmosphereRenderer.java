package com.blockhorizon.render;

import com.badlogic.gdx.Gdx;
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
import com.badlogic.gdx.graphics.g3d.attributes.DepthTestAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.blockhorizon.GameConfig;

import java.util.Random;

public final class AtmosphereRenderer implements Disposable {
    private static final long MODEL_ATTRS = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static final Color NIGHT = new Color(0x071128ff);
    private static final Color DAWN = new Color(0xd77e72ff);
    private static final Color DAY = new Color(0x78cfffff);
    private static final Color STORM = new Color(0x35485cff);

    private final Environment environment = new Environment();
    private final DirectionalLight sunlight = new DirectionalLight();
    private final Material skyMaterial;
    private final Material cloudMaterial;
    private final Model skyModel;
    private final Model sunModel;
    private final Model moonModel;
    private final Model cloudModel;
    private final Model starModel;
    private final ModelInstance sky;
    private final ModelInstance sun;
    private final ModelInstance moon;
    private final Array<ModelInstance> clouds = new Array<>();
    private final Array<ModelInstance> stars = new Array<>();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Random random;
    private final Vector3 temp = new Vector3();
    private final Color skyColor = new Color();
    private float timeOfDay = 0.17f;
    private float weatherTimer;
    private float rainAmount;
    private float rainPhase;
    private boolean raining;
    private float lightningFlash;
    private boolean lightningEvent;
    private float meteorTimer;
    private float meteorLife;
    private final Vector3 meteorStart = new Vector3();
    private final Vector3 meteorEnd = new Vector3();
    private boolean meteorEvent;

    public AtmosphereRenderer(long seed) {
        random = new Random(seed ^ 0xA7F05C1L);
        environment.set(ColorAttribute.createAmbientLight(0.64f, 0.67f, 0.72f, 1f));
        environment.set(ColorAttribute.createFog(DAY));
        environment.add(sunlight.set(1f, 0.96f, 0.84f, -0.6f, -1f, -0.35f));

        ModelBuilder builder = new ModelBuilder();
        skyMaterial = new Material(ColorAttribute.createDiffuse(DAY), IntAttribute.createCullFace(GL20.GL_FRONT),
                new DepthTestAttribute(GL20.GL_LEQUAL, false));
        skyModel = builder.createSphere(180f, 180f, 180f, 28, 18, skyMaterial, MODEL_ATTRS);
        sunModel = builder.createSphere(6f, 6f, 6f, 14, 10,
                new Material(ColorAttribute.createDiffuse(new Color(0xffe995ff)), ColorAttribute.createEmissive(1f, 0.78f, 0.2f, 1f)), MODEL_ATTRS);
        moonModel = builder.createSphere(4.8f, 4.8f, 4.8f, 14, 10,
                new Material(ColorAttribute.createDiffuse(new Color(0xdde7ffff)), ColorAttribute.createEmissive(0.4f, 0.5f, 0.72f, 1f)), MODEL_ATTRS);
        cloudMaterial = new Material(ColorAttribute.createDiffuse(new Color(0xf3fbffff)), new BlendingAttribute(0.68f));
        cloudModel = builder.createBox(1f, 1f, 1f, cloudMaterial, MODEL_ATTRS);
        starModel = builder.createSphere(0.28f, 0.28f, 0.28f, 6, 4,
                new Material(ColorAttribute.createDiffuse(Color.WHITE), ColorAttribute.createEmissive(Color.WHITE)), MODEL_ATTRS);
        sky = new ModelInstance(skyModel);
        sun = new ModelInstance(sunModel);
        moon = new ModelInstance(moonModel);
        createClouds();
        createStars();
        weatherTimer = 65f + random.nextFloat() * 80f;
        meteorTimer = 35f + random.nextFloat() * 75f;
    }

    private void createClouds() {
        for (int i = 0; i < 38; i++) {
            ModelInstance cloud = new ModelInstance(cloudModel);
            float x = -58f + random.nextFloat() * 116f;
            float y = 22f + random.nextFloat() * 8f;
            float z = -58f + random.nextFloat() * 116f;
            cloud.transform.setToTranslation(x, y, z).scale(4f + random.nextFloat() * 7f, 0.65f + random.nextFloat() * 0.65f, 2f + random.nextFloat() * 3f);
            clouds.add(cloud);
        }
    }

    private void createStars() {
        for (int i = 0; i < 95; i++) {
            float theta = random.nextFloat() * MathUtils.PI2;
            float phi = 0.15f + random.nextFloat() * 1.25f;
            float radius = 77f;
            ModelInstance star = new ModelInstance(starModel);
            star.transform.setToTranslation(MathUtils.cos(theta) * MathUtils.cos(phi) * radius,
                    MathUtils.sin(phi) * radius,
                    MathUtils.sin(theta) * MathUtils.cos(phi) * radius);
            float scale = 0.65f + random.nextFloat() * 1.6f;
            star.transform.scale(scale, scale, scale);
            stars.add(star);
        }
    }

    public void update(float delta, PerspectiveCamera camera, boolean paused) {
        if (!paused) {
            rainPhase = (rainPhase + delta * 19f) % 26f;
            timeOfDay = (timeOfDay + delta / GameConfig.DAY_LENGTH_SECONDS) % 1f;
            weatherTimer -= delta;
            meteorTimer -= delta;
            if (weatherTimer <= 0f) {
                raining = !raining && random.nextFloat() < 0.62f;
                weatherTimer = raining ? 38f + random.nextFloat() * 52f : 70f + random.nextFloat() * 100f;
            }
            rainAmount = MathUtils.lerp(rainAmount, raining ? 1f : 0f, 1f - (float) Math.pow(0.04f, delta));
            if (raining && rainAmount > 0.7f && random.nextFloat() < delta * 0.018f) {
                lightningFlash = 1f;
                lightningEvent = true;
            }
            lightningFlash = Math.max(0f, lightningFlash - delta * 3.2f);
            if (isNight() && meteorTimer <= 0f && meteorLife <= 0f && random.nextFloat() < 0.15f) startMeteor(camera);
            if (meteorTimer <= -8f) meteorTimer = 35f + random.nextFloat() * 85f;
            if (meteorLife > 0f) meteorLife -= delta;
        }

        float angle = timeOfDay * MathUtils.PI2;
        float sunHeight = MathUtils.sin(angle);
        float daylight = smoothStep(-0.2f, 0.25f, sunHeight);
        float dawnMix = 1f - Math.min(1f, Math.abs(sunHeight) * 4f);
        skyColor.set(NIGHT).lerp(DAY, daylight).lerp(DAWN, dawnMix * (0.8f - daylight * 0.35f));
        if (rainAmount > 0f) skyColor.lerp(STORM, rainAmount * 0.62f);
        if (lightningFlash > 0f) skyColor.lerp(Color.WHITE, lightningFlash * 0.72f);
        ((ColorAttribute) skyMaterial.get(ColorAttribute.Diffuse)).color.set(skyColor);
        ((ColorAttribute) environment.get(ColorAttribute.AmbientLight)).color.set(
                0.1f + daylight * 0.34f + lightningFlash * 0.5f,
                0.12f + daylight * 0.34f + lightningFlash * 0.5f,
                0.2f + daylight * 0.29f + lightningFlash * 0.5f,
                1f);
        ((ColorAttribute) environment.get(ColorAttribute.Fog)).color.set(skyColor);
        sunlight.set(0.2f + daylight * 0.58f, 0.22f + daylight * 0.54f, 0.34f + daylight * 0.42f,
                -MathUtils.cos(angle), -Math.max(0.12f, sunHeight), -0.32f);

        sky.transform.setToTranslation(camera.position);
        temp.set(MathUtils.cos(angle), sunHeight, -0.26f).nor().scl(72f).add(camera.position);
        sun.transform.setToTranslation(temp);
        temp.set(MathUtils.cos(angle + MathUtils.PI), -sunHeight, 0.26f).nor().scl(72f).add(camera.position);
        moon.transform.setToTranslation(temp);
        for (int i = 0; i < clouds.size; i++) {
            ModelInstance cloud = clouds.get(i);
            if (!paused) cloud.transform.trn(delta * (0.7f + (i % 5) * 0.08f), 0, 0);
            cloud.transform.getTranslation(temp);
            if (temp.x > camera.position.x + 70f) cloud.transform.trn(-140f, 0, 0);
        }
    }

    private void startMeteor(PerspectiveCamera camera) {
        meteorLife = 1.35f;
        meteorStart.set(camera.position).add(-25f + random.nextFloat() * 50f, 33f + random.nextFloat() * 13f, -35f + random.nextFloat() * 30f);
        meteorEnd.set(meteorStart).add(18f, -10f, 9f);
        meteorEvent = true;
        meteorTimer = 55f + random.nextFloat() * 85f;
    }

    public void renderModels(ModelBatch batch) {
        batch.render(sky);
        if (isNight()) {
            for (ModelInstance star : stars) batch.render(star);
            batch.render(moon);
        } else batch.render(sun);
        for (ModelInstance cloud : clouds) batch.render(cloud, environment);
    }

    public void renderWeather(PerspectiveCamera camera) {
        if (rainAmount <= 0.02f && meteorLife <= 0f) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (rainAmount > 0.02f) {
            shapes.setColor(0.68f, 0.84f, 1f, 0.28f * rainAmount);
            float t = rainPhase;
            for (int i = 0; i < 210; i++) {
                float x = camera.position.x + hash(i * 17) * 40f - 20f;
                float z = camera.position.z + hash(i * 31 + 7) * 40f - 20f;
                float y = camera.position.y - 7f + Math.floorMod((int) ((hash(i * 47 + 13) * 26f - t) * 10), 260) / 10f;
                shapes.line(x, y, z, x - 0.07f, y - 1.25f, z + 0.04f);
            }
        }
        if (meteorLife > 0f) {
            float alpha = Math.min(1f, meteorLife * 2.5f);
            shapes.setColor(0.75f, 0.88f, 1f, alpha);
            shapes.line(meteorStart, meteorEnd);
            shapes.setColor(0.55f, 0.35f, 1f, alpha * 0.55f);
            shapes.line(new Vector3(meteorStart).add(-2f, 1f, -1f), meteorEnd);
        }
        shapes.end();
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
    }

    private float hash(int value) {
        int n = value * 0x45d9f3b;
        n = (n ^ (n >>> 16)) * 0x45d9f3b;
        n ^= n >>> 16;
        return (n & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float t = MathUtils.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public Environment environment() { return environment; }
    public float timeOfDay() { return timeOfDay; }
    public void setTimeOfDay(float time) { timeOfDay = MathUtils.clamp(time, 0f, 1f); }
    public boolean isNight() { return MathUtils.sin(timeOfDay * MathUtils.PI2) < -0.18f; }
    public boolean raining() { return rainAmount > 0.35f; }
    public String weatherName() { return raining() ? "细雨" : "晴朗"; }
    public Color skyColor() { return skyColor; }

    public String clockText() {
        float hours = (timeOfDay * 24f + 6f) % 24f;
        int hour = (int) hours;
        int minute = (int) ((hours - hour) * 60f);
        return String.format("%02d:%02d", hour, minute);
    }

    public void toggleWeather() {
        raining = !raining;
        weatherTimer = raining ? 55f : 90f;
    }

    public void clearWeather() {
        raining = false;
        weatherTimer = 120f;
    }

    public boolean consumeLightningEvent() {
        boolean value = lightningEvent;
        lightningEvent = false;
        return value;
    }

    public boolean consumeMeteorEvent() {
        boolean value = meteorEvent;
        meteorEvent = false;
        return value;
    }

    @Override
    public void dispose() {
        skyModel.dispose();
        sunModel.dispose();
        moonModel.dispose();
        cloudModel.dispose();
        starModel.dispose();
        shapes.dispose();
    }
}
