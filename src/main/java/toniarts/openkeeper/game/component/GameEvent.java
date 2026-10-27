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
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.event.ThingFlag;
import toniarts.openkeeper.utils.Point;

/**
 * Something that happened in the game. The game logic only tells it and each client reacts on its own (plays a
 * sound...). The event lives for a short while (see {@code GameEventSystem}), long enough to reach the clients.
 */
public final class GameEvent implements EntityComponent {

    /**
     * Seen by everyone
     */
    public static final short ALL_PLAYERS = -1;

    public GameEventType type;

    /**
     * The thing type the event is about, {@code null} if there is none
     */
    public ThingFlag subjectKind;
    public int subjectId;

    /**
     * The thing type the event is aimed at (e.g. the victim of a hit), {@code null} if there is none
     */
    public ThingFlag targetKind;
    public int targetId;

    /**
     * The entity that the event is about, to tell the events of the same entity apart
     */
    public EntityId entity;

    /**
     * The tile where it happened, {@code null} if the event has no place
     */
    public Point position;

    /**
     * The only player that gets the event, {@link #ALL_PLAYERS} if everyone does
     */
    public short playerId = ALL_PLAYERS;

    public GameEvent() {
        // For serialization
    }

    public GameEvent(GameEventType type, ThingFlag subjectKind, int subjectId, ThingFlag targetKind, int targetId,
            EntityId entity, Point position, short playerId) {
        this.type = type;
        this.subjectKind = subjectKind;
        this.subjectId = subjectId;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.entity = entity;
        this.position = position;
        this.playerId = playerId;
    }

}
