package com.blockhorizon.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.blockhorizon.BlockHorizonGame;
import com.blockhorizon.save.SaveData;

public final class LoadingScreen implements Screen {
    private final BlockHorizonGame game;
    private final String seed;
    private final SaveData save;
    private final Matrix4 projection = new Matrix4();
    private final GlyphLayout layout = new GlyphLayout();
    private int frames;
    private float elapsed;
    private String error;

    public LoadingScreen(BlockHorizonGame game, String seed, SaveData save) {
        this.game = game;
        this.seed = seed;
        this.save = save;
    }

    @Override public void show() { Gdx.input.setCursorCatched(false); }

    @Override
    public void render(float delta) {
        elapsed += delta;
        frames++;
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        projection.setToOrtho2D(0, 0, w, h);
        Gdx.gl.glClearColor(0.025f, 0.055f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        game.shapes().setProjectionMatrix(projection);
        game.shapes().begin(ShapeRenderer.ShapeType.Filled);
        game.shapes().rect(0, 0, w, h, new Color(0x07111fff), new Color(0x07111fff), new Color(0x183f53ff), new Color(0x214b5fff));
        float barW = Math.min(520, w - 100);
        float progress = error == null ? 0.15f + MathUtils.sin(elapsed * 2.4f) * 0.05f + Math.min(0.68f, elapsed * 0.18f) : 1f;
        game.shapes().setColor(new Color(0x142c3cff));
        game.shapes().rect((w - barW) / 2f, h * 0.38f, barW, 12);
        game.shapes().setColor(new Color(error == null ? 0x66e1c5ff : 0xe66d77ff));
        game.shapes().rect((w - barW) / 2f, h * 0.38f, barW * MathUtils.clamp(progress, 0, 1), 12);
        game.shapes().end();

        game.batch().setProjectionMatrix(projection);
        game.batch().begin();
        String title = error == null ? "正在生成你的世界" : "世界生成失败";
        game.fonts().heading.setColor(new Color(error == null ? 0xc9fff3ff : 0xffa6adff));
        layout.setText(game.fonts().heading, title);
        game.fonts().heading.draw(game.batch(), title, (w - layout.width) / 2f, h * 0.58f);
        String detail = error == null ? "种子：" + seed : error;
        game.fonts().small.setColor(new Color(0xa6c6d3ff));
        layout.setText(game.fonts().small, detail);
        game.fonts().small.draw(game.batch(), detail, (w - layout.width) / 2f, h * 0.50f);
        game.fonts().tiny.setColor(new Color(0x7698a7ff));
        String tip = error == null ? "塑造山川 · 唤醒森林 · 点亮星辰" : "按 ESC 返回标题界面";
        layout.setText(game.fonts().tiny, tip);
        game.fonts().tiny.draw(game.batch(), tip, (w - layout.width) / 2f, h * 0.31f);
        game.batch().end();

        if (frames == 3 && error == null) {
            try {
                game.switchTo(new GameScreen(game, seed, save));
            } catch (Exception exception) {
                Gdx.app.error("LoadingScreen", "无法创建世界", exception);
                error = exception.getClass().getSimpleName() + "：" + (exception.getMessage() == null ? "未知错误" : exception.getMessage());
                if (Boolean.getBoolean("blockhorizon.smoke")) Gdx.app.exit();
            }
        }
        if (error != null && Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) game.showTitle();
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
