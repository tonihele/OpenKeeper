/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert;

import com.jme3.system.AppSettings;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.AssetsConverter.ConvertProcess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests when the asset conversion runs its optional HD textures step
 */
class AssetsConverterTest {

    @Test
    void hdTexturesStepRunsOnlyWhenSwitchedOnOrToDeleteThem() {
        AppSettings settings = new AppSettings(false);
        for (ConvertProcess process : ConvertProcess.values()) {
            settings.putInteger(process.getSettingName(), process.getVersion());
        }
        settings.putInteger(ConvertProcess.HD_TEXTURES.getSettingName(), 0);

        // Off by default: a converted install needs nothing
        assertFalse(AssetsConverter.isConversionNeeded(settings));

        // Switched on, it runs once
        settings.putBoolean(AssetsConverter.HD_TEXTURES_SETTING, true);
        assertEquals(List.of(ConvertProcess.HD_TEXTURES), AssetsConverter.getConversionNeeded(settings));
        settings.putInteger(ConvertProcess.HD_TEXTURES.getSettingName(), ConvertProcess.HD_TEXTURES.getVersion());
        assertFalse(AssetsConverter.isConversionNeeded(settings));

        // And again when the converted textures change
        settings.putInteger(ConvertProcess.TEXTURES.getSettingName(), 0);
        assertTrue(AssetsConverter.isConversionNeeded(ConvertProcess.HD_TEXTURES, settings));
        settings.putInteger(ConvertProcess.TEXTURES.getSettingName(), ConvertProcess.TEXTURES.getVersion());

        // Switched off, it runs once more to delete them
        settings.putBoolean(AssetsConverter.HD_TEXTURES_SETTING, false);
        assertTrue(AssetsConverter.isConversionNeeded(ConvertProcess.HD_TEXTURES, settings));
        settings.putInteger(ConvertProcess.HD_TEXTURES.getSettingName(), 0);
        assertFalse(AssetsConverter.isConversionNeeded(settings));
    }
}
