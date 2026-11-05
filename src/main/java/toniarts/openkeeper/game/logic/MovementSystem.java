/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.math.Vector2;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.utils.Point;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringBehavior;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringFactory;
import toniarts.openkeeper.game.navigation.steering.FollowWaypoints;
import toniarts.openkeeper.game.navigation.steering.ISteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles moving of the entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MovementSystem implements IGameLogicUpdatable {
    
    private final static boolean INDEPENDENT_FACING = false;

    /**
     * A creature pushed away from a taken end point stops this close to it.
     * DK II plans the walk again when it makes no headway (0x4D3C40).
     */
    private static final float END_TOLERANCE = 0x6148 / 65536f;

    /**
     * The radius of what has no creature width
     */
    private static final float DEFAULT_RADIUS = 0.25f;

    /**
     * The Dungeon Heart Obstacle, a solid object 3 tiles across at the centre
     * of an intact heart (0x4DB700, 0x4D12B0)
     */
    private static final float HEART_OBSTACLE_RADIUS = 1.5f;

    /**
     * A creature moving away from a solid obstacle at least this fast in a
     * tick isn't held by it (0x4D4760)
     */
    private static final float OBSTACLE_CLEARING = 0xCCC / 65536f;

    /**
     * DK II's fight state, where the pilot still runs while the creature
     * stands (0x488930)
     */
    private static final EnumSet<CreatureState> FIGHTING = EnumSet.of(CreatureState.FIGHT, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL);

    /**
     * How far round a waypoint DK II counts the crowd, 0.95 tiles (0x4D15E0)
     */
    private static final float CROWD_SIZE = 0xF333 / 65536f;
    
    private final SafeArrayList<EntitySteeringBehavior> steeringBehaviors = new SafeArrayList<>(EntitySteeringBehavior.class);
    private final Map<EntityId, ISteerableEntity> steerableEntitiesByEntityId = new HashMap<>();
    private final Map<ISteerableEntity, EntitySteeringBehavior> steeringBehaviorsBySteerableEntity = new HashMap<>();
    private final Map<EntitySteeringBehavior, ISteerableEntity> steerableEntitiesBySteeringBehavior = new HashMap<>();
    private final Map<EntitySteeringBehavior, EntityId> entityIdsBySteeringBehavior = new HashMap<>();

    // For tracking the changes in navigation itself, for now it might be enough to just see that if the target has been changed
    private final Map<EntityId, Point> targetPointsByEntityId = new HashMap<>();

    /**
     * This is for saving in object creation
     */
    private final Map<EntitySteeringBehavior, SteeringAcceleration<Vector2>> steeringOutputsBySteeringBehaviors = new HashMap<>();
    private final EntitySet movableEntities;
    private final EntitySet pushedEntities;
    private final EntitySet creatures;

    /**
     * The sum of the radii of the creatures on each tile, worked out every
     * step for the crowds round the waypoints
     */
    private final Map<Point, Float> radiiByTile = new HashMap<>();
    private final Map<EntityId, Vector2> standingVelocities = new HashMap<>();

    /**
     * The walkers touching the Dungeon Heart Obstacle this tick, with the
     * direction from its centre to them
     */
    private final Map<EntityId, Vector2> heartContacts = new HashMap<>();
    private float tickTime;
    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final int ticksPerSecond;

    public MovementSystem(EntityData entityData, IKwdFile kwdFile, IMapController mapController) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        movableEntities = entityData.getEntities(Position.class, Mobile.class, Navigation.class);
        pushedEntities = entityData.getEntities(Position.class, Mobile.class, SeparationPush.class);
        creatures = entityData.getEntities(CreatureComponent.class, Position.class);

        processAddedEntities(movableEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (movableEntities.applyChanges()) {
            processDeletedEntities(movableEntities.getRemovedEntities());

            processAddedEntities(movableEntities.getAddedEntities());

            processChangedEntities(movableEntities.getChangedEntities());
        }

        countRadii();
        tickTime += tpf;
        if (tickTime >= 1f / ticksPerSecond) {
            tickTime -= 1f / ticksPerSecond;
            findHeartContacts();
        }

        // Process ticks
        for (EntitySteeringBehavior steeringBehavior : steeringBehaviors.getArray()) {
            EntityId entityId = entityIdsBySteeringBehavior.get(steeringBehavior);
            ISteerableEntity steerableEntity = steerableEntitiesBySteeringBehavior.get(steeringBehavior);
            steerableEntity.setMaxLinearSpeed(getMaxSpeed(movableEntities.getEntity(entityId).get(Mobile.class), steerableEntity.getPosition()));

            // Calculate steering acceleration
            SteeringAcceleration<Vector2> steeringOutput = steeringOutputsBySteeringBehaviors.get(steeringBehavior);
            steeringBehavior.calculateSteering(steeringOutput);

            /*
             * Here you might want to add a motor control layer filtering steering accelerations.
             *
             * For instance, a car in a driving game has physical constraints on its movement: it cannot turn while stationary; the
             * faster it moves, the slower it can turn (without going into a skid); it can brake much more quickly than it can
             * accelerate; and it only moves in the direction it is facing (ignoring power slides).
             */
            if (!steeringOutput.linear.isZero()) {
                separate(entityId, steerableEntity, steeringOutput.linear);
                holdOffHeart(entityId, steerableEntity, steeringOutput.linear);
            }

            // Apply steering acceleration
            applySteering(entityId, steerableEntity, steeringOutput, tpf);
        }

        pushedEntities.applyChanges();
        for (Entity entity : pushedEntities) {
            if (!steerableEntitiesByEntityId.containsKey(entity.getId())) {
                standingVelocities.computeIfAbsent(entity.getId(), id -> new Vector2());
            }
        }
        for (Entity entity : creatures) {
            if (!steerableEntitiesByEntityId.containsKey(entity.getId()) && !standingVelocities.containsKey(entity.getId())
                    && getHeartContact(entity.getId(), entity.get(Position.class)) != null) {
                standingVelocities.put(entity.getId(), new Vector2());
            }
        }
        standingVelocities.entrySet().removeIf(entry -> !pushStanding(entry.getKey(), entry.getValue(), tpf));
    }

    private void countRadii() {
        creatures.applyChanges();
        radiiByTile.clear();
        for (Entity entity : creatures) {
            Creature creature = kwdFile.getCreature(entity.get(CreatureComponent.class).creatureId);
            if (creature != null) {
                radiiByTile.merge(WorldUtils.vectorToPoint(entity.get(Position.class).position), creature.getAttributes().getWidth() / 2, Float::sum);
            }
        }
    }

    /**
     * The sum of the radii of the creatures on the tiles round a waypoint. DK
     * II counts the tiles a square 0.95 tiles across at the waypoint touches,
     * rounding its far edges up (0x4D15E0).
     *
     * @param waypoint the waypoint
     * @return the sum of the radii
     */
    private double getCrowd(Vector2 waypoint) {
        float crowd = 0;
        for (int x = (int) Math.floor(waypoint.x + 0.5f - CROWD_SIZE / 2); x <= (int) Math.ceil(waypoint.x + 0.5f + CROWD_SIZE / 2); x++) {
            for (int y = (int) Math.floor(waypoint.y + 0.5f - CROWD_SIZE / 2); y <= (int) Math.ceil(waypoint.y + 0.5f + CROWD_SIZE / 2); y++) {
                crowd += radiiByTile.getOrDefault(new Point(x, y), 0f);
            }
        }
        return crowd;
    }

    /**
     * Moves a creature that is pushed where it stands. DK II's pilot runs for
     * a fighting creature too (0x488930, 0x4D4760). Its path is finished, so
     * the path following only brakes it (0x4D3C40, weight 0.35) and the push
     * of a tick changes its velocity. Against the Dungeon Heart Obstacle it
     * slides, see {@link #slideAlongHeart}.
     *
     * @param entityId the creature
     * @param velocity its velocity, changed here
     * @param tpf the time of the step
     * @return false once it walks, or stands still with no push
     */
    private boolean pushStanding(EntityId entityId, Vector2 velocity, float tpf) {
        Position position = entityData.getComponent(entityId, Position.class);
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (position == null || mobile == null || steerableEntitiesByEntityId.containsKey(entityId)) {
            return false;
        }
        SeparationPush push = entityData.getComponent(entityId, SeparationPush.class);
        Vector2 heartNormal = getHeartContact(entityId, position);
        if (push == null && heartNormal == null && velocity.isZero(0.01f)) {
            return false;
        }

        // The change of the velocity of a tick: the push less the braking, worked out in velocities of a tick
        Vector2 tickVelocity = new Vector2(velocity).scl(1f / ticksPerSecond);
        Vector2 force = new Vector2(tickVelocity).scl(-FollowWaypoints.WEIGHT);
        if (push != null) {
            force.add(push.push.x, push.push.y);
        }
        if (heartNormal != null) {
            slideAlongHeart(heartNormal, force, tickVelocity);
        }
        force.limit(FollowWaypoints.MAX_FORCE);
        velocity.set(tickVelocity.mulAdd(force, tpf * ticksPerSecond)).scl(ticksPerSecond).limit(mobile.maxSpeed);
        Vector2 location = new Vector2(position.position.x, position.position.z);
        move(location, velocity, tpf, true);
        position.position.x = location.x;
        position.position.z = location.y;
        entityData.setComponent(entityId, new Position(position.rotation, position.position));
        return true;
    }

    /**
     * Get the speed for the tile the entity is on. Walking through water halves
     * the speed and lava speeds it up by half, flying is not affected
     *
     * @param mobile the mobility of the entity
     * @param position the position of the entity
     * @return the maximum speed
     */
    private float getMaxSpeed(Mobile mobile, Vector2 position) {
        if (!mobile.canFly) {
            Point p = WorldUtils.vectorToPoint(position);
            if (mapController.isWater(p)) {
                return mobile.maxSpeed * 0.5f;
            }
            if (mapController.isLava(p)) {
                return mobile.maxSpeed * 1.5f;
            }
        }
        return mobile.maxSpeed;
    }

    /**
     * A fighting creature touching the Dungeon Heart Obstacle. DK II's
     * obstacle query finds it within the obstacle's and the creature's radii
     * (0x4D12B0, 0x4D3020).
     *
     * @param entityId the creature
     * @param position where it is
     * @return the direction from the centre of the heart to the creature, or
     * null if it doesn't touch it or doesn't fight
     */
    private Vector2 getHeartContact(EntityId entityId, Position position) {
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        Creature creature = getCreature(entityId);
        if (creatureAi == null || !FIGHTING.contains(creatureAi.getCreatureState()) || creature == null) {
            return null;
        }
        return getHeartContact(new Vector2(position.position.x, position.position.z), creature.getAttributes().getWidth() / 2);
    }

    /**
     * Something touching the Dungeon Heart Obstacle
     *
     * @param position where it is
     * @param radius its radius
     * @return the direction from the centre of the heart to it, or null if it
     * doesn't touch it
     */
    private Vector2 getHeartContact(Vector2 position, float radius) {
        Point center = getIntactHeartCenter(position.x, position.y);
        if (center == null) {
            return null;
        }
        Vector2 normal = new Vector2(position).sub(center.x, center.y);
        float distance = normal.len();
        if (distance == 0 || distance > HEART_OBSTACLE_RADIUS + radius) {
            return null;
        }
        return normal.scl(1 / distance);
    }

    /**
     * The centre of the intact Dungeon Heart a point is on, where its obstacle
     * stands
     *
     * @param x the x of the point
     * @param y the y of the point
     * @return the centre, null if there is no intact heart
     */
    private Point getIntactHeartCenter(float x, float y) {
        IRoomController room = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(x, y));
        if (room == null || !room.isDungeonHeart() || room.getRoomInstance().isDestroyed()) {
            return null;
        }
        return room.getRoomInstance().getCenter();
    }

    private boolean isInHeartObstacle(Vector2 point) {
        Point center = getIntactHeartCenter(point.x, point.y);
        return center != null && point.dst2(center.x, center.y) <= HEART_OBSTACLE_RADIUS * HEART_OBSTACLE_RADIUS;
    }

    /**
     * Once a game tick DK II's pilot finds the solid obstacles a walking
     * creature touches, within their and its radii (0x4D31D0, 0x4D3020),
     * after it notes its headway (0x4D4760). A creature held up too long walks
     * through them.
     */
    private void findHeartContacts() {
        heartContacts.clear();
        for (EntitySteeringBehavior steeringBehavior : steeringBehaviors.getArray()) {
            FollowWaypoints pathFollowing = steeringBehavior.getPathFollowing();
            if (pathFollowing == null) {
                continue;
            }
            pathFollowing.countHeadway();
            ISteerableEntity steerable = steerableEntitiesBySteeringBehavior.get(steeringBehavior);
            Vector2 normal = pathFollowing.isThroughObstacles() ? null : getHeartContact(steerable.getPosition(), steerable.getBoundingRadius());
            if (normal != null) {
                heartContacts.put(entityIdsBySteeringBehavior.get(steeringBehavior), normal);
            }
        }
    }

    /**
     * Keeps a walking creature that touches the Dungeon Heart Obstacle off it,
     * it slides along the edge, see {@link #slideAlongHeart}
     *
     * @param entityId the creature
     * @param steerable its steering state, its velocity is changed here
     * @param linear the steering acceleration, changed here
     */
    private void holdOffHeart(EntityId entityId, ISteerableEntity steerable, Vector2 linear) {
        Vector2 normal = heartContacts.get(entityId);
        if (normal == null) {
            return;
        }

        // In the changes of the velocity of a tick and the velocities of a tick, as the pilot works
        float scale = ticksPerSecond * ticksPerSecond;
        Vector2 force = new Vector2(linear).scl(1 / scale);
        Vector2 tickVelocity = new Vector2(steerable.getLinearVelocity()).scl(1f / ticksPerSecond);
        slideAlongHeart(normal, force, tickVelocity);
        linear.set(force.limit(FollowWaypoints.MAX_FORCE)).scl(scale);
        steerable.getLinearVelocity().set(tickVelocity).scl(ticksPerSecond);
    }

    /**
     * DK II's pilot keeps a creature against a solid obstacle from going
     * further in: unless it already moves away from it, the force and the
     * velocity lose their part towards the obstacle, and a full force along
     * the edge is added, the way it already goes (0x4D4760). So a creature
     * pushed against the heart runs along its edge.
     *
     * @param normal the direction away from the obstacle
     * @param force the change of the velocity of a tick, changed here
     * @param tickVelocity the velocity of a tick, changed here
     */
    private static void slideAlongHeart(Vector2 normal, Vector2 force, Vector2 tickVelocity) {
        if (force.dot(normal) + tickVelocity.dot(normal) >= OBSTACLE_CLEARING) {
            return;
        }
        force.mulAdd(normal, -force.dot(normal));
        tickVelocity.mulAdd(normal, -tickVelocity.dot(normal));
        Vector2 edge = new Vector2(-normal.y, normal.x);
        if (force.dot(edge) + tickVelocity.dot(edge) < 0) {
            edge.scl(-1);
        }
        force.mulAdd(edge, FollowWaypoints.MAX_FORCE);
    }

    private void processAddedEntities(Set<Entity> addedEntities) {
        for (Entity entity : addedEntities) {
            addEntity(entity);
        }
    }

    private void addEntity(Entity entity) {
        Mobile mobile = entity.get(Mobile.class);
        Navigation navigation = entity.get(Navigation.class);
        Position position = entity.get(Position.class);

        // Steering orientation is the rotation negated, as applySteering writes it back
        Creature creature = getCreature(entity.getId());
        float radius = creature != null && creature.getAttributes().getWidth() > 0 ? creature.getAttributes().getWidth() / 2 : DEFAULT_RADIUS;
        ISteerableEntity steerableEntity = new SteerableEntity(entity.getId(), mobile.maxSpeed, radius, position.position.x, position.position.z, -position.rotation);
        EntitySteeringBehavior steeringBehavior = EntitySteeringFactory.navigateToPoint(navigation.navigationPath, navigation.faceTarget, steerableEntity, navigation.target,
                this::getCrowd, this::isInHeartObstacle, creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER), navigation.pursuingAttackTarget, ticksPerSecond);
        if (steeringBehavior == null) {

            // The fug, can't navigate, are we there already??
            entityData.removeComponent(entity.getId(), Navigation.class);
            return;
        }
        steerableEntitiesByEntityId.put(entity.getId(), steerableEntity);
        steeringBehaviorsBySteerableEntity.put(steerableEntity, steeringBehavior);
        steerableEntitiesBySteeringBehavior.put(steeringBehavior, steerableEntity);
        int index = Collections.binarySearch(steeringBehaviors, steeringBehavior);
        steeringBehaviors.add(~index, steeringBehavior);
        steeringOutputsBySteeringBehaviors.put(steeringBehavior, new SteeringAcceleration<>(new Vector2()));
        entityIdsBySteeringBehavior.put(steeringBehavior, entity.getId());
        targetPointsByEntityId.put(entity.getId(), navigation.target);
    }

    private Creature getCreature(EntityId entityId) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent != null ? kwdFile.getCreature(creatureComponent.creatureId) : null;
    }

    private void processDeletedEntities(Set<Entity> removedEntities) {
        for (Entity entity : removedEntities) {
            deleteEntity(entity);
        }
    }

    private void deleteEntity(Entity entity) {
        ISteerableEntity steerableEntity = steerableEntitiesByEntityId.remove(entity.getId());
        EntitySteeringBehavior steeringBehavior = steeringBehaviorsBySteerableEntity.remove(steerableEntity);
        steerableEntitiesBySteeringBehavior.remove(steeringBehavior);
        if (steeringBehavior != null) {
            int index = Collections.binarySearch(steeringBehaviors, steeringBehavior);
            steeringBehaviors.remove(index);
        }
        steeringOutputsBySteeringBehaviors.remove(steeringBehavior);
        entityIdsBySteeringBehavior.remove(steeringBehavior);
        targetPointsByEntityId.remove(entity.getId());
    }

    private void processChangedEntities(Set<Entity> changedEntities) {
        for (Entity entity : changedEntities) {

            // Dirty trick to try to see if the navigation has changed
            Navigation navigation = entity.get(Navigation.class);
            if (!navigation.target.equals(targetPointsByEntityId.get(entity.getId()))) {
                deleteEntity(entity);
                addEntity(entity);
                continue;
            }

            // We are only prepared for the changes in Mobile
            // Not the position, the position is managed by us only
            Mobile mobile = entity.get(Mobile.class);

            ISteerableEntity steerableEntity = steerableEntitiesByEntityId.get(entity.getId());
            steerableEntity.setMaxLinearSpeed(mobile.maxSpeed);
        }
    }

    private void applySteering(EntityId entityId, ISteerableEntity steerableEntity, SteeringAcceleration<Vector2> steering, float tpf) {

        // We are done
        if (steering.isZero()
                && steerableEntity.getLinearVelocity().isZero(steerableEntity.getZeroLinearSpeedThreshold())
                && isZeroAngular(steerableEntity.getAngularVelocity(), steerableEntity.getZeroLinearSpeedThreshold())) {
            entityData.removeComponent(entityId, Navigation.class);
            return;
        }

        // trim speed if have no forces
        if (steering.linear.isZero() && !steerableEntity.getLinearVelocity().isZero()) {
            steerableEntity.getLinearVelocity().scl(0.8f);
        }
        // apply linear
        steerableEntity.getLinearVelocity().mulAdd(steering.linear, tpf).limit(steerableEntity.getMaxLinearSpeed());
        move(steerableEntity, tpf);

        // Update orientation and angular velocity
        if (INDEPENDENT_FACING
                || steerableEntity.getLinearVelocity().isZero(steerableEntity.getZeroLinearSpeedThreshold())) {
            // trim speed if have no forces
            if (steering.angular == 0 && steerableEntity.getAngularVelocity() != 0) {
                steerableEntity.setAngularVelocity(steerableEntity.getAngularVelocity() * 0.7f);
            }
            // Update angular velocity. Velocity is trimmed to maximum speed
            steerableEntity.setAngularVelocity(steerableEntity.getAngularVelocity()
                    + limitAngular(steering.angular, steerableEntity.getMaxAngularAcceleration()) * tpf);
            steerableEntity.setAngularVelocity(limitAngular(steerableEntity.getAngularVelocity(), steerableEntity.getMaxAngularSpeed()));
            // apply
            if (!isZeroAngular(steerableEntity.getAngularVelocity(), steerableEntity.getZeroLinearSpeedThreshold())) {
                steerableEntity.setOrientation(steerableEntity.getOrientation() + steerableEntity.getAngularVelocity() * tpf);
            }
        } else {
            float newOrientation = SteeringUtils.calculateVectorToAngle(steerableEntity.getLinearVelocity());
            steerableEntity.setAngularVelocity(newOrientation - steerableEntity.getOrientation());
            steerableEntity.setOrientation(newOrientation);
        }

        // Also update the real components
        Position oldPosition = entityData.getComponent(entityId, Position.class);
        if (oldPosition != null) {

            // Re-use the vector, the position might be already removed from another thread
            oldPosition.position.x = steerableEntity.getPosition().x;
            oldPosition.position.z = steerableEntity.getPosition().y;
            entityData.setComponent(entityId, new Position(-steerableEntity.getOrientation(), oldPosition.position));
        }
    }

    /**
     * Moves the entity by its velocity. Like DK II's pilot (0x4D4760) it
     * doesn't step into a wall: it moves along the one axis that is free and
     * stops on the other, or else steps sideways, or else stays.
     *
     * @param steerable the entity
     * @param tpf the time of the step
     */
    private void move(ISteerableEntity steerable, float tpf) {
        move(steerable.getPosition(), steerable.getLinearVelocity(), tpf, false);
    }

    private void move(Vector2 position, Vector2 velocity, float tpf, boolean avoidHeart) {
        float dx = velocity.x * tpf;
        float dy = velocity.y * tpf;
        if (isFree(avoidHeart, position.x + dx, position.y + dy) || !isFree(avoidHeart, position.x, position.y)) {
            position.add(dx, dy);
        } else if (isFree(avoidHeart, position.x + dx, position.y)) {
            position.x += dx;
            velocity.y = 0;
        } else if (isFree(avoidHeart, position.x, position.y + dy)) {
            position.y += dy;
            velocity.x = 0;
        } else if (isFree(avoidHeart, position.x - dy, position.y + dx)) {
            position.add(-dy, dx);
        } else if (isFree(avoidHeart, position.x + dy, position.y - dx)) {
            position.add(dy, -dx);
        }
    }

    private boolean isFree(boolean avoidHeart, float x, float y) {
        Point tile = WorldUtils.vectorToPoint(x, y);
        if (mapController.isSolid(tile)) {
            return false;
        }
        if (!avoidHeart) {
            return true;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(tile);
        if (room == null || !room.isDungeonHeart() || room.getRoomInstance().isDestroyed()) {
            return true;
        }
        Point center = room.getRoomInstance().getCenter();
        return Vector2.dst2(x, y, center.x, center.y) >= HEART_OBSTACLE_RADIUS * HEART_OBSTACLE_RADIUS;
    }

    /**
     * Adds the separation push the logic tick worked out (see
     * {@link SeparationSystem}) to the path following. Near the end of its path
     * a pushed creature stops where it is, since the end point is taken.
     *
     * @param entityId the creature
     * @param steerable its steering state
     * @param linear the path following acceleration, the push is added to it
     */
    private void separate(EntityId entityId, ISteerableEntity steerable, Vector2 linear) {
        SeparationPush separationPush = entityData.getComponent(entityId, SeparationPush.class);
        if (separationPush == null) {
            return;
        }

        Navigation navigation = entityData.getComponent(entityId, Navigation.class);
        if (navigation != null && !navigation.navigationPath.isEmpty()
                && steerable.getPosition().dst(navigation.navigationPath.get(navigation.navigationPath.size() - 1)) <= END_TOLERANCE) {
            linear.setZero();
            steerable.getLinearVelocity().setZero();
            return;
        }

        // Like the path following, the push changes the velocity of a tick and the sum is capped (0x4D4760)
        float scale = ticksPerSecond * ticksPerSecond;
        linear.add(separationPush.push.x * scale, separationPush.push.y * scale).limit(FollowWaypoints.MAX_FORCE * scale);
    }

    /**
     * Limit angular speed or acceleration
     *
     * @param angular
     * @param limit
     * @return
     */
    public float limitAngular(float angular, float limit) {
        return (Math.abs(angular) > limit) ? Math.copySign(limit, angular) : angular;
    }

    public boolean isZeroAngular(float angular, float threshold) {
        return Math.abs(angular) < threshold;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        movableEntities.release();
        pushedEntities.release();
        creatures.release();
        radiiByTile.clear();
        heartContacts.clear();
        steeringBehaviors.clear();
        steerableEntitiesByEntityId.clear();
        steerableEntitiesBySteeringBehavior.clear();
        steeringBehaviorsBySteerableEntity.clear();
        steeringOutputsBySteeringBehaviors.clear();
        entityIdsBySteeringBehavior.clear();
        targetPointsByEntityId.clear();
    }

}
