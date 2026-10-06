package com.blockhorizon.player;

import com.badlogic.gdx.math.MathUtils;

public final class PlayerStats {
    private float health = 100f;
    private float hunger = 100f;
    private float stamina = 100f;
    private float regenAccumulator;
    private float starvationAccumulator;

    public void update(float delta, boolean sprinting, boolean moving, boolean creative) {
        if (creative) {
            health = 100f;
            hunger = 100f;
            stamina = Math.min(100f, stamina + 35f * delta);
            return;
        }
        hunger = Math.max(0f, hunger - delta * (sprinting && moving ? 0.38f : 0.17f));
        if (sprinting && moving) stamina = Math.max(0f, stamina - 19f * delta);
        else stamina = Math.min(100f, stamina + 13f * delta);

        if (hunger > 70f && health < 100f) {
            regenAccumulator += delta;
            if (regenAccumulator >= 2.25f) {
                heal(2f);
                hunger = Math.max(0, hunger - 0.8f);
                regenAccumulator = 0f;
            }
        } else regenAccumulator = 0f;

        if (hunger <= 0f) {
            starvationAccumulator += delta;
            if (starvationAccumulator >= 3f) {
                damage(2f);
                starvationAccumulator = 0f;
            }
        } else starvationAccumulator = 0f;
    }

    public void damage(float amount) { health = Math.max(0f, health - Math.max(0f, amount)); }
    public void heal(float amount) { health = Math.min(100f, health + Math.max(0f, amount)); }
    public void feed(float amount) { hunger = Math.min(100f, hunger + Math.max(0f, amount)); }
    public boolean alive() { return health > 0f; }
    public boolean canSprint() { return stamina > 8f && hunger > 5f; }
    public float health() { return health; }
    public float hunger() { return hunger; }
    public float stamina() { return stamina; }

    public void restore(float health, float hunger, float stamina) {
        this.health = MathUtils.clamp(health, 0f, 100f);
        this.hunger = MathUtils.clamp(hunger, 0f, 100f);
        this.stamina = MathUtils.clamp(stamina, 0f, 100f);
    }

    public void respawn() {
        health = 100f;
        hunger = 82f;
        stamina = 100f;
    }
}
