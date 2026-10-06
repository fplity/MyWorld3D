package com.blockhorizon.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.blockhorizon.BlockHorizonGame;
import com.blockhorizon.GameConfig;

public final class DesktopLauncher {
    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle(GameConfig.TITLE);
        config.setWindowedMode(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        config.setWindowSizeLimits(960, 600, -1, -1);
        config.setHdpiMode(HdpiMode.Logical);
        config.setBackBufferConfig(8, 8, 8, 8, 24, 8, 4);
        config.useVsync(true);
        config.setForegroundFPS(144);
        config.setIdleFPS(30);
        new Lwjgl3Application(new BlockHorizonGame(), config);
        if (Boolean.getBoolean("blockhorizon.smoke") && !SmokeEvidence.succeeded()) System.exit(1);
    }
}
