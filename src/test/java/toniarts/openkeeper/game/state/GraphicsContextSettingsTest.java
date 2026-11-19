/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.state;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.system.AppSettings;

import org.junit.jupiter.api.Test;

import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.data.TextureFiltering;

class GraphicsContextSettingsTest {
    @Test
    void pendingPauseMenuPreferencesStillRequireMainMenuContextRestart() {
        var running = new AppSettings(true);
        var desired = new AppSettings(false);
        desired.copyFrom(running); // jME context settings are a separate copy.
        desired.setGammaCorrection(!running.isGammaCorrection());
        var saved = new AppSettings(false);
        saved.copyFrom(desired); // Reopening the menu displays already-saved preferences.
        assertTrue(MainMenuScreenController.requiresContextRestart(running, saved));
        running.copyFrom(saved);
        assertFalse(MainMenuScreenController.requiresContextRestart(running, saved));
    }

    @Test
    void filteringOnlyChangesAreLiveAndAllRendererOptionsRequireRestart() {
        var running = new AppSettings(true);
        var desired = new AppSettings(false);
        desired.copyFrom(running);
        for (var filtering : TextureFiltering.values()) {
            desired.putString(Settings.Setting.TEXTURE_FILTERING.getKey(), filtering.name());
            assertFalse(MainMenuScreenController.requiresContextRestart(running, desired));
        }
        for (var change :
                java.util.List.<java.util.function.Consumer<AppSettings>>of(
                        s -> s.setWidth(running.getWidth() + 1),
                        s -> s.setHeight(running.getHeight() + 1),
                        s -> s.setDepthBits(running.getDepthBits() + 1),
                        s -> s.setFrequency(running.getFrequency() + 1),
                        s -> s.setFullscreen(!running.isFullscreen()),
                        s -> s.setVSync(!running.isVSync()),
                        s -> s.setRenderer(AppSettings.LWJGL_OPENGL2),
                        s -> s.setSamples(running.getSamples() + 1),
                        s -> s.setGammaCorrection(!running.isGammaCorrection()))) {
            desired.copyFrom(running);
            change.accept(desired);
            assertTrue(MainMenuScreenController.requiresContextRestart(running, desired));
        }
    }
}
