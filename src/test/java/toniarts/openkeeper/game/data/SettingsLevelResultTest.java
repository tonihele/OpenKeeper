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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsLevelResultTest {

    private final Settings settings = Settings.getInstance();
    private final CampaignLevel level8 = new CampaignLevel(CampaignLevel.LevelType.Level, 8);

    // The reset doesn't clear the attempts (it uses another key), so each test has its own level
    private final CampaignLevel level9 = new CampaignLevel(CampaignLevel.LevelType.Level, 9);

    @BeforeEach
    void resetCampaign() {
        settings.resetCampaignProgress();
    }

    @Test
    void wonLevelIsCompletedAndRated() {
        settings.recordLevelResult(level8, true, 1234);

        assertEquals(Settings.LevelStatus.COMPLETED, settings.getLevelStatus(level8));
        assertEquals(9, settings.getSetting(Settings.Setting.LEVEL_NUMBER));
        assertEquals(1234, settings.getOverallEvilRating());
        assertEquals(0, settings.getLevelAttempts(level8));
    }

    @Test
    void lostLevelIsAnAttemptAndStaysOpen() {
        settings.recordLevelResult(level9, false, 50);
        settings.recordLevelResult(level9, false, 60);

        assertEquals(Settings.LevelStatus.NOT_COMPLETED, settings.getLevelStatus(level9));
        assertEquals(2, settings.getLevelAttempts(level9));
        assertEquals(60, settings.getOverallEvilRating());
    }
}
