package com.blockhorizon;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.blockhorizon.audio.ProceduralAudio;
import com.blockhorizon.save.SaveData;
import com.blockhorizon.save.SaveSystem;
import com.blockhorizon.screen.LoadingScreen;
import com.blockhorizon.screen.TitleScreen;
import com.blockhorizon.ui.FontBook;

public final class BlockHorizonGame extends Game {
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private FontBook fonts;
    private ProceduralAudio audio;
    private SaveSystem saves;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        fonts = new FontBook();
        audio = new ProceduralAudio();
        saves = new SaveSystem();
        if (Boolean.getBoolean("blockhorizon.smoke") && !Boolean.getBoolean("blockhorizon.smokeTitle")) launchNewWorld("自动测试-2026");
        else showTitle();
    }

    public void showTitle() {
        switchTo(new TitleScreen(this));
    }

    public void launchNewWorld(String seed) {
        switchTo(new LoadingScreen(this, seed, null));
    }

    public void continueWorld() {
        SaveData data = saves.load();
        if (data == null) showTitle();
        else switchTo(new LoadingScreen(this, data.seed, data));
    }

    public void switchTo(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) previous.dispose();
    }

    public SpriteBatch batch() { return batch; }
    public ShapeRenderer shapes() { return shapes; }
    public FontBook fonts() { return fonts; }
    public ProceduralAudio audio() { return audio; }
    public SaveSystem saves() { return saves; }

    @Override
    public void dispose() {
        Screen active = getScreen();
        super.dispose();
        if (active != null) active.dispose();
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (fonts != null) fonts.dispose();
        if (audio != null) audio.close();
    }
}
