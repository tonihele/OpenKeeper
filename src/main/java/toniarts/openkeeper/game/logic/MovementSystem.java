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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import toniarts.openkeeper.game.component.ChickenAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.chicken.IChickenController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Advances AI-controlled mobile entities along their already-authoritative path.
 * Every displacement is bounded by speed and the current fixed game step, so
 * a creature cannot coast past a waypoint.
 */
public final class MovementSystem implements IGameLogicUpdatable {

    private static final float TWO_PI = (float) (Math.PI * 2d);
    private static final float PI = (float) Math.PI;
    private static final float POSITION_EPSILON = 0.00001f;

    private final EntityData entityData;
    private final EntitySet movableEntities;
    private final MovementSettings settings;
    private final float waypointToleranceSquared;
    private final GridMovementCollision collision;
    private final ICreaturesController creaturesController;
    private final IObjectsController objectsController;
    private final Map<EntityId, MovementCursor> movementCursors = new HashMap<>();

    public MovementSystem(EntityData entityData) {
        this(entityData, MovementSettings.DEFAULT);
    }

    public MovementSystem(EntityData entityData, MovementSettings settings) {
        this(entityData, settings, null, null);
    }

    public MovementSystem(EntityData entityData, MovementSettings settings, GridMovementCollision collision,
            ICreaturesController creaturesController) {
        this(entityData, settings, collision, creaturesController, null);
    }

    public MovementSystem(EntityData entityData, MovementSettings settings, GridMovementCollision collision,
            ICreaturesController creaturesController, IObjectsController objectsController) {
        this.entityData = Objects.requireNonNull(entityData, "entityData");
        this.settings = Objects.requireNonNull(settings, "settings");
        float waypointTolerance = settings.waypointTolerance();
        waypointToleranceSquared = waypointTolerance * waypointTolerance;
        this.collision = collision;
        this.creaturesController = creaturesController;
        this.objectsController = objectsController;
        movableEntities = entityData.getEntities(Position.class, Mobile.class, Navigation.class);
    }

    @Override
    public void processTick(float tpf) {
        if (!Float.isFinite(tpf) || tpf <= 0f) {
            return;
        }
        if (movableEntities.applyChanges()) {
            for (Entity removedEntity : movableEntities.getRemovedEntities()) {
                movementCursors.remove(removedEntity.getId());
            }
        }
        for (Entity entity : movableEntities) {
            moveEntity(entity, tpf);
        }
    }

    private void moveEntity(Entity entity, float tpf) {
        EntityId entityId = entity.getId();
        Navigation navigation = entity.get(Navigation.class);
        Position position = entity.get(Position.class);
        Mobile mobile = entity.get(Mobile.class);
        if (navigation == null || position == null || mobile == null) {
            return;
        }
        if (entityData.getComponent(entityId, Possessed.class) != null) {
            return;
        }

        List<Vector2> path = navigation.navigationPath;
        int pathSize = path == null ? 0 : path.size();
        MovementCursor cursor = pathSize > 2 ? movementCursor(entityId, navigation, path, pathSize) : null;
        Vector3f moved = position.position.clone();
        Vector2 waypoint = cursor == null ? findNextWaypoint(path, pathSize, moved) : findNextWaypoint(cursor, moved);
        float rotation = position.rotation;
        boolean movedOnGround = false;
        if (waypoint != null && mobile.maxSpeed > 0f) {
            float dx = waypoint.x - moved.x;
            float dz = waypoint.y - moved.z;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            float travel = Math.min(distance, mobile.maxSpeed * tpf);
            if (travel > 0f && distance > 0f) {
                float deltaX = dx / distance * travel;
                float deltaZ = dz / distance * travel;
                float pathHeading;
                if (cursor != null) {
                    if (!cursor.headingValid) {
                        cursor.heading = (float) Math.atan2(dx, dz);
                        cursor.headingValid = true;
                    }
                    pathHeading = cursor.heading;
                } else {
                    pathHeading = (float) Math.atan2(dx, dz);
                }
                float oldX = moved.x;
                float oldZ = moved.z;
                if (collision != null) {
                    INavigable navigable = navigableFor(entity);
                    if (navigable != null) {
                        collision.move(navigable, oldX, oldZ, deltaX, deltaZ, moved);
                    } else {
                        moved.x += deltaX;
                        moved.z += deltaZ;
                    }
                } else {
                    moved.x += deltaX;
                    moved.z += deltaZ;
                }
                movedOnGround = Math.abs(moved.x - oldX) > POSITION_EPSILON
                        || Math.abs(moved.z - oldZ) > POSITION_EPSILON;
                if (movedOnGround) {
                    boolean followedPath = Math.abs((moved.x - oldX) - deltaX) <= POSITION_EPSILON
                            && Math.abs((moved.z - oldZ) - deltaZ) <= POSITION_EPSILON;
                    if (followedPath) {
                        rotation = pathHeading;
                    } else {
                        rotation = (float) Math.atan2(moved.x - oldX, moved.z - oldZ);
                        if (cursor != null) {
                            cursor.headingValid = false;
                        }
                    }
                }
            }
        }

        boolean pathComplete = (cursor == null ? findNextWaypoint(path, pathSize, moved)
                : findNextWaypoint(cursor, moved)) == null;

        boolean facingComplete = true;
        if (pathComplete && navigation.faceTarget != null) {
            Point target = navigation.faceTarget;
            float targetX = target.x * WorldUtils.TILE_WIDTH;
            float targetZ = target.y * WorldUtils.TILE_WIDTH;
            float desiredRotation = (float) Math.atan2(targetX - moved.x, targetZ - moved.z);
            rotation = rotateToward(rotation, desiredRotation, settings.turnRate() * tpf);
            facingComplete = Math.abs(angleDifference(desiredRotation, rotation)) <= settings.facingTolerance();
            if (facingComplete) {
                rotation = desiredRotation;
            }
        }

        if (!moved.equals(position.position) || Math.abs(rotation - position.rotation) > POSITION_EPSILON) {
            entityData.setComponent(entityId, new Position(rotation, moved));
        }
        if (pathComplete && facingComplete) {
            entityData.removeComponent(entityId, Navigation.class);
        }
    }

