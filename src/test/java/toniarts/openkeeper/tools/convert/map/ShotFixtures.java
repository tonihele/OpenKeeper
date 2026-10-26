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

import java.util.EnumSet;

public final class ShotFixtures {

    private ShotFixtures() {
    }

    public static Shot shot(short shotId, Shot.ProcessType processType, int health, EnumSet<Shot.ShotProcessFlag> processFlags) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName("Shot " + shotId);
        shot.setProcessType(processType);
        shot.setHealth(health);
        shot.setShotProcessFlags(processFlags);
        return shot;
    }

}
