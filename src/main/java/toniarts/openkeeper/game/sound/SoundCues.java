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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.component.SoundCue;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The game logic's way to make a noise: it creates a {@link SoundCue} entity and the clients play it.
 * Nothing in here touches the audio, the sound engine lives on the clients only.
 */
public final class SoundCues {

    private SoundCues() {
        // Static helper
    }

    /**
     * A positional sound, heard by everyone
     */
    public static void emit(EntityData entityData, @Nullable String category, int eventId, Vector3f position) {
        emit(entityData, category, eventId, WorldUtils.vectorToPoint(position));
    }

    /**
     * A positional sound, heard by everyone
     */
    public static void emit(EntityData entityData, @Nullable String category, int eventId, Point position) {
        create(entityData, category, eventId, position, SoundCue.ALL_PLAYERS, null, false);
    }

    /**
     * A sound that is not restarted while the previous one of the source is still playing
     */
    public static void emitExclusive(EntityData entityData, @Nullable String category, int eventId, Point position,
            EntityId source) {
        create(entityData, category, eventId, position, SoundCue.ALL_PLAYERS, source, true);
    }

    /**
     * A sound that comes from the hand of the player, only that player hears it
     */
    public static void emitToPlayer(EntityData entityData, String category, int eventId, short playerId) {
        create(entityData, category, eventId, null, playerId, null, false);
    }

    private static void create(EntityData entityData, @Nullable String category, int eventId, @Nullable Point position,
            short playerId, @Nullable EntityId source, boolean exclusive) {
        if (category == null || category.isEmpty()) {
            return;
        }
        entityData.setComponents(entityData.createEntity(),
                new SoundCue(category, eventId, position, playerId, source, exclusive));
    }
}