    /**
     * Resolve the passability rules for the mobile entity being moved. The
     * movement query also includes wandering chickens, whose controller is
     * owned by the objects controller rather than the creatures controller.
     */
    private INavigable navigableFor(Entity entity) {
        EntityId entityId = entity.getId();
        if (entityData.getComponent(entityId, CreatureComponent.class) != null && creaturesController != null) {
            ICreatureController creatureController = creaturesController.createController(entityId);
            return creatureController;
        }
        if (entityData.getComponent(entityId, ChickenAi.class) != null && objectsController != null) {
            IChickenController chickenController = objectsController.createChickenController(entityId);
            return chickenController;
        }
        return null;
    }

    /**
     * Path element zero is the tile where pathfinding began. Movement starts at
     * the next element when there is a route; this also works when pathfinding
     * rounded a creature's in-tile position to the tile centre.
     */
    private MovementCursor movementCursor(EntityId entityId, Navigation navigation, List<Vector2> path, int pathSize) {
        MovementCursor cursor = movementCursors.get(entityId);
        if (cursor == null || cursor.navigation != navigation || cursor.path != path || cursor.pathSize != pathSize) {
            cursor = new MovementCursor(navigation, path, pathSize);
            movementCursors.put(entityId, cursor);
        }
        return cursor;
    }

    /**
     * Advances only through waypoints already reached. Keeping this progress
     * avoids rescanning the route on every tick and prevents a creature from
     * turning back toward a waypoint it has passed.
     */
    private Vector2 findNextWaypoint(MovementCursor cursor, Vector3f position) {
        List<Vector2> path = cursor.path;
        if (path == null || cursor.index >= cursor.pathSize) {
            return null;
        }
        while (cursor.index < cursor.pathSize) {
            Vector2 waypoint = path.get(cursor.index);
            if (cursor.waypoint != waypoint || cursor.waypointX != waypoint.x || cursor.waypointY != waypoint.y) {
                cursor.waypoint = waypoint;
                cursor.waypointX = waypoint.x;
                cursor.waypointY = waypoint.y;
                cursor.headingValid = false;
            }
            float dx = waypoint.x - position.x;
            float dz = waypoint.y - position.z;
            if (dx * dx + dz * dz > waypointToleranceSquared) {
                return waypoint;
            }
            cursor.index++;
        }
        return null;
    }

    /**
     * Short paths have no repeated prefix worth caching. Avoid a cursor-map
     * lookup for the common single-segment route.
     */
    private Vector2 findNextWaypoint(List<Vector2> path, int pathSize, Vector3f position) {
        if (pathSize == 0) {
            return null;
        }
        Vector2 waypoint = path.get(pathSize > 1 ? 1 : 0);
        float dx = waypoint.x - position.x;
        float dz = waypoint.y - position.z;
        return dx * dx + dz * dz > waypointToleranceSquared ? waypoint : null;
    }

    private static float rotateToward(float current, float target, float maxChange) {
        float difference = angleDifference(target, current);
        if (Math.abs(difference) <= maxChange) {
            return target;
        }
        return current + Math.copySign(maxChange, difference);
    }

    private static float angleDifference(float target, float current) {
        float difference = (target - current) % TWO_PI;
        if (difference > PI) {
            difference -= TWO_PI;
        } else if (difference < -PI) {
            difference += TWO_PI;
        }
        return difference;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        movableEntities.release();
        movementCursors.clear();
    }

    private static final class MovementCursor {

        private final Navigation navigation;
        private final List<Vector2> path;
        private final int pathSize;
        private int index;
        private Vector2 waypoint;
        private float waypointX;
        private float waypointY;
        private float heading;
        private boolean headingValid;

        private MovementCursor(Navigation navigation, List<Vector2> path, int pathSize) {
            this.navigation = navigation;
            this.path = path;
            this.pathSize = pathSize;
            index = pathSize > 1 ? 1 : 0;
        }
    }
}
