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
package toniarts.openkeeper.game.event;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;

/**
 * Something that happened in the game. The game logic only tells it and each client reacts on its own (plays a
 * sound...)
 */
public final class GameEvent {

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
     * The type of a thing (an id in the map data) that an event is about
     */
    public enum ThingFlag {
        CREATURE,
        OBJECT,
        DOOR,
        TRAP,
        ROOM,
        TERRAIN,
        SHOT,
        KEEPER_SPELL
    }

    public GameEvent(GameEventType type, ThingFlag subjectKind, int subjectId, ThingFlag targetKind, int targetId,
            EntityId entity, Point position) {
        this.type = type;
        this.subjectKind = subjectKind;
        this.subjectId = subjectId;
        this.targetKind = targetKind;
        this.targetId = targetId;
        this.entity = entity;
        this.position = position;
    }

}
