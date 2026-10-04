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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.Point;

/**
 * The sounds rooms make on their own: the steam and air blasts of the workshop and the beating of the dungeon
 * heart.
 */
public final class RoomSoundSystem extends GameTimeCounter {

    /**
     * The workshop makes a noise every 8 to 24 seconds
     */
    private static final double WORKSHOP_MIN_INTERVAL = 8.0;
    private static final double WORKSHOP_INTERVAL_RANGE = 16.0;

    /**
     * The original beats with the animation of the heart, which is about this fast
     */
    private static final double HEART_BEAT_INTERVAL = 2.0;

    private final IMapController mapController;
    private final Random random = new Random();
    private final Map<EntityId, RoomTimers> timers = new HashMap<>();

    public RoomSoundSystem(IMapController mapController) {
        this.mapController = mapController;
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        List<EntityId> existing = new ArrayList<>();
        for (IRoomController room : new ArrayList<>(mapController.getRoomControllers())) {
            List<Point> coordinates = room.getRoomInstance().getCoordinates();
            String category = room.getRoom().getSoundCategory();
            if (coordinates.isEmpty() || category == null) {
                continue;
            }

            if (ThingSounds.CATEGORY_WORKSHOP.equalsIgnoreCase(category)) {
                existing.add(room.getEntityId());
                tickWorkshop(room, category, coordinates);
            } else if (ThingSounds.CATEGORY_DUNGEON_HEART.equalsIgnoreCase(category)) {
                existing.add(room.getEntityId());
                tickHeart(room, category, coordinates);
            }
        }
        timers.keySet().retainAll(existing);
    }

    private void tickWorkshop(IRoomController room, String category, List<Point> coordinates) {
        RoomTimers roomTimers = timers.computeIfAbsent(room.getEntityId(), id -> new RoomTimers(nextWorkshopTime()));
        if (timeElapsed < roomTimers.nextSound) {
            return;
        }
        roomTimers.nextSound = nextWorkshopTime();

        // One clip at a time, a new one is dropped while the previous one is still playing
        if (roomTimers.handle != SoundEngine.NO_HANDLE && SoundManager.isPlaying(roomTimers.handle)) {
            return;
        }
        Point tile = coordinates.get(random.nextInt(coordinates.size()));
        roomTimers.handle = ThingSounds.play(category,
                random.nextBoolean() ? ThingSounds.ROOM_WORKSHOP_STEAM : ThingSounds.ROOM_WORKSHOP_AIR, tile);
    }

    private void tickHeart(IRoomController room, String category, List<Point> coordinates) {
        RoomTimers roomTimers = timers.computeIfAbsent(room.getEntityId(), id -> new RoomTimers(timeElapsed));
        if (timeElapsed < roomTimers.nextSound) {
            return;
        }
        roomTimers.nextSound = timeElapsed + HEART_BEAT_INTERVAL;

        // The heart is 5x5 tiles, beat from its middle
        int x = 0;
        int y = 0;
        for (Point point : coordinates) {
            x += point.x;
            y += point.y;
        }
        ThingSounds.play(category, ThingSounds.ROOM_HEART_BEAT, new Point(x / coordinates.size(), y / coordinates.size()));
    }

    @Override
    public void start() {
        // Nothing to start
    }

    @Override
    public void stop() {
        timers.clear();
    }

    private double nextWorkshopTime() {
        return timeElapsed + WORKSHOP_MIN_INTERVAL + random.nextDouble() * WORKSHOP_INTERVAL_RANGE;
    }

    private static final class RoomTimers {

        private double nextSound;
        private int handle = SoundEngine.NO_HANDLE;

        RoomTimers(double nextSound) {
            this.nextSound = nextSound;
        }
    }
}
