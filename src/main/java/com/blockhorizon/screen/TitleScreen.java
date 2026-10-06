package com.blockhorizon.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.blockhorizon.BlockHorizonGame;
import com.blockhorizon.GameConfig;
import com.blockhorizon.desktop.SmokeEvidence;
import com.blockhorizon.world.WorldGenerator;

public final class TitleScreen extends InputAdapter implements Screen {
    private final BlockHorizonGame game;
    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final Matrix4 projection = new Matrix4();
    private final GlyphLayout layout = new GlyphLayout();
    private String seed = WorldGenerator.randomSeed();
    private boolean seedFocused;
    private float elapsed;

    public TitleScreen(BlockHorizonGame game) {
        this.game = game;
        this.batch = game.batch();
        this.shapes = game.shapes();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(Boolean.getBoolean("blockhorizon.smoke") ? null : this);
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0, 0, width, height);
        Gdx.gl.glClearColor(0.025f, 0.055f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, width, height,
                new Color(0x07101fff), new Color(0x07101fff), new Color(0x246178ff), new Color(0x17394fff));
        drawStars(width, height);
        drawFloatingIslands(width, height);
        float panelWidth = Math.min(610, width - 64);
        float panelX = (width - panelWidth) / 2f;
        float panelY = height * 0.10f;
        float panelH = Math.min(410, height * 0.52f);
        panel(panelX, panelY, panelWidth, panelH, new Color(0x081624df), new Color(0x56d9c048));

        float seedY = panelY + panelH - 104;
        panel(panelX + 44, seedY, panelWidth - 88, 58, new Color(seedFocused ? 0x173b4fff : 0x0d2636ff),
                new Color(seedFocused ? 0x72efd5aa : 0x37677aaa));
        float mainButtonY = seedY - 88;
        buttonShape(panelX + 44, mainButtonY, panelWidth - 88, 60, isHover(panelX + 44, mainButtonY, panelWidth - 88, 60), true);
        if (game.saves().exists()) buttonShape(panelX + 44, mainButtonY - 76, panelWidth - 88, 56,
                isHover(panelX + 44, mainButtonY - 76, panelWidth - 88, 56), false);
        float randomX = panelX + panelWidth - 140;
        buttonShape(randomX, seedY + 13, 76, 32, isHover(randomX, seedY + 13, 76, 32), false);
        shapes.end();

        batch.setProjectionMatrix(projection);
        batch.begin();
        float titleY = height * 0.84f + MathUtils.sin(elapsed * 1.2f) * 4f;
        drawCentered(game.fonts().title, "方境", width / 2f, titleY, new Color(0xc9fff3ff));
        drawCentered(game.fonts().heading, "B L O C K H O R I Z O N", width / 2f, titleY - 62, new Color(0x7cebd3ff));
        drawCentered(game.fonts().small, "每一次出发，世界都会记住你的故事", width / 2f, titleY - 102, new Color(0xc6dbe4dd));

        game.fonts().small.setColor(new Color(0x8eb8caff));
        game.fonts().small.draw(batch, "世界种子", panelX + 50, seedY + 86);
        game.fonts().body.setColor(Color.WHITE);
        String visibleSeed = seed + (seedFocused && ((int) (elapsed * 2) & 1) == 0 ? "▏" : "");
        game.fonts().body.draw(batch, visibleSeed, panelX + 62, seedY + 39);
        game.fonts().small.setColor(new Color(0xa8e8d9ff));
        game.fonts().small.draw(batch, "随机", randomX + 17, seedY + 37);
        drawCentered(game.fonts().body, "创建新世界", width / 2f, mainButtonY + 39, new Color(0xf3fffcff));
        if (game.saves().exists()) drawCentered(game.fonts().body, "继续上次旅程", width / 2f, mainButtonY - 40, new Color(0xdcecf2ff));

