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

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiConsumer;
import java.util.function.Function;
import toniarts.openkeeper.game.data.CampaignLevel.LevelType;
import toniarts.openkeeper.game.data.Settings.SecretLevelStatus;

/**
 * Which secret levels the campaign levels hide, and discovering them
 *
 * @author OpenKeeper
 */
public final class SecretLevels {

    /**
     * The Locate Hidden Land special on a campaign level reveals this secret
     * level. In DKII.EXE the level positions 5, 8, 11 and 16 (counting
     * Level 6a/6b and Level 11a/11b/11c as separate levels) unlock secret
     * levels 1 to 4, those are the levels 5, 7, 10 and 13.
     */
    private static final Map<Integer, Integer> LOCATED_SECRET_LEVELS = Map.of(5, 1, 7, 2, 10, 3, 13, 4);

    private SecretLevels() {
    }

    /**
     * Get the secret level that the Locate Hidden Land special of the given
     * level reveals
     *
     * @param level the campaign level the special was used on
     * @return the secret level number, empty if the level hides none
     */
    public static OptionalInt getLocatedSecretLevel(CampaignLevel level) {
        if (level.getType() != LevelType.Level || !level.getVariation().isEmpty()) {
            return OptionalInt.empty();
        }
        Integer secretLevel = LOCATED_SECRET_LEVELS.get(level.getLevel());
        return secretLevel != null ? OptionalInt.of(secretLevel) : OptionalInt.empty();
    }

    /**
     * Discover the secret level hidden on the given level. A secret level that
     * is already discovered, or in progress, is left alone.
     *
     * @param level the campaign level the special was used on
     * @param statusReader reads the status of a secret level
     * @param statusWriter stores the status of a secret level
     * @return the secret level that was newly discovered, empty if nothing changed
     */
    public static Optional<CampaignLevel> discover(CampaignLevel level, Function<CampaignLevel, SecretLevelStatus> statusReader,
            BiConsumer<CampaignLevel, SecretLevelStatus> statusWriter) {
        OptionalInt secretLevelNumber = getLocatedSecretLevel(level);
        if (secretLevelNumber.isEmpty()) {
            return Optional.empty();
        }

        CampaignLevel secretLevel = new CampaignLevel(LevelType.Secret, secretLevelNumber.getAsInt());
        if (statusReader.apply(secretLevel) != SecretLevelStatus.NOT_DISCOVED) {
            return Optional.empty();
        }
        statusWriter.accept(secretLevel, SecretLevelStatus.DISCOVERED);
        return Optional.of(secretLevel);
    }
}
