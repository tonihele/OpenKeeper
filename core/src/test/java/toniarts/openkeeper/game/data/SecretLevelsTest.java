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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.data.CampaignLevel.LevelType;
import toniarts.openkeeper.game.data.Settings.SecretLevelStatus;

/**
 * The Locate Hidden Land special discovers a secret level for the campaign
 * level it is found on, like in DKII.EXE (0x004A4F70, which unlocks secret
 * level 0 to 3 from the campaign level position 5, 8, 11 and 16, those are
 * levels 5, 7, 10 and 13 once Level 6a/6b and Level 11a/11b/11c are counted
 * as one level each).
 */
class SecretLevelsTest {

    private final Map<Integer, SecretLevelStatus> statuses = new HashMap<>();

    private SecretLevelStatus status(CampaignLevel secretLevel) {
        return statuses.getOrDefault(secretLevel.getLevel(), SecretLevelStatus.NOT_DISCOVED);
    }

    private Optional<CampaignLevel> discover(CampaignLevel level) {
        return SecretLevels.discover(level, this::status, (secret, status) -> statuses.put(secret.getLevel(), status));
    }

    @Test
    void campaignLevelsFiveSevenTenAndThirteenLocateTheSecretLevels() {
        assertEquals(1, SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 5)).getAsInt());
        assertEquals(2, SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 7)).getAsInt());
        assertEquals(3, SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 10)).getAsInt());
        assertEquals(4, SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 13)).getAsInt());
    }

    @Test
    void otherLevelsLocateNothing() {
        assertTrue(SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 4)).isEmpty());
        assertTrue(SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Level, 6, "a")).isEmpty());
        assertTrue(SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.MPD, 5)).isEmpty());
        assertTrue(SecretLevels.getLocatedSecretLevel(new CampaignLevel(LevelType.Secret, 5)).isEmpty());
    }

    @Test
    void discoversTheSecretLevel() {
        Optional<CampaignLevel> discovered = discover(new CampaignLevel(LevelType.Level, 5));

        assertTrue(discovered.isPresent());
        assertEquals(LevelType.Secret, discovered.get().getType());
        assertEquals(1, discovered.get().getLevel());
        assertEquals(SecretLevelStatus.DISCOVERED, statuses.get(1));
    }

    @Test
    void doesNotTouchAnAlreadyKnownSecretLevel() {
        statuses.put(2, SecretLevelStatus.IN_PROGRESS);

        assertTrue(discover(new CampaignLevel(LevelType.Level, 7)).isEmpty());
        assertEquals(SecretLevelStatus.IN_PROGRESS, statuses.get(2));
    }

    @Test
    void doesNothingOnALevelWithoutAHiddenLand() {
        assertTrue(discover(new CampaignLevel(LevelType.Level, 3)).isEmpty());
        assertFalse(statuses.containsKey(1));
    }
}