        float hintY = panelY + 36;
        drawCentered(game.fonts().tiny, "WASD 移动  ·  鼠标探索  ·  左键采集  ·  右键放置  ·  E 背包", width / 2f, hintY,
                new Color(0x9bb5c2ff));
        game.fonts().tiny.setColor(new Color(0x7393a1cc));
        game.fonts().tiny.draw(batch, "Java + LibGDX  ·  程序化世界  ·  自动存档", 22, 28);
        layout.setText(game.fonts().tiny, "ESC 退出");
        game.fonts().tiny.draw(batch, "ESC 退出", width - layout.width - 22, 28);
        batch.end();
        runSmokeHook(width, height);
    }

    private void drawStars(int width, int height) {
        for (int i = 0; i < 56; i++) {
            float x = ((i * 193.7f) % width + MathUtils.sin(elapsed * 0.08f + i) * 8f + width) % width;
            float y = height * 0.38f + ((i * 97.3f) % (height * 0.62f));
            float alpha = 0.2f + (MathUtils.sin(elapsed * 1.4f + i * 2.1f) + 1f) * 0.2f;
            shapes.setColor(0.72f, 0.96f, 1f, alpha);
            float size = i % 9 == 0 ? 3f : 1.5f;
            shapes.rect(x, y, size, size);
        }
    }

    private void drawFloatingIslands(int width, int height) {
        drawCube(width * 0.12f, height * 0.65f + MathUtils.sin(elapsed) * 8f, 84f, new Color(0x4b9165aa));
        drawCube(width * 0.84f, height * 0.72f + MathUtils.sin(elapsed * 0.8f + 2) * 10f, 105f, new Color(0x6a77b6aa));
        drawCube(width * 0.91f, height * 0.32f + MathUtils.sin(elapsed * 1.1f) * 6f, 54f, new Color(0xc59657aa));
    }

    private void drawCube(float cx, float cy, float size, Color top) {
        shapes.setColor(top);
        shapes.triangle(cx, cy + size * 0.5f, cx + size, cy, cx, cy - size * 0.5f);
        shapes.triangle(cx, cy + size * 0.5f, cx - size, cy, cx, cy - size * 0.5f);
        shapes.setColor(top.r * 0.56f, top.g * 0.62f, top.b * 0.67f, top.a);
        shapes.triangle(cx - size, cy, cx, cy - size * 0.5f, cx, cy - size * 1.35f);
        shapes.triangle(cx - size, cy, cx - size, cy - size * 0.85f, cx, cy - size * 1.35f);
        shapes.setColor(top.r * 0.35f, top.g * 0.43f, top.b * 0.55f, top.a);
        shapes.triangle(cx + size, cy, cx, cy - size * 0.5f, cx, cy - size * 1.35f);
        shapes.triangle(cx + size, cy, cx + size, cy - size * 0.85f, cx, cy - size * 1.35f);
    }

    private void panel(float x, float y, float w, float h, Color fill, Color border) {
        shapes.setColor(border);
        shapes.rect(x - 2, y - 2, w + 4, h + 4);
        shapes.setColor(fill);
        shapes.rect(x, y, w, h);
    }

    private void buttonShape(float x, float y, float w, float h, boolean hover, boolean primary) {
        Color border = primary ? new Color(0x78f6d6dd) : new Color(0x638ba0cc);
        Color fill = primary ? new Color(hover ? 0x268f7fff : 0x176b64ff) : new Color(hover ? 0x25465aff : 0x172f40ff);
        panel(x, y, w, h, fill, border);
        if (hover) {
            shapes.setColor(1f, 1f, 1f, 0.07f);
            shapes.rect(x + 4, y + h - 9, w - 8, 5);
        }
    }

    private void drawCentered(BitmapFont font, String text, float centerX, float baselineY, Color color) {
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, text, centerX - layout.width / 2f, baselineY);
    }

    private boolean isHover(float x, float y, float w, float h) {
        float mouseX = Gdx.input.getX();
        float mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        float y = height - screenY;
        float panelWidth = Math.min(610, width - 64);
        float panelX = (width - panelWidth) / 2f;
        float panelY = height * 0.10f;
        float panelH = Math.min(410, height * 0.52f);
        float seedY = panelY + panelH - 104;
        float mainButtonY = seedY - 88;
        if (contains(screenX, y, panelX + panelWidth - 140, seedY + 13, 76, 32)) {
            seed = WorldGenerator.randomSeed();
            seedFocused = true;
            return true;
        }
        if (contains(screenX, y, panelX + 44, seedY, panelWidth - 88, 58)) {
            seedFocused = true;
            return true;
        }
        if (contains(screenX, y, panelX + 44, mainButtonY, panelWidth - 88, 60)) {
            launch();
            return true;
        }
        if (game.saves().exists() && contains(screenX, y, panelX + 44, mainButtonY - 76, panelWidth - 88, 56)) {
            game.continueWorld();
            return true;
        }
        seedFocused = false;
        return false;
    }

    private boolean contains(float px, float py, float x, float y, float w, float h) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) Gdx.app.exit();
        if (keycode == Input.Keys.ENTER) launch();
        if (keycode == Input.Keys.BACKSPACE && seedFocused && !seed.isEmpty()) seed = seed.substring(0, seed.length() - 1);
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        if (!seedFocused || Character.isISOControl(character) || seed.codePointCount(0, seed.length()) >= 28) return false;
        seed += character;
        return true;
    }

    private void launch() {
        if (seed.isBlank()) seed = WorldGenerator.randomSeed();
        game.launchNewWorld(seed.trim());
    }

    private void runSmokeHook(int width, int height) {
        if (!Boolean.getBoolean("blockhorizon.smokeTitle") || elapsed < 0.75f) return;
        try {
            Pixmap source = Pixmap.createFromFrameBuffer(0, 0, width, height);
            Pixmap flipped = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            for (int y = 0; y < height; y++) flipped.drawPixmap(source, 0, y, width, 1, 0, height - 1 - y, width, 1);
            source.dispose();
            String destination = System.getProperty("blockhorizon.smokeScreenshot", "build/smoke/title.png");
            PixmapIO.writePNG(Gdx.files.absolute(new java.io.File(destination).getAbsolutePath()), flipped);
            flipped.dispose();
            // Exercise the actual hit-testing order of the nested random-seed button.
            float pw = Math.min(610, width - 64);
            float px = (width - pw) / 2f;
            float sy = height * 0.10f + Math.min(410, height * 0.52f) - 104;
            String before = seed;
            touchDown((int)(px + pw - 102), (int)(height - sy - 29), 0, Input.Buttons.LEFT);
            if (before.equals(seed)) throw new IllegalStateException("随机按钮没有更新种子");
            SmokeEvidence.success(java.util.Map.of("mode", "title", "randomSeedButton", true));
        } finally {
            Gdx.app.exit();
        }
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
