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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.controller.IMapController;
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
    private final Map<EntityId, Vector2> standingVelocities = new HashMap<>();
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

        processAddedEntities(movableEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (movableEntities.applyChanges()) {
            processDeletedEntities(movableEntities.getRemovedEntities());

            processAddedEntities(movableEntities.getAddedEntities());

            processChangedEntities(movableEntities.getChangedEntities());
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
        standingVelocities.entrySet().removeIf(entry -> !pushStanding(entry.getKey(), entry.getValue(), tpf));
    }

    /**
     * Moves a creature that is pushed where it stands. DK II's pilot runs for
     * a fighting creature too (0x488930, 0x4D4760). Its path is finished, so
     * the path following only brakes it (0x4D3C40, weight 0.35) and the push
     * of a tick changes its velocity.
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
        if (push == null && velocity.isZero(0.01f)) {
            return false;
        }

        // A change of the velocity of a tick: the push less the braking
        float scale = ticksPerSecond * ticksPerSecond;
        Vector2 acceleration = new Vector2(velocity).scl(-FollowWaypoints.WEIGHT * ticksPerSecond);
        if (push != null) {
            acceleration.add(push.push.x * scale, push.push.y * scale);
        }
        velocity.mulAdd(acceleration, tpf).limit(mobile.maxSpeed);
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
                creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER), ticksPerSecond);
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
        steeringBehaviors.clear();
        steerableEntitiesByEntityId.clear();
        steerableEntitiesBySteeringBehavior.clear();
        steeringBehaviorsBySteerableEntity.clear();
        steeringOutputsBySteeringBehaviors.clear();
        entityIdsBySteeringBehavior.clear();
        targetPointsByEntityId.clear();
    }

}
