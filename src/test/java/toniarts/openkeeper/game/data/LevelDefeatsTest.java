/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.data;

import com.jme3.system.AppSettings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelDefeatsTest {
    private final CampaignLevel level = new CampaignLevel(CampaignLevel.LevelType.Level, 6, "a");

    @Test
    void oldAttemptStatisticsDoNotBecomeDefeats() throws Exception {
        Settings settings = isolatedSettings();
        settings.increaseLevelAttempts(level);
        settings.increaseLevelAttempts(level);
        assertEquals(2, settings.getLevelAttempts(level));
        assertEquals(0, settings.getLevelDefeats(level));
        settings.increaseLevelDefeats(level);
        assertEquals(1, settings.getLevelDefeats(level));
        assertEquals(2, settings.getLevelAttempts(level));
    }

    @Test
    void isolatesTypesNumbersAndVariations() throws Exception {
        Settings settings = isolatedSettings();
        settings.increaseLevelDefeats(level);
        settings.increaseLevelDefeats(level);
        assertEquals(2, settings.getLevelDefeats(level));
        assertEquals(0, settings.getLevelDefeats(new CampaignLevel(CampaignLevel.LevelType.Level, 6, "b")));
        assertEquals(0, settings.getLevelDefeats(new CampaignLevel(CampaignLevel.LevelType.Level, 7, "a")));
        assertEquals(0, settings.getLevelDefeats(new CampaignLevel(CampaignLevel.LevelType.Secret, 6, "a")));
        assertEquals(0, settings.getLevelDefeats(new CampaignLevel(CampaignLevel.LevelType.MPD, 6, "a")));
    }

    @Test
    void defeatHistorySurvivesProfileSerialization() throws Exception {
        Settings settings = isolatedSettings();
        settings.increaseLevelDefeats(level);
        settings.increaseLevelDefeats(level);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        settings.getAppSettings().save(out);
        Settings restored = isolatedSettings();
        restored.getAppSettings().load(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(2, restored.getLevelDefeats(level));
    }

    @Test
    void campaignResetClearsDefeatsAcrossAllTypesAndVariations() throws Exception {
        Settings settings = isolatedSettings();
        CampaignLevel secret = new CampaignLevel(CampaignLevel.LevelType.Secret, 1);
        CampaignLevel mpd = new CampaignLevel(CampaignLevel.LevelType.MPD, 1);
        for (CampaignLevel item : new CampaignLevel[]{level, secret, mpd}) {
            settings.increaseLevelDefeats(item);
        }
        settings.getAppSettings().put("UnrelatedSetting", 42);
        settings.resetCampaignProgress();
        for (CampaignLevel item : new CampaignLevel[]{level, secret, mpd}) {
            assertEquals(0, settings.getLevelDefeats(item));
        }
        assertEquals(42, settings.getAppSettings().get("UnrelatedSetting"));
    }

    @Test
    void currentSimulationKeepsItsStartSnapshot() throws Exception {
        Settings settings = isolatedSettings();
        settings.increaseLevelDefeats(level);
        Class<?> type = Class.forName("toniarts.openkeeper.game.controller.GameController$LevelInfo");
        var constructor = type.getDeclaredConstructor(IKwdFile.class, int.class);
        constructor.setAccessible(true);
        ILevelInfo info = (ILevelInfo) constructor.newInstance(null, settings.getLevelDefeats(level));
        settings.increaseLevelDefeats(level);
        assertEquals(1, info.getLevelPlayed());
        ILevelInfo retry = (ILevelInfo) constructor.newInstance(null, settings.getLevelDefeats(level));
        assertEquals(2, retry.getLevelPlayed());
    }

    /** Uses an independent in-memory profile and never saves to the user's file. */
    private static Settings isolatedSettings() throws Exception {
        var constructor = Settings.class.getDeclaredConstructor(AppSettings.class);
        constructor.setAccessible(true);
        Settings settings = constructor.newInstance(new AppSettings(false));
        settings.getAppSettings().clear();
        return settings;
    }
}
