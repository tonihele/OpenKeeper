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

import java.util.Arrays;

/**
 * The slow frame throttle: while the game is running below its normal frame rate,
 * 29 frequent events (footsteps, digging, chickens, combat grunts and mood
 * chatter) are dropped with a rotating pattern, so that the game stays responsive instead of drowning in sound.
 * <p>
 * At normal frame rates the throttle is inert. Speech playback disables it
 * around its own call.
 */
public class SoundThrottle {

    /**
     * Throttled events, all below {@code 0x400}.
     */
    private static final int[] EVENTS = {0xE5, 0x314, 0x332, 0x315, 0x333, 0x331, 0x337, 0x338, 0x147, 0x33F,
        0xDC, 0x150, 0xD6, 0xDB, 0x114, 0x1BB, 0x1BC, 0xD7, 0xD8, 0x334, 0x109, 0xF1, 0xF4, 0x110, 0xCB,
        0xCC, 0xCA, 0xD0, 0x30F};

    /**
     * Number of ticks an event is dropped after it was let through. The entries are used in
     * turns, so that the gaps between played sounds vary.
     */
    private static final int[] ROT = {4, 1, 5, 2, 6, 3, 1, 3, 5, 4, 2, 4, 5, 2, 6, 2, 1};

    /**
     * Frame times below this count as normal, up to {@link #HEAVY_FRAME_MS} as
     * slow and beyond it as very slow.
     */
    private static final long SLOW_FRAME_MS = 300;
    private static final long HEAVY_FRAME_MS = 500;

    private final int[] counter = new int[0x400];

    private long lastTickMs;
    private boolean referenceEstablished;
    private int loadLevel;
    private int rotation;
    private boolean enabled = true;

    public SoundThrottle() {
        clear();
    }

    public static boolean isThrottled(int eventId) {
        return eventId > 0 && eventId < 0x400 && Arrays.binarySearch(EVENTS, eventId) >= 0;
    }

    /**
     * The throttle can be switched off, e.g. around speech playback.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Once per game tick: derives the load level from the tick duration and
     * ages the counters of the throttled events.
     */
    public void tick(long nowMs) {
        if (!referenceEstablished) {
            // the first tick only establishes the reference point
            referenceEstablished = true;
            lastTickMs = nowMs;
            loadLevel = 0;
            return;
        }

        long elapsed = nowMs - lastTickMs;
        lastTickMs = nowMs;

        loadLevel = elapsed < SLOW_FRAME_MS ? 0 : (elapsed < HEAVY_FRAME_MS ? 1 : 2);

        for (int event : EVENTS) {
            if (counter[event] > 1) {
                counter[event]--;
            }
        }
    }

    /**
     * @param eventId the requested event
     * @return {@code true} if the request may be played
     */
    public boolean allow(int eventId) {
        if (!enabled || loadLevel == 0 || eventId < 0 || eventId >= counter.length
                || counter[eventId] == 0) {
            return true;
        }
        if (loadLevel == 2 || counter[eventId] > 1) {
            // a very slow frame drops everything, a slow one only what is over
            // its allowance
            return false;
        }
        rotation = (rotation + 1) % ROT.length;
        counter[eventId] = 1 + ROT[rotation];
        return true;
    }

    public void clear() {
        Arrays.fill(counter, 0);
        for (int event : EVENTS) {
            counter[event] = 1;
        }
        lastTickMs = 0;
        referenceEstablished = false;
        loadLevel = 0;
        rotation = 0;
    }
}