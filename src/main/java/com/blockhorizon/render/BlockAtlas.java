package com.blockhorizon.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.Random;

public final class BlockAtlas implements AutoCloseable {
    public static final int TILE_SIZE = 16;
    public static final int COLUMNS = 4;
    public static final int ATLAS_SIZE = TILE_SIZE * COLUMNS;

    private final Texture texture;

    public BlockAtlas() {
        Pixmap atlas = new Pixmap(ATLAS_SIZE, ATLAS_SIZE, Pixmap.Format.RGBA8888);
        atlas.setColor(Color.CLEAR);
        atlas.fill();
        paintTile(atlas, 0, 0x65a84fff, 0x82c45fff, 0x3f7c3cff, Pattern.FLECK);
        paintGrassSide(atlas, 1);
        paintTile(atlas, 2, 0x80573cff, 0xa2704bff, 0x543825ff, Pattern.FLECK);
        paintTile(atlas, 3, 0x7e858cff, 0xa1a7adff, 0x5b6168ff, Pattern.FLECK);
        paintTile(atlas, 4, 0xd8c078ff, 0xf1dc96ff, 0xaf9456ff, Pattern.FLECK);
        paintTile(atlas, 5, 0x7e5433ff, 0xa87948ff, 0x53331fff, Pattern.STRIPE);
        paintTile(atlas, 6, 0xa97848ff, 0xc0915cff, 0x68452aff, Pattern.RINGS);
        paintTile(atlas, 7, 0x397d4bff, 0x58a761ff, 0x205b39ff, Pattern.LEAVES);
        paintTile(atlas, 8, 0xeaf5f5ff, 0xffffffff, 0xbcd9e2ff, Pattern.FLECK);
        paintTile(atlas, 9, 0x348ed0d0, 0x58b6e8d0, 0x256eadd0, Pattern.WAVES);
        paintTile(atlas, 10, 0xaa7445ff, 0xc18b56ff, 0x6b452cff, Pattern.PLANKS);
        paintTile(atlas, 11, 0xd5a33bff, 0xffe58bff, 0x885b2aff, Pattern.GLOW);
        paintTile(atlas, 12, 0x6546bdff, 0xb2a3ffff, 0x3e277aff, Pattern.CRYSTAL);
        paintTile(atlas, 13, 0x777e85ff, 0x959ca3ff, 0x4e555cff, Pattern.BRICKS);
        paintTile(atlas, 14, 0x292d33ff, 0x4a4f57ff, 0x101318ff, Pattern.FLECK);
        paintTile(atlas, 15, 0xa86f2eff, 0xe3ad54ff, 0x53351fff, Pattern.CHEST);
        texture = new Texture(atlas);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        atlas.dispose();
    }

    private enum Pattern { FLECK, STRIPE, RINGS, LEAVES, WAVES, PLANKS, GLOW, CRYSTAL, BRICKS, CHEST }

