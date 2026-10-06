package com.blockhorizon.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.Random;

public final class ParticleSystem implements Disposable {
    private static final class Particle {
        final Vector3 position = new Vector3();
        final Vector3 velocity = new Vector3();
        final Color color = new Color();
        float life;
        float maxLife;
    }

    private final Array<Particle> particles = new Array<>();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Random random = new Random();

    public void burst(Vector3 center, Color color, int amount, float energy) {
        for (int i = 0; i < amount && particles.size < 600; i++) {
            Particle particle = new Particle();
            particle.position.set(center).add(randomRange(0.42f), randomRange(0.42f), randomRange(0.42f));
            particle.velocity.set(randomRange(energy), random.nextFloat() * energy + 0.6f, randomRange(energy));
            particle.color.set(color).lerp(Color.WHITE, random.nextFloat() * 0.22f);
            particle.maxLife = particle.life = 0.42f + random.nextFloat() * 0.55f;
            particles.add(particle);
        }
    }

    public void sparkle(Vector3 center, Color color) {
        burst(center, color, 28, 4.4f);
    }

    public void update(float delta) {
        for (int i = particles.size - 1; i >= 0; i--) {
            Particle particle = particles.get(i);
            particle.life -= delta;
            if (particle.life <= 0) {
                particles.removeIndex(i);
                continue;
            }
            particle.velocity.y -= 8.5f * delta;
            particle.velocity.scl(1f - Math.min(0.8f, delta * 1.2f));
            particle.position.mulAdd(particle.velocity, delta);
        }
    }

    public void render(PerspectiveCamera camera) {
        if (particles.isEmpty()) return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (Particle particle : particles) {
            float alpha = particle.life / particle.maxLife;
            shapes.setColor(particle.color.r, particle.color.g, particle.color.b, alpha);
            shapes.line(particle.position.x, particle.position.y, particle.position.z,
                    particle.position.x - particle.velocity.x * 0.025f,
                    particle.position.y - particle.velocity.y * 0.025f,
                    particle.position.z - particle.velocity.z * 0.025f);
        }
        shapes.end();
    }

    private float randomRange(float range) { return (random.nextFloat() * 2f - 1f) * range; }
    @Override public void dispose() { shapes.dispose(); }
}
