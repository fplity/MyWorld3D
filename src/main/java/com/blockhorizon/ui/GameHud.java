package com.blockhorizon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.blockhorizon.BlockHorizonGame;
import com.blockhorizon.player.Inventory;
import com.blockhorizon.player.PlayerController;
import com.blockhorizon.player.PlayerStats;
import com.blockhorizon.player.Recipe;
import com.blockhorizon.player.RecipeBook;
import com.blockhorizon.render.AtmosphereRenderer;
import com.blockhorizon.world.Biome;
import com.blockhorizon.world.BlockType;
import com.blockhorizon.world.GridPos;
import com.blockhorizon.world.VoxelRaycaster;
import com.blockhorizon.world.VoxelWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class GameHud {
    private static final class Notice {
        final String text;
        final Color color;
        float life;
        final float maxLife;

        Notice(String text, Color color, float life) {
            this.text = text;
            this.color = new Color(color);
            this.life = life;
            this.maxLife = life;
        }
    }

    private final BlockHorizonGame game;
    private final VoxelWorld world;
    private final Inventory inventory;
    private final PlayerController player;
    private final PlayerStats stats;
    private final AtmosphereRenderer atmosphere;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4();
    private final List<Notice> notices = new ArrayList<>();
    private float elapsed;

    public GameHud(BlockHorizonGame game, VoxelWorld world, Inventory inventory, PlayerController player,
                   PlayerStats stats, AtmosphereRenderer atmosphere) {
        this.game = game;
        this.world = world;
        this.inventory = inventory;
        this.player = player;
        this.stats = stats;
        this.atmosphere = atmosphere;
        this.batch = game.batch();
        this.shapes = game.shapes();
    }

    public void update(float delta) {
        elapsed += delta;
        Iterator<Notice> iterator = notices.iterator();
        while (iterator.hasNext()) {
            Notice notice = iterator.next();
            notice.life -= delta;
            if (notice.life <= 0) iterator.remove();
        }
    }

    public void notify(String text) { notify(text, new Color(0xbdeee4ff), 3.5f); }
    public void warn(String text) { notify(text, new Color(0xffba8cff), 3f); }

    public void achievement(String text) {
        notify("成就达成 · " + text, new Color(0xffdb70ff), 5f);
    }

    public void notify(String text, Color color, float life) {
        notices.add(0, new Notice(text, color, life));
        while (notices.size() > 5) notices.remove(notices.size() - 1);
    }

    public void draw(boolean inventoryOpen, boolean mapOpen, boolean paused, boolean dead, boolean debug,
                     boolean hidden, float miningProgress, VoxelRaycaster.Hit target, int questStage, String saveStatus) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0, 0, w, h);
        shapes.setProjectionMatrix(projection);
        batch.setProjectionMatrix(projection);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (mapOpen) drawMapShapes(w, h, questStage);
        boolean showGameplayHud = !hidden && !mapOpen && !inventoryOpen && !paused && !dead;
        if (showGameplayHud) drawHudShapes(w, h, miningProgress, target, questStage, debug);
        if (inventoryOpen) drawInventoryShapes(w, h);
        if (paused) drawPauseShapes(w, h);
        if (dead) drawDeathShapes(w, h);

        batch.begin();
        if (mapOpen) drawMapText(w, h, questStage);
        if (showGameplayHud) drawHudText(w, h, target, questStage, debug, saveStatus);
        if (inventoryOpen) drawInventoryText(w, h);
        if (paused) drawPauseText(w, h);
        if (dead) drawDeathText(w, h);
        batch.end();
    }

    private void drawHudShapes(int w, int h, float miningProgress, VoxelRaycaster.Hit target, int questStage, boolean debug) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        panel(18, h - 74, 390, 51, new Color(0x07131edb), new Color(0x6fdcc34c));
        panel(w - 310, h - 155, 290, 132, new Color(0x07131ed0), new Color(0x6fdcc344));
        drawBar(26, 72, 205, 15, stats.health() / 100f, new Color(0xe25e6bff), new Color(0x3f2028ff));
        drawBar(26, 48, 205, 12, stats.hunger() / 100f, new Color(0xe2b554ff), new Color(0x443822ff));
        drawBar(26, 29, 205, 7, stats.stamina() / 100f, new Color(0x66d8c6ff), new Color(0x193f3aff));

        float slot = MathUtils.clamp(w / 16f, 50f, 64f);
        float hotbarW = slot * Inventory.HOTBAR.size();
        float hotbarX = (w - hotbarW) / 2f;
        for (int i = 0; i < Inventory.HOTBAR.size(); i++) {
            float x = hotbarX + i * slot;
            BlockType type = Inventory.HOTBAR.get(i);
            shapes.setColor(i == inventory.selectedIndex() ? new Color(0x8af5d4ff) : new Color(0x294252dd));
            shapes.rect(x, 18, slot - 3, slot - 3);
            shapes.setColor(new Color(0x0a1721f2));
            shapes.rect(x + 3, 21, slot - 9, slot - 9);
            shapes.setColor(type.color());
            shapes.rect(x + 12, 30, slot - 27, slot - 27);
        }
        if (miningProgress > 0 && target != null) {
            float bw = 250;
            panel((w - bw) / 2f, 94, bw, 16, new Color(0x07131edd), new Color(0x629aa755));
            shapes.setColor(new Color(0x7cebd3ff));
            shapes.rect((w - bw) / 2f + 3, 97, (bw - 6) * MathUtils.clamp(miningProgress, 0, 1), 10);
        }
        int cx = w / 2;
        int cy = h / 2;
        shapes.setColor(new Color(0x08131dcc));
        shapes.rect(cx - 12, cy - 2, 24, 4);
        shapes.rect(cx - 2, cy - 12, 4, 24);
        shapes.setColor(new Color(0xe8fffaff));
        shapes.rect(cx - 9, cy - 1, 18, 2);
        shapes.rect(cx - 1, cy - 9, 2, 18);

        drawNoticeShapes(w, h);
        if (debug) panel(18, h - 310, 310, 215, new Color(0x02070dcc), new Color(0x62899a55));
        shapes.end();
    }

    private void drawNoticeShapes(int w, int h) {
        int count = Math.min(4, notices.size());
        for (int i = 0; i < count; i++) {
            Notice notice = notices.get(i);
            float fade = Math.min(1f, notice.life * 2f) * Math.min(1f, (notice.maxLife - notice.life) * 5f);
            float width = Math.min(560, 120 + notice.text.codePointCount(0, notice.text.length()) * 18f);
            float x = (w - width) / 2f;
            float y = h - 108 - i * 48;
            panel(x, y, width, 38, new Color(0x07131edd), new Color(notice.color.r, notice.color.g, notice.color.b, 0.5f * fade));
        }
    }

    private void drawHudText(int w, int h, VoxelRaycaster.Hit target, int questStage, boolean debug, String saveStatus) {
        int px = MathUtils.floor(player.position().x);
        int pz = MathUtils.floor(player.position().z);
        Biome biome = world.biomeAt(px, pz);
        game.fonts().small.setColor(new Color(0xe5f7f4ff));
        game.fonts().small.draw(batch, biome.displayName() + "  ·  " + atmosphere.clockText() + "  ·  " + atmosphere.weatherName(), 34, h - 40);
        game.fonts().tiny.setColor(new Color(0x87aab7ff));
        game.fonts().tiny.draw(batch, "种子 " + world.seed(), 34, h - 61);

        game.fonts().tiny.setColor(new Color(0x8db2beff));
        game.fonts().tiny.draw(batch, "当前目标", w - 290, h - 48);
        game.fonts().small.setColor(new Color(0xe4f7f2ff));
        game.fonts().small.draw(batch, objective(questStage), w - 290, h - 77, 255, -1, true);
        game.fonts().tiny.setColor(new Color(0x799eabff));
        game.fonts().tiny.draw(batch, "E 背包  ·  M 地图  ·  ESC 暂停", w - 290, h - 137);

        game.fonts().tiny.setColor(new Color(0xffbac0ff));
        game.fonts().tiny.draw(batch, "生命  " + Math.round(stats.health()), 245, 84);
        game.fonts().tiny.setColor(new Color(0xffdc88ff));
        game.fonts().tiny.draw(batch, "饱食  " + Math.round(stats.hunger()), 245, 60);
        game.fonts().tiny.setColor(new Color(0x91e9dcff));
        game.fonts().tiny.draw(batch, "体力  " + Math.round(stats.stamina()), 245, 39);

        float slot = MathUtils.clamp(w / 16f, 50f, 64f);
        float hotbarX = (w - slot * Inventory.HOTBAR.size()) / 2f;
        for (int i = 0; i < Inventory.HOTBAR.size(); i++) {
            BlockType type = Inventory.HOTBAR.get(i);
            game.fonts().tiny.setColor(new Color(0xd9f0efff));
            game.fonts().tiny.draw(batch, String.valueOf(i + 1), hotbarX + i * slot + 6, 18 + slot - 9);
            int count = inventory.count(type);
            String value = inventory.creative() ? "∞" : String.valueOf(count);
            layout.setText(game.fonts().tiny, value);
            game.fonts().tiny.draw(batch, value, hotbarX + (i + 1) * slot - layout.width - 9, 35);
        }
        if (target != null) drawCentered(game.fonts().small, target.type().displayName(), w / 2f, 126, new Color(0xeafcf8dd));
        if (inventory.creative()) {
            game.fonts().tiny.setColor(new Color(0xffdd77ff));
            game.fonts().tiny.draw(batch, "创造模式" + (player.flying() ? " · 飞行" : ""), 26, 111);
        } else if (player.swimming()) {
            game.fonts().tiny.setColor(new Color(0x8bdcffff));
            game.fonts().tiny.draw(batch, "沉浸在水中", 26, 111);
        }
        if (saveStatus != null && !saveStatus.isBlank()) {
            layout.setText(game.fonts().tiny, saveStatus);
            game.fonts().tiny.setColor(new Color(0x8ba6b0cc));
            game.fonts().tiny.draw(batch, saveStatus, w - layout.width - 22, 22);
        }
        drawNoticeText(w, h);
        if (debug) drawDebugText(h);
    }

    private void drawNoticeText(int w, int h) {
        int count = Math.min(4, notices.size());
        for (int i = 0; i < count; i++) {
            Notice notice = notices.get(i);
            float fade = Math.min(1f, notice.life * 2f) * Math.min(1f, (notice.maxLife - notice.life) * 5f);
            game.fonts().small.setColor(notice.color.r, notice.color.g, notice.color.b, fade);
            layout.setText(game.fonts().small, notice.text);
            game.fonts().small.draw(batch, notice.text, (w - layout.width) / 2f, h - 82 - i * 48);
        }
    }

    private void drawDebugText(int h) {
        game.fonts().tiny.setColor(new Color(0xb7ced7ff));
        float x = 32;
        float y = h - 118;
        game.fonts().tiny.draw(batch, "调试信息", x, y); y -= 27;
        game.fonts().tiny.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), x, y); y -= 24;
        game.fonts().tiny.draw(batch, String.format("坐标: %.2f / %.2f / %.2f", player.position().x, player.position().y, player.position().z), x, y); y -= 24;
        game.fonts().tiny.draw(batch, String.format("速度: %.2f / %.2f / %.2f", player.velocity().x, player.velocity().y, player.velocity().z), x, y); y -= 24;
        game.fonts().tiny.draw(batch, "方块: " + world.blockCount(), x, y); y -= 24;
        game.fonts().tiny.draw(batch, "朝向: " + Math.round(player.yaw()) + "° / " + Math.round(player.pitch()) + "°", x, y); y -= 24;
        game.fonts().tiny.draw(batch, "F1 隐藏界面  F2 截图  F5 保存", x, y);
    }

    private void drawInventoryShapes(int w, int h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0x02070dcc));
        shapes.rect(0, 0, w, h);
        float pw = Math.min(1050, w - 70);
        float ph = Math.min(670, h - 70);
        float x = (w - pw) / 2f;
        float y = (h - ph) / 2f;
        panel(x, y, pw, ph, new Color(0x091925f5), new Color(0x6fe1ca77));
        shapes.setColor(new Color(0x1d3443ff));
        shapes.rect(x + pw * 0.54f, y + 62, 2, ph - 124);
        int index = 0;
        float cell = Math.min(92, (pw * 0.48f - 54) / 4f);
        for (BlockType type : BlockType.values()) {
            if (type == BlockType.BEDROCK || type == BlockType.WATER) continue;
            int col = index % 4;
            int row = index / 4;
            float cx = x + 34 + col * cell;
            float cy = y + ph - 155 - row * cell;
            panel(cx, cy, cell - 10, cell - 10, new Color(0x102635ff), new Color(0x35596aff));
            shapes.setColor(type.color());
            shapes.rect(cx + 13, cy + 28, 30, 30);
            index++;
        }
        for (int i = 0; i < RecipeBook.RECIPES.size(); i++) {
            float rx = x + pw * 0.58f;
            float ry = y + ph - 162 - i * 88;
            boolean can = inventory.canCraft(RecipeBook.RECIPES.get(i));
            boolean hover = recipeAt(Gdx.input.getX(), Gdx.input.getY(), w, h) == i;
            panel(rx, ry, pw * 0.38f, 70, new Color(hover ? 0x1c4652ff : 0x112c3aff),
                    new Color(can ? 0x62d8b888 : 0x6e657088));
        }
        drawBar(x + 34, y + 45, pw * 0.42f, 10, stats.hunger() / 100f, new Color(0xe2b554ff), new Color(0x443822ff));
        shapes.end();
    }

    private void drawInventoryText(int w, int h) {
        float pw = Math.min(1050, w - 70);
        float ph = Math.min(670, h - 70);
        float x = (w - pw) / 2f;
        float y = (h - ph) / 2f;
        game.fonts().heading.setColor(new Color(0xd7fff5ff));
        game.fonts().heading.draw(batch, "背包与合成", x + 34, y + ph - 35);
        game.fonts().tiny.setColor(new Color(0x7fa2afff));
        game.fonts().tiny.draw(batch, "点击配方，或按 1—4 快速合成", x + pw * 0.58f, y + ph - 64);
        int index = 0;
        float cell = Math.min(92, (pw * 0.48f - 54) / 4f);
        for (BlockType type : BlockType.values()) {
            if (type == BlockType.BEDROCK || type == BlockType.WATER) continue;
            int col = index % 4;
            int row = index / 4;
            float cx = x + 34 + col * cell;
            float cy = y + ph - 155 - row * cell;
            game.fonts().tiny.setColor(new Color(0xc9e1e7ff));
            game.fonts().tiny.draw(batch, type.displayName(), cx + 10, cy + 22);
            String count = inventory.creative() ? "∞" : String.valueOf(inventory.count(type));
            layout.setText(game.fonts().small, count);
            game.fonts().small.setColor(new Color(0xf1fffaff));
            game.fonts().small.draw(batch, count, cx + cell - layout.width - 20, cy + 52);
            index++;
        }
        game.fonts().small.setColor(new Color(0xffcf7dff));
        game.fonts().small.draw(batch, "浆果 × " + inventory.berries() + "   ·   饱食度", x + 34, y + 77);
        for (int i = 0; i < RecipeBook.RECIPES.size(); i++) {
            Recipe recipe = RecipeBook.RECIPES.get(i);
            float rx = x + pw * 0.58f;
            float ry = y + ph - 162 - i * 88;
            boolean can = inventory.canCraft(recipe);
            game.fonts().small.setColor(can ? new Color(0xddfff6ff) : new Color(0xa7949bff));
            game.fonts().small.draw(batch, (i + 1) + "  " + recipe.name(), rx + 16, ry + 49);
            game.fonts().tiny.setColor(can ? new Color(0x8fbbb4ff) : new Color(0x8f747bff));
            game.fonts().tiny.draw(batch, recipe.description() + "  ·  " + costText(recipe), rx + 16, ry + 24);
        }
        game.fonts().tiny.setColor(new Color(0x789aa8ff));
        game.fonts().tiny.draw(batch, "R 吃浆果  ·  C 切换创造  ·  F 飞行（创造）  ·  E 关闭", x + pw * 0.54f, y + 30);
    }

    private String costText(Recipe recipe) {
        StringBuilder result = new StringBuilder("消耗 ");
        recipe.cost().forEach((type, count) -> result.append(type.displayName()).append('×').append(count).append(' '));
        return result.toString().trim();
    }

    private void drawMapShapes(int w, int h, int questStage) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0x02070de8));
        shapes.rect(0, 0, w, h);
        float size = Math.min(610, h - 120);
        float x = (w - size) / 2f;
        float y = (h - size) / 2f;
        panel(x - 18, y - 18, size + 36, size + 36, new Color(0x091925f5), new Color(0x6fe1ca88));
        float cell = size / 32f;
        for (int ix = 0; ix < 32; ix++) {
            for (int iz = 0; iz < 32; iz++) {
                int wx = -32 + ix * 2;
                int wz = -32 + iz * 2;
                int surface = world.highestSolidY(wx, wz);
                BlockType block = world.get(wx, surface, wz);
                Color color = block == null ? new Color(0x172a35ff) : block.color();
                float shade = 0.72f + surface / 45f;
                shapes.setColor(color.r * shade, color.g * shade, color.b * shade, 1f);
                shapes.rect(x + ix * cell, y + (31 - iz) * cell, cell + 0.3f, cell + 0.3f);
            }
        }
        float playerX = x + (player.position().x + 32f) / 64f * size;
        float playerY = y + (32f - player.position().z) / 64f * size;
        shapes.setColor(new Color(0xfff29cff));
        shapes.triangle(playerX, playerY + 9, playerX - 7, playerY - 7, playerX + 7, playerY - 7);
        if (questStage >= 3 && world.cabinCenter() != null) marker(x, y, size, world.cabinCenter(), new Color(0xf0c275ff));
        if (questStage >= 4 && world.shrineCenter() != null) marker(x, y, size, world.shrineCenter(), new Color(0xbd9affff));
        shapes.end();
    }

    private void marker(float x, float y, float size, GridPos pos, Color color) {
        float mx = x + (pos.x() + 32f) / 64f * size;
        float my = y + (32f - pos.z()) / 64f * size;
        shapes.setColor(new Color(0x09131ddd));
        shapes.circle(mx, my, 8);
        shapes.setColor(color);
        shapes.circle(mx, my, 5);
    }

    private void drawMapText(int w, int h, int questStage) {
        float size = Math.min(610, h - 120);
        float x = (w - size) / 2f;
        float y = (h - size) / 2f;
        drawCentered(game.fonts().heading, "旅者地图", w / 2f, y + size + 55, new Color(0xd7fff5ff));
        drawCentered(game.fonts().tiny, "北 ↑    M 或 ESC 关闭地图", w / 2f, y - 30, new Color(0x8baab5ff));
        game.fonts().tiny.setColor(new Color(0xffe39bff));
        game.fonts().tiny.draw(batch, "▲ 你", x + 8, y + size + 28);
        if (questStage >= 3) {
            game.fonts().tiny.setColor(new Color(0xf0c275ff));
            game.fonts().tiny.draw(batch, "● 林间小屋", x + 90, y + size + 28);
        }
        if (questStage >= 4) {
            game.fonts().tiny.setColor(new Color(0xbd9affff));
            game.fonts().tiny.draw(batch, "● 回响遗迹", x + 220, y + size + 28);
        }
    }

    private void drawPauseShapes(int w, int h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0x02070dd5));
        shapes.rect(0, 0, w, h);
        float x = w / 2f - 230;
        float y = h / 2f - 210;
        panel(x, y, 460, 430, new Color(0x091925f7), new Color(0x6fe1ca88));
        for (int i = 0; i < 3; i++) {
            float by = y + 218 - i * 72;
            boolean hover = pauseAction(Gdx.input.getX(), Gdx.input.getY(), w, h) == i + 1;
            panel(x + 50, by, 360, 54, new Color(hover ? 0x205464ff : 0x133242ff), new Color(0x65ccb688));
        }
        shapes.end();
    }

    private void drawPauseText(int w, int h) {
        float y = h / 2f - 210;
        drawCentered(game.fonts().heading, "旅程暂停", w / 2f, y + 375, new Color(0xd7fff5ff));
        drawCentered(game.fonts().tiny, "世界在这里静静等你", w / 2f, y + 337, new Color(0x86a8b4ff));
        drawCentered(game.fonts().body, "继续游戏", w / 2f, y + 252, Color.WHITE);
        drawCentered(game.fonts().body, "保存世界", w / 2f, y + 180, Color.WHITE);
        drawCentered(game.fonts().body, "保存并返回标题", w / 2f, y + 108, Color.WHITE);
        drawCentered(game.fonts().tiny, "ESC 也可以继续", w / 2f, y + 36, new Color(0x7697a5ff));
    }

    private void drawDeathShapes(int w, int h) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(new Color(0x3d0910c8));
        shapes.rect(0, 0, w, h);
        panel(w / 2f - 260, h / 2f - 150, 520, 310, new Color(0x160c14f2), new Color(0xe56e7988));
        panel(w / 2f - 190, h / 2f - 60, 380, 56, new Color(0x47212aff), new Color(0xe7898fff));
        panel(w / 2f - 190, h / 2f - 132, 380, 52, new Color(0x291e2aff), new Color(0xa56f7d88));
        shapes.end();
    }

    private void drawDeathText(int w, int h) {
        drawCentered(game.fonts().heading, "你倒下了", w / 2f, h / 2f + 105, new Color(0xffb1b7ff));
        drawCentered(game.fonts().small, "别担心，方境会为你保留走过的痕迹", w / 2f, h / 2f + 60, new Color(0xc9aeb6ff));
        drawCentered(game.fonts().body, "R  重生", w / 2f, h / 2f - 24, Color.WHITE);
        drawCentered(game.fonts().small, "ESC  返回标题", w / 2f, h / 2f - 99, new Color(0xe4ccd2ff));
    }

    public int recipeAt(int screenX, int screenY, int w, int h) {
        float py = h - screenY;
        float pw = Math.min(1050, w - 70);
        float ph = Math.min(670, h - 70);
        float x = (w - pw) / 2f;
        float y = (h - ph) / 2f;
        float rx = x + pw * 0.58f;
        for (int i = 0; i < RecipeBook.RECIPES.size(); i++) {
            float ry = y + ph - 162 - i * 88;
            if (contains(screenX, py, rx, ry, pw * 0.38f, 70)) return i;
        }
        return -1;
    }

    public int pauseAction(int screenX, int screenY, int w, int h) {
        float py = h - screenY;
        float x = w / 2f - 230;
        float y = h / 2f - 210;
        for (int i = 0; i < 3; i++) if (contains(screenX, py, x + 50, y + 218 - i * 72, 360, 54)) return i + 1;
        return 0;
    }

    private boolean contains(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    private String objective(int stage) {
        return switch (stage) {
            case 0 -> "收集 3 块原木";
            case 1 -> "合成第一批木板";
            case 2 -> "寻找林间的小屋";
            case 3 -> "循着低语寻找遗迹";
            case 4 -> "让回响晶体苏醒";
            default -> "任务完成，自由探索方境";
        };
    }

    private void panel(float x, float y, float w, float h, Color fill, Color border) {
        shapes.setColor(border);
        shapes.rect(x - 2, y - 2, w + 4, h + 4);
        shapes.setColor(fill);
        shapes.rect(x, y, w, h);
    }

    private void drawBar(float x, float y, float w, float h, float value, Color fill, Color back) {
        shapes.setColor(back);
        shapes.rect(x, y, w, h);
        shapes.setColor(fill);
        shapes.rect(x + 2, y + 2, (w - 4) * MathUtils.clamp(value, 0, 1), h - 4);
    }

    private void drawCentered(BitmapFont font, String text, float x, float y, Color color) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, x - layout.width / 2f, y);
    }
}
