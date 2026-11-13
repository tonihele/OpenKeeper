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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds {@link Shot} fixtures for the keeper spell tests. Lives in
 * {@code Shot}'s own package because its setters are {@code protected}
 * (parser-only).
 */
public final class SpellShotFixtures {

    private SpellShotFixtures() {
    }

    /**
     * A shot with the given process type
     *
     * @param shotId the shot ID
     * @param name the shot name
     * @param processType what the shot does
     * @param health the shot lifetime, in game ticks
     * @return the shot
     */
    public static Shot shot(short shotId, String name, Shot.ProcessType processType, int health) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName(name);
        shot.setProcessType(processType);
        shot.setHealth(health);
        return shot;
    }

}
