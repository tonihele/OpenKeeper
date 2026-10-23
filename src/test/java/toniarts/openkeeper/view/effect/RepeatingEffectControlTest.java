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
package toniarts.openkeeper.view.effect;

import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepeatingEffectControlTest {

    private static final float MIN = 0.075f;
    private static final float MAX = 0.251f;
    private static final float FRAME = 1 / 60f;

    @Test
    void playsAgainAndAgainAtUnevenPauses() {
        List<Float> plays = new ArrayList<>();
        float[] time = {0};
        Node fire = new Node("Inferno");
        fire.addControl(new RepeatingEffectControl(MIN, MAX, () -> plays.add(time[0]), new Random(42)));

        // 7.5 seconds at 60 frames a second
        for (int frame = 0; frame < 450; frame++) {
            time[0] += FRAME;
            fire.updateLogicalState(FRAME);
        }

        assertTrue(plays.size() > 7.5 / MAX && plays.size() < 7.5 / MIN, "Played " + plays.size() + " times");
        float shortest = Float.MAX_VALUE;
        float longest = 0;
        for (int i = 1; i < plays.size(); i++) {
            float pause = plays.get(i) - plays.get(i - 1);
            shortest = Math.min(shortest, pause);
            longest = Math.max(longest, pause);
        }

        // Within the limits, give or take a frame, and not all the same
        assertTrue(shortest >= MIN - FRAME && longest <= MAX + FRAME, "Pauses " + shortest + " to " + longest);
        assertTrue(longest - shortest > 0.1f, "Pauses " + shortest + " to " + longest);
    }

    @Test
    void stopsWhenTheShotIsGone() {
        List<Float> plays = new ArrayList<>();
        Node fire = new Node("Inferno");
        RepeatingEffectControl control = new RepeatingEffectControl(MIN, MAX, () -> plays.add(0f), new Random(42));
        fire.addControl(control);
        fire.updateLogicalState(1f);
        int playedWhileBurning = plays.size();

        fire.removeControl(control);
        fire.updateLogicalState(5f);

        assertEquals(playedWhileBurning, plays.size());
    }

}
