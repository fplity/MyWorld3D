package com.blockhorizon.player;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlayerStatsTest {
    @Test
    public void damageHealingAndFoodStayWithinBounds() {
        PlayerStats stats = new PlayerStats();
        stats.damage(130);
        assertFalse(stats.alive());
        stats.heal(250);
        assertTrue(stats.alive());
        assertEquals(100f, stats.health(), 0.001f);
        stats.feed(500);
        assertEquals(100f, stats.hunger(), 0.001f);
    }

    @Test
    public void creativeModeRestoresVitals() {
        PlayerStats stats = new PlayerStats();
        stats.restore(12, 3, 2);
        stats.update(0.5f, true, true, true);
        assertEquals(100f, stats.health(), 0.001f);
        assertEquals(100f, stats.hunger(), 0.001f);
        assertTrue(stats.stamina() > 2f);
    }
}
