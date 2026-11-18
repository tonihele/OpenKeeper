/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.data;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

class TextureFilteringTest {
    @Test
    void legacyAndExplicitPreferencesSurviveTheActualSettingsFileFormat() throws Exception {
        var settings = new AppSettings(false);
        settings.putInteger(Settings.Setting.ANISOTROPY.getKey(), 8);
        var loaded = roundTrip(settings);
        assertSame(
                TextureFiltering.ANISOTROPIC_8,
                TextureFiltering.resolve(
                        loaded.getString(Settings.Setting.TEXTURE_FILTERING.getKey()),
                        loaded.getInteger(Settings.Setting.ANISOTROPY.getKey())));

        settings.putString(
                Settings.Setting.TEXTURE_FILTERING.getKey(), TextureFiltering.BILINEAR.name());
        loaded = roundTrip(settings);
        assertSame(
                TextureFiltering.BILINEAR,
                TextureFiltering.resolve(
                        loaded.getString(Settings.Setting.TEXTURE_FILTERING.getKey()),
                        loaded.getInteger(Settings.Setting.ANISOTROPY.getKey())));
        assertEquals(8, loaded.getInteger(Settings.Setting.ANISOTROPY.getKey()));
    }

    private static AppSettings roundTrip(AppSettings settings) throws Exception {
        var output = new ByteArrayOutputStream();
        settings.save(output);
        var loaded = new AppSettings(false);
        loaded.load(new ByteArrayInputStream(output.toByteArray()));
        return loaded;
    }

    @Test
    void explicitNamesWinAndUnknownVersionsMigrateLegacyPreferences() {
        for (var mode : TextureFiltering.values()) {
            assertSame(mode, TextureFiltering.resolve(mode.name(), 16));
            assertTrue(mode.getMinFilter().usesMipMapLevels());
        }
        assertSame(TextureFiltering.TRILINEAR, TextureFiltering.resolve(null, -1));
        assertSame(TextureFiltering.TRILINEAR, TextureFiltering.resolve("", 0));
        assertSame(TextureFiltering.TRILINEAR, TextureFiltering.resolve(" ", 1));
        assertSame(TextureFiltering.ANISOTROPIC_2, TextureFiltering.resolve("unknown", 2));
        assertSame(TextureFiltering.ANISOTROPIC_4, TextureFiltering.resolve("unknown", 3));
        assertSame(TextureFiltering.ANISOTROPIC_4, TextureFiltering.resolve("", 4));
        assertSame(TextureFiltering.ANISOTROPIC_8, TextureFiltering.resolve("", 8));
        assertSame(TextureFiltering.ANISOTROPIC_16, TextureFiltering.resolve("", 16));
        assertSame(TextureFiltering.ANISOTROPIC_16, TextureFiltering.resolve("", 32));
        assertSame(
                Texture.MinFilter.BilinearNearestMipMap, TextureFiltering.BILINEAR.getMinFilter());
    }
}
