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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * The sounds rooms make on their own: the steam and air blasts of the workshop and the beating of the dungeon
 * heart. They are heard from the centre of the room.
 */
public final class RoomSoundState extends AbstractPauseAwareState {

    /**
     * The workshop makes a noise every 8 to 24 seconds
     */
    private static final double WORKSHOP_MIN_INTERVAL = 8.0;
    private static final double WORKSHOP_INTERVAL_RANGE = 16.0;

    /**
     * The original beats with the animation of the heart, which is about this fast
     */
    private static final double HEART_BEAT_INTERVAL = 2.0;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final Random random = new Random();
    private final Map<EntityId, RoomTimers> timers = new HashMap<>();

    private EntitySet roomEntities;
    private double timeElapsed;

    public RoomSoundState(EntityData entityData, IKwdFile kwdFile) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        roomEntities = entityData.getEntities(RoomComponent.class);
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled() || !SoundManager.isInitialized()) {
            return;
        }
        timeElapsed += tpf;

        roomEntities.applyChanges();
        timers.keySet().retainAll(roomEntities.getEntityIds());
        for (Entity entity : roomEntities) {
            RoomComponent roomComponent = entity.get(RoomComponent.class);
            Room room = kwdFile.getRoomById(roomComponent.roomId);
            String category = room != null ? room.getSoundCategory() : null;
            if (roomComponent.destroyed || roomComponent.location == null || category == null) {
                continue;
            }

            if (ThingSounds.CATEGORY_WORKSHOP.equalsIgnoreCase(category)) {
                tickWorkshop(entity.getId(), roomComponent, category);
            } else if (ThingSounds.CATEGORY_DUNGEON_HEART.equalsIgnoreCase(category)) {
                tickHeart(entity.getId(), roomComponent, category);
            }
        }
    }

    private void tickWorkshop(EntityId roomId, RoomComponent room, String category) {
        RoomTimers roomTimers = timers.computeIfAbsent(roomId, id -> new RoomTimers(nextWorkshopTime()));
        if (timeElapsed < roomTimers.nextSound) {
            return;
        }
        roomTimers.nextSound = nextWorkshopTime();

        // One clip at a time, a new one is dropped while the previous one is still playing
        if (roomTimers.handle != SoundEngine.NO_HANDLE && SoundManager.isPlaying(roomTimers.handle)) {
            return;
        }
        roomTimers.handle = ThingSounds.play(category,
                random.nextBoolean() ? ThingSounds.ROOM_WORKSHOP_STEAM : ThingSounds.ROOM_WORKSHOP_AIR, room.location);
    }

    private void tickHeart(EntityId roomId, RoomComponent room, String category) {
        RoomTimers roomTimers = timers.computeIfAbsent(roomId, id -> new RoomTimers(timeElapsed));
        if (timeElapsed < roomTimers.nextSound) {
            return;
        }
        roomTimers.nextSound = timeElapsed + HEART_BEAT_INTERVAL;
        ThingSounds.play(category, ThingSounds.ROOM_HEART_BEAT, room.location);
    }

    @Override
    public void cleanup() {
        if (roomEntities != null) {
            roomEntities.release();
            roomEntities = null;
        }
        timers.clear();
        super.cleanup();
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
