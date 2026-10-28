package toniarts.openkeeper.game.event;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;

import javax.annotation.Nullable;

public interface GameEventService {

    /**
     * Seen by everyone
     */
    short ALL_PLAYERS = -1;

    /**
     * An event about a thing type, seen by everyone
     */
    void emit(GameEventType type, GameEvent.ThingFlag subjectKind, int subjectId,
              @Nullable Point position);

    /**
     * An event about a thing type, seen by everyone
     */
    void emit(GameEventType type, GameEvent.ThingFlag subjectKind, int subjectId,
              Vector3f position);

    /**
     * An event with no subject, seen by everyone
     */
    void emit(GameEventType type, Point position);

    /**
     * An event with no subject, seen by everyone
     */
    void emit(GameEventType type, Vector3f position);

    /**
     * An event about an entity (a creature or an object), seen by everyone. Nothing happens if the entity is
     * neither.
     */
    void emit(GameEventType type, EntityId entity, Vector3f position);

    /**
     * An event about an entity at its current position, seen by everyone. Nothing happens if the entity has no
     * position or is neither a creature nor an object.
     */
    void emit(GameEventType type, EntityId entity);

    /**
     * An event about a thing type that is aimed at an entity (e.g. a hit), seen by everyone
     */
    void emitAt(GameEventType type, GameEvent.ThingFlag subjectKind, int subjectId,
                EntityId target, Vector3f position);

    /**
     * An event only one player gets, with no place
     */
    void emitToPlayer(GameEventType type, short playerId);
}
