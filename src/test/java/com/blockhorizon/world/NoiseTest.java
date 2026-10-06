package com.blockhorizon.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class NoiseTest {
    @Test
    public void noiseIsDeterministicAndBounded() {
        long seed = Noise.seedHash("测试种子");
        double first = Noise.fbm2(1.25, -8.75, seed, 5);
        assertEquals(first, Noise.fbm2(1.25, -8.75, seed, 5), 0.0);
        assertTrue(first >= -1.0 && first <= 1.0);
        assertNotEquals(first, Noise.fbm2(1.25, -8.75, seed + 1, 5), 0.0);
    }

    @Test
    public void negativeCoordinatesUseMathematicalFloor() {
        assertEquals(-2, Noise.fastFloor(-1.01));
        assertEquals(-1, Noise.fastFloor(-1.0));
        assertEquals(1, Noise.fastFloor(1.99));
    }
}