    private void paintTile(Pixmap pixmap, int index, int base, int light, int dark, Pattern pattern) {
        int ox = (index % COLUMNS) * TILE_SIZE;
        int oy = (index / COLUMNS) * TILE_SIZE;
        pixmap.setColor(base);
        pixmap.fillRectangle(ox, oy, TILE_SIZE, TILE_SIZE);
        Random random = new Random(0xB10C000L + index * 991L);
        switch (pattern) {
            case FLECK -> {
                for (int i = 0; i < 42; i++) dot(pixmap, ox, oy, random, i % 3 == 0 ? dark : light, i % 7 == 0 ? 2 : 1);
            }
            case STRIPE -> {
                pixmap.setColor(light);
                for (int x = 1; x < 16; x += 4) pixmap.fillRectangle(ox + x, oy, 2, 16);
                pixmap.setColor(dark);
                for (int x = 3; x < 16; x += 6) pixmap.drawLine(ox + x, oy, ox + x, oy + 15);
            }
            case RINGS -> {
                pixmap.setColor(dark);
                pixmap.drawRectangle(ox + 2, oy + 2, 12, 12);
                pixmap.drawRectangle(ox + 5, oy + 5, 6, 6);
                pixmap.setColor(light);
                pixmap.drawPixel(ox + 7, oy + 7);
            }
            case LEAVES -> {
                for (int i = 0; i < 58; i++) dot(pixmap, ox, oy, random, i % 2 == 0 ? light : dark, i % 5 == 0 ? 2 : 1);
                pixmap.setColor(0x00000000);
                for (int i = 0; i < 12; i++) pixmap.drawPixel(ox + random.nextInt(16), oy + random.nextInt(16));
            }
            case WAVES -> {
                pixmap.setColor(light);
                pixmap.drawLine(ox + 1, oy + 4, ox + 9, oy + 4);
                pixmap.drawLine(ox + 7, oy + 11, ox + 14, oy + 11);
                pixmap.setColor(dark);
                pixmap.drawLine(ox + 3, oy + 7, ox + 13, oy + 7);
            }
            case PLANKS -> {
                pixmap.setColor(dark);
                for (int y = 0; y < 16; y += 4) pixmap.drawLine(ox, oy + y, ox + 15, oy + y);
                pixmap.drawLine(ox + 7, oy + 1, ox + 7, oy + 3);
                pixmap.drawLine(ox + 12, oy + 5, ox + 12, oy + 7);
                pixmap.setColor(light);
                pixmap.drawPixel(ox + 3, oy + 2);
                pixmap.drawPixel(ox + 9, oy + 10);
            }
            case GLOW -> {
                for (int i = 0; i < 50; i++) dot(pixmap, ox, oy, random, i % 4 == 0 ? dark : light, i % 3 == 0 ? 2 : 1);
            }
            case CRYSTAL -> {
                pixmap.setColor(light);
                pixmap.fillRectangle(ox + 6, oy + 1, 4, 14);
                pixmap.fillRectangle(ox + 3, oy + 5, 10, 6);
                pixmap.setColor(0xe4ddffff);
                pixmap.fillRectangle(ox + 7, oy + 2, 1, 10);
                pixmap.setColor(dark);
                pixmap.drawLine(ox + 3, oy + 11, ox + 12, oy + 11);
            }
            case BRICKS -> {
                pixmap.setColor(dark);
                pixmap.drawLine(ox, oy + 5, ox + 15, oy + 5);
                pixmap.drawLine(ox, oy + 11, ox + 15, oy + 11);
                pixmap.drawLine(ox + 7, oy, ox + 7, oy + 5);
                pixmap.drawLine(ox + 3, oy + 6, ox + 3, oy + 11);
                pixmap.drawLine(ox + 11, oy + 12, ox + 11, oy + 15);
                pixmap.setColor(light);
                pixmap.drawPixel(ox + 2, oy + 2);
            }
            case CHEST -> {
                pixmap.setColor(dark);
                pixmap.drawRectangle(ox + 1, oy + 3, 14, 11);
                pixmap.drawLine(ox + 1, oy + 7, ox + 14, oy + 7);
                pixmap.setColor(light);
                pixmap.fillRectangle(ox + 7, oy + 7, 3, 4);
                pixmap.setColor(0x4b3322ff);
                pixmap.drawPixel(ox + 8, oy + 9);
            }
        }
    }

    private void paintGrassSide(Pixmap pixmap, int index) {
        paintTile(pixmap, index, 0x80573cff, 0x9b6b48ff, 0x553723ff, Pattern.FLECK);
        int ox = (index % COLUMNS) * TILE_SIZE;
        int oy = (index / COLUMNS) * TILE_SIZE;
        pixmap.setColor(0x64a64fff);
        pixmap.fillRectangle(ox, oy, 16, 4);
        pixmap.setColor(0x407a39ff);
        for (int x = 0; x < 16; x += 2) pixmap.fillRectangle(ox + x, oy + 4, 1, 1 + x % 3);
    }

    private void dot(Pixmap pixmap, int ox, int oy, Random random, int color, int size) {
        pixmap.setColor(color);
        pixmap.fillRectangle(ox + random.nextInt(16), oy + random.nextInt(16), size, size);
    }

    public Texture texture() { return texture; }

    public float u0(int tile) { return ((tile % COLUMNS) * TILE_SIZE + 0.04f) / ATLAS_SIZE; }
    public float u1(int tile) { return ((tile % COLUMNS + 1) * TILE_SIZE - 0.04f) / ATLAS_SIZE; }
    public float v0(int tile) { return ((tile / COLUMNS) * TILE_SIZE + 0.04f) / ATLAS_SIZE; }
    public float v1(int tile) { return ((tile / COLUMNS + 1) * TILE_SIZE - 0.04f) / ATLAS_SIZE; }

    @Override
    public void close() { texture.dispose(); }
}
