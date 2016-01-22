/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game;

/**
 *
 * @author ArchDemon
 */

// TODO create timer or use existed
public final class GameTimer {

    private float time = 0;
    private boolean active = false;

    public GameTimer() {
    }

    public GameTimer(boolean active) {
        setActive(active);
    }

    public void setActive(boolean value) {
        active = value;
        if (active) {
            // start
        } else {
            // stop
        }
    }

    public boolean getActive() {
        return active;
    }

    public float getTime() {
        // FIXME
        if (active) {
            time += 0.01f;
        }
        return time;
    }
}
