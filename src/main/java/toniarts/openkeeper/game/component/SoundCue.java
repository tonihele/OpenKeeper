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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;

/**
 * Something that should be heard. The game logic does not make any noise, it only tells that a sound
 * happened and each client plays it on its own. The cue lives for a short while (see
 * {@code SoundCueSystem}), long enough to reach the clients.
 */
public final class SoundCue implements EntityComponent {

    /**
     * Heard by everyone
     */
    public static final short ALL_PLAYERS = -1;

    /**
     * The sound category, e.g. the one of a creature type
     */
    public String category;
    public int eventId;

    /**
     * The tile the sound comes from, {@code null} if the sound is not positional
     */
    public Point position;

    /**
     * The only player that hears the sound, {@link #ALL_PLAYERS} if everyone does
     */
    public short playerId = ALL_PLAYERS;

    /**
     * The entity that makes the sound, only used with {@link #exclusive}
     */
    public EntityId source;

    /**
     * Do not start the sound if the previous exclusive sound of the same source and event still plays
     */
    public boolean exclusive;

    public SoundCue() {
        // For serialization
    }

    public SoundCue(String category, int eventId, Point position, short playerId, EntityId source, boolean exclusive) {
        this.category = category;
        this.eventId = eventId;
        this.position = position;
        this.playerId = playerId;
        this.source = source;
        this.exclusive = exclusive;
    }

}
