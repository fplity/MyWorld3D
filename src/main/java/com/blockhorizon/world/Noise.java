package com.blockhorizon.world;

public final class Noise {
    private Noise() {
    }

    public static long seedHash(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static double hash(int x, int y, int z, long seed) {
        long n = x * 0x632BE59BD9B4E019L ^ y * 0x9E3779B97F4A7C15L ^ z * 0x94D049BB133111EBL ^ seed;
        n ^= n >>> 30;
        n *= 0xBF58476D1CE4E5B9L;
        n ^= n >>> 27;
        n *= 0x94D049BB133111EBL;
        n ^= n >>> 31;
        return ((n >>> 11) & ((1L << 53) - 1)) / (double) (1L << 53) * 2.0 - 1.0;
    }

    private static double fade(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double value2(double x, double z, long seed) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double tz = fade(z - z0);
        double a = hash(x0, 0, z0, seed);
        double b = hash(x0 + 1, 0, z0, seed);
        double c = hash(x0, 0, z0 + 1, seed);
        double d = hash(x0 + 1, 0, z0 + 1, seed);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    public static double value3(double x, double y, double z, long seed) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double ty = fade(y - y0);
        double tz = fade(z - z0);
        double x00 = lerp(hash(x0, y0, z0, seed), hash(x0 + 1, y0, z0, seed), tx);
        double x10 = lerp(hash(x0, y0 + 1, z0, seed), hash(x0 + 1, y0 + 1, z0, seed), tx);
        double x01 = lerp(hash(x0, y0, z0 + 1, seed), hash(x0 + 1, y0, z0 + 1, seed), tx);
        double x11 = lerp(hash(x0, y0 + 1, z0 + 1, seed), hash(x0 + 1, y0 + 1, z0 + 1, seed), tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    public static double fbm2(double x, double z, long seed, int octaves) {
        double value = 0;
        double amplitude = 0.5;
        double total = 0;
        double frequency = 1;
        for (int i = 0; i < octaves; i++) {
            value += value2(x * frequency, z * frequency, seed + i * 1013L) * amplitude;
            total += amplitude;
            frequency *= 2.03;
            amplitude *= 0.5;
        }
        return value / total;
    }

    public static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }
}
