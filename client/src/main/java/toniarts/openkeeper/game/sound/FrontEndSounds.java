/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import java.util.ArrayList;
import java.util.List;

/**
 * The background soundscape of the main menu: two independent streams of random atmosphere noises (drips, distant
 * doors, groans) and the choir and the birds of the hero gate that is in the scene.
 */
public final class FrontEndSounds {

    private final List<Integer> handles = new ArrayList<>();

    /**
     * Starts the soundscape, does nothing if it is already playing.
     */
    public void start() {
        if (!handles.isEmpty()) {
            return;
        }

        // The original starts the atmosphere twice, the global sound layer and the front end itself
        add(SoundManager.playSound(GlobalCategory.ONE_SHOT_ATMOS, ThingSounds.AMBIENT_BED));
        add(SoundManager.playSound(GlobalCategory.ONE_SHOT_ATMOS, ThingSounds.AMBIENT_BED));

        // The gate is right in front of the camera, so the beds are heard on the listener
        add(SoundManager.playSound(GlobalCategory.ROOM_HERO_GATE, ThingSounds.AMBIENT_BED));
        add(SoundManager.playSound(GlobalCategory.ROOM_HERO_GATE, ThingSounds.AMBIENT_BED_2));
    }

    public void stop() {
        for (int handle : handles) {
            SoundManager.stopSound(handle);
        }
        handles.clear();
    }

    private void add(int handle) {
        if (handle != SoundEngine.NO_HANDLE) {
            handles.add(handle);
        }
    }
}
