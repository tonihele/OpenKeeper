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
package toniarts.openkeeper.game.data;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsEvilRatingTest {

    private final Settings settings = Settings.getInstance();
    private final AppSettings userSettings = new AppSettings(false);

    @BeforeEach
    void clearSettings() {
        // Start from the defaults, not the user's settings file, and put the user's settings back after
        userSettings.copyFrom(settings.getAppSettings());
        settings.getAppSettings().clear();
    }

    @AfterEach
    void restoreSettings() {
        settings.getAppSettings().clear();
        settings.getAppSettings().copyFrom(userSettings);
    }

    @Test
    void overallEvilRatingIsTheSumOfTheLevels() {
        settings.setLevelEvilRating(new CampaignLevel(CampaignLevel.LevelType.Level, 1), 1000);
        settings.setLevelEvilRating(new CampaignLevel(CampaignLevel.LevelType.Level, 20), 234);

        assertEquals(1234, settings.getOverallEvilRating());
    }

    @Test
    void replayingALevelReplacesItsEvilRating() {
        settings.setLevelEvilRating(new CampaignLevel(CampaignLevel.LevelType.Level, 6, "a"), 1000);
        settings.setLevelEvilRating(new CampaignLevel(CampaignLevel.LevelType.Level, 6, "b"), 500);

        assertEquals(500, settings.getOverallEvilRating());
    }

    @Test
    void newCampaignClearsTheEvilRatings() {
        settings.setLevelEvilRating(new CampaignLevel(CampaignLevel.LevelType.Level, 3), 1000);
        settings.resetCampaignProgress();

        assertEquals(0, settings.getOverallEvilRating());
    }
}
