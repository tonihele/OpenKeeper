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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.GameEvent;
import toniarts.openkeeper.game.component.GameEvent.ThingFlag;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The game logic's way to tell that something happened: it creates a {@link GameEvent} entity and the clients
 * react to it. Nothing in here touches the audio or the view.
 */
public final class GameEvents {

    private GameEvents() {
        // Static helper
    }

    /**
     * An event about a thing type, seen by everyone
     */
    public static void emit(EntityData entityData, GameEventType type, ThingFlag subjectKind, int subjectId,
            @Nullable Point position) {
        create(entityData, type, subjectKind, subjectId, null, 0, null, position, GameEvent.ALL_PLAYERS);
    }

    /**
     * An event about a thing type, seen by everyone
     */
    public static void emit(EntityData entityData, GameEventType type, ThingFlag subjectKind, int subjectId,
            Vector3f position) {
        emit(entityData, type, subjectKind, subjectId, WorldUtils.vectorToPoint(position));
    }

    /**
     * An event with no subject, seen by everyone
     */
    public static void emit(EntityData entityData, GameEventType type, Point position) {
        create(entityData, type, null, 0, null, 0, null, position, GameEvent.ALL_PLAYERS);
    }

    /**
     * An event with no subject, seen by everyone
     */
    public static void emit(EntityData entityData, GameEventType type, Vector3f position) {
        emit(entityData, type, WorldUtils.vectorToPoint(position));
    }

    /**
     * An event about an entity (a creature or an object), seen by everyone. Nothing happens if the entity is
     * neither.
     */
    public static void emit(EntityData entityData, GameEventType type, EntityId entity, Vector3f position) {
        ThingFlag kind = getKind(entityData, entity);
        if (kind != null) {
            create(entityData, type, kind, getTypeId(entityData, entity, kind), null, 0, entity,
                    WorldUtils.vectorToPoint(position), GameEvent.ALL_PLAYERS);
        }
    }

    /**
     * An event about an entity at its current position, seen by everyone. Nothing happens if the entity has no
     * position or is neither a creature nor an object.
     */
    public static void emit(EntityData entityData, GameEventType type, EntityId entity) {
        Position position = entityData.getComponent(entity, Position.class);
        if (position != null) {
            emit(entityData, type, entity, position.position);
        }
    }

    /**
     * An event about a thing type that is aimed at an entity (e.g. a hit), seen by everyone
     */
    public static void emitAt(EntityData entityData, GameEventType type, ThingFlag subjectKind, int subjectId,
            EntityId target, Vector3f position) {
        ThingFlag targetKind = getKind(entityData, target);
        create(entityData, type, subjectKind, subjectId, targetKind,
                targetKind != null ? getTypeId(entityData, target, targetKind) : 0, target,
                WorldUtils.vectorToPoint(position), GameEvent.ALL_PLAYERS);
    }

    /**
     * An event only one player gets, with no place
     */
    public static void emitToPlayer(EntityData entityData, GameEventType type, short playerId) {
        create(entityData, type, null, 0, null, 0, null, null, playerId);
    }

    private static void create(EntityData entityData, GameEventType type, @Nullable ThingFlag subjectKind,
            int subjectId, @Nullable ThingFlag targetKind, int targetId, @Nullable EntityId entity,
            @Nullable Point position, short playerId) {
        entityData.setComponents(entityData.createEntity(),
                new GameEvent(type, subjectKind, subjectId, targetKind, targetId, entity, position, playerId));
    }

    /**
     * @return the thing kind of the entity, {@code null} if it is not a creature, door, trap nor an object
     */
    @Nullable
    private static ThingFlag getKind(EntityData entityData, @Nullable EntityId entity) {
        if (entity == null) {
            return null;
        }
        if (entityData.getComponent(entity, CreatureComponent.class) != null) {
            return ThingFlag.CREATURE;
        }
        if (entityData.getComponent(entity, ObjectComponent.class) != null) {
            return ThingFlag.OBJECT;
        }
        if (entityData.getComponent(entity, DoorComponent.class) != null) {
            return ThingFlag.DOOR;
        }
        if (entityData.getComponent(entity, TrapComponent.class) != null) {
            return ThingFlag.TRAP;
        }
        return null;
    }

    private static int getTypeId(EntityData entityData, EntityId entity, ThingFlag kind) {
        return switch (kind) {
            case CREATURE ->
                entityData.getComponent(entity, CreatureComponent.class).creatureId;
            case OBJECT ->
                entityData.getComponent(entity, ObjectComponent.class).objectId;
            case DOOR ->
                entityData.getComponent(entity, DoorComponent.class).doorId;
            case TRAP ->
                entityData.getComponent(entity, TrapComponent.class).trapId;
            default ->
                throw new IllegalArgumentException("Not an entity kind: " + kind);
        };
    }
}
