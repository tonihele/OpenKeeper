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
import com.jme3.math.Vector2f;
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
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.component.StandingVelocity;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.steering.FurnitureCollision;
import toniarts.openkeeper.game.navigation.steering.FurnitureContactSolver;
import toniarts.openkeeper.game.navigation.steering.CreatureSizing;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringBehavior;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringFactory;
import toniarts.openkeeper.game.navigation.steering.FollowWaypoints;
import toniarts.openkeeper.game.navigation.steering.ISteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles moving of the entities. Like DK II's pilot (0x4D4760) it works the
 * steering out once a game tick.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MovementSystem implements IGameLogicUpdatable {
    
    private final static boolean INDEPENDENT_FACING = false;
    
    /**
     * A creature pushed away from a taken end point stops this close to it.
     * DK II plans the walk again when it makes no headway (0x4D3C40).
     */
    public static final float END_TOLERANCE = 0x6148 / 65536f;

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
    private final EntitySet creatures;

    /**
     * The sum of the radii of the creatures on each tile, worked out every
     * tick for the crowds round the waypoints
     */
    private final Map<Point, Float> radiiByTile = new HashMap<>();
    private final EntityData entityData;
    private final IMapInformation mapInformation;
    private final IKwdFile level;
    private final FurnitureCollision furnitureCollision;
    private final IEntityPositionLookup positionLookup;
    private final ICreaturesController creaturesController;
    private final CreatureMovementCollision movementCollision = new CreatureMovementCollision();

    public MovementSystem(EntityData entityData, IMapInformation mapInformation) {
        this(entityData, mapInformation, null);
    }

    public MovementSystem(EntityData entityData, IMapInformation mapInformation, IKwdFile level) {
        this(entityData, mapInformation, level, null, null);
    }

    public MovementSystem(EntityData entityData, IMapInformation mapInformation, IKwdFile level,
            IEntityPositionLookup positionLookup, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.mapInformation = mapInformation;
        this.positionLookup = positionLookup;
        this.creaturesController = creaturesController;
        this.level = level;
        furnitureCollision = level == null ? null : new FurnitureCollision(entityData, level);
        movableEntities = entityData.getEntities(Position.class, Mobile.class, Navigation.class);
        creatures = entityData.getEntities(CreatureComponent.class, Position.class);

        processAddedEntities(movableEntities);
    }

    @Override
    public void processTick(float tpf) {
        if (furnitureCollision != null) {
            furnitureCollision.update();
        }
        if (movableEntities.applyChanges()) {
            processDeletedEntities(movableEntities.getRemovedEntities());

            processAddedEntities(movableEntities.getAddedEntities());

            processChangedEntities(movableEntities.getChangedEntities());
        }

        countRadii();

        // DK II's pilot caps every steering force to 1.5 tiles a tick (0x4D5B30), as an acceleration over the tick
        float maxForce = FollowWaypoints.MAX_FORCE / (tpf * tpf);

        // Process ticks
        for (EntitySteeringBehavior steeringBehavior : steeringBehaviors.getArray()) {
            EntityId entityId = entityIdsBySteeringBehavior.get(steeringBehavior);
            ISteerableEntity steerableEntity = steerableEntitiesBySteeringBehavior.get(steeringBehavior);
            steerableEntity.setMaxLinearSpeed(getMaxSpeed(entityId, movableEntities.getEntity(entityId).get(Mobile.class), steerableEntity.getPosition()));
            steerableEntity.setMaxLinearAcceleration(maxForce);

            // Calculate steering acceleration
            SteeringAcceleration<Vector2> steeringOutput = steeringOutputsBySteeringBehaviors.get(steeringBehavior);
            FollowWaypoints pathFollowing = steeringBehavior.getPathFollowing();
            if (pathFollowing != null) {
                pathFollowing.countHeadway();
            }
            steeringBehavior.setUpdateInterval(tpf);
            steeringBehavior.calculateSteering(steeringOutput);
            Navigation navigation = movableEntities.getEntity(entityId).get(Navigation.class);
            if (steeringOutput.linear.isZero() && pathFollowing != null && pathFollowing.isFinished()) {
                // Arrival has finished linear travel. Residual velocity would
                // drift out of tolerance and oscillate at the logic tick rate.
                steerableEntity.getLinearVelocity().setZero();
            }

            /*
             * Here you might want to add a motor control layer filtering steering accelerations.
             *
             * For instance, a car in a driving game has physical constraints on its movement: it cannot turn while stationary; the
             * faster it moves, the slower it can turn (without going into a skid); it can brake much more quickly than it can
             * accelerate; and it only moves in the direction it is facing (ignoring power slides).
             */
            if (!steeringOutput.linear.isZero()) {
                separate(entityId, steerableEntity, pathFollowing, steeringOutput.linear, tpf);
            }
            if (furnitureCollision != null && entityData.getComponent(entityId, CreatureComponent.class) != null
                    && navigation.navigationPath.size() > 1 && !pathFollowing.isThroughObstacles()) {
                furnitureCollision.constrain(steerableEntity, steeringOutput,
                        navigation.endInObstacle && steeringBehavior.isOnFinalPathSegment()
                                ? navigation.navigationPath.getLast() : null, tpf);
            }
            steeringOutput.linear.limit(maxForce);

            // Apply steering acceleration
            applySteering(entityId, steerableEntity, steeringOutput, tpf);
        }

        for (Entity entity : creatures) {
            if ((steerableEntitiesByEntityId.containsKey(entity.getId()) || !pushStanding(entity.getId(), entity.get(Position.class), tpf))
                    && entityData.getComponent(entity.getId(), StandingVelocity.class) != null) {
                entityData.removeComponent(entity.getId(), StandingVelocity.class);
            }
        }
    }

    private void countRadii() {
        creatures.applyChanges();
        radiiByTile.clear();
        for (Entity entity : creatures) {
            Creature creature = getCreature(entity.getId());
            if (creature != null) {
                radiiByTile.merge(WorldUtils.vectorToPoint(entity.get(Position.class).position), CreatureSizing.getBodyRadius(creature), Float::sum);
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
     * slides along the edge, see {@link FurnitureContactSolver}.
     *
     * @param entityId the creature
     * @param position where it is
     * @param tpf the length of the tick
     * @return false if it stands still with no push
     */
    private boolean pushStanding(EntityId entityId, Position position, float tpf) {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (mobile == null) {
            return false;
        }
        SeparationPush push = entityData.getComponent(entityId, SeparationPush.class);
        Vector2 heartNormal = getHeartContact(entityId, position);
        StandingVelocity standing = entityData.getComponent(entityId, StandingVelocity.class);

        // The velocity of a tick and its change, the push less the braking
        Vector2 tickVelocity = standing == null ? new Vector2() : new Vector2(standing.velocity.x, standing.velocity.y).scl(tpf);
        if (push == null && heartNormal == null && tickVelocity.isZero(0.01f * tpf)) {
            return false;
        }
        Vector2 force = new Vector2(tickVelocity).scl(-FollowWaypoints.WEIGHT);
        if (push != null) {
            force.add(push.push.x, push.push.y);
        }
        if (heartNormal != null) {
            FurnitureContactSolver.solve(new Vector2[]{heartNormal}, 1, tickVelocity, force, FollowWaypoints.MAX_FORCE, new Vector2());
        }
        force.limit(FollowWaypoints.MAX_FORCE);
        Vector2 velocity = tickVelocity.add(force).scl(1 / tpf).limit(mobile.maxSpeed);
        Vector2 from = new Vector2(position.position.x, position.position.z);
        Vector2 location = new Vector2(from);
        movementCollision.move(location, velocity, tpf, candidate -> canMoveTo(entityId, from, candidate));
        position.position.x = location.x;
        position.position.z = location.y;
        entityData.setComponent(entityId, new Position(position.rotation, position.position));
        entityData.setComponent(entityId, new StandingVelocity(new Vector2f(velocity.x, velocity.y)));
        return true;
    }

    /**
     * Get the speed of the entity for this step. A creature other than a worker
     * walks at half speed below a fifth of its health. Walking through water
     * halves the speed and lava speeds it up by half, flying is not affected
     *
     * @param entityId the entity
     * @param mobile the mobility of the entity
     * @param position the position of the entity
     * @return the maximum speed
     */
    private float getMaxSpeed(EntityId entityId, Mobile mobile, Vector2 position) {
        float speed = mobile.maxSpeed;
        Health health = entityData.getComponent(entityId, Health.class);
        CreatureComponent creature = entityData.getComponent(entityId, CreatureComponent.class);
        if (health != null && creature != null && !creature.worker && health.health < health.maxHealth * 20 / 100) {
            speed *= 0.5f;
        }
        if (!mobile.canFly) {
            Point p = WorldUtils.vectorToPoint(position);
            if (mapInformation.isWater(p)) {
                speed *= 0.5f;
            } else if (mapInformation.isLava(p)) {
                speed *= 1.5f;
            }
        }
        return speed;
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
        if (creatureAi == null || !FIGHTING.contains(creatureAi.getCreatureState()) || creature == null
                || !(mapInformation instanceof IMapController mapController)) {
            return null;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(position.position));
        if (room == null || !room.isDungeonHeart() || room.getRoomInstance().isDestroyed()) {
            return null;
        }
        Point center = room.getRoomInstance().getCenter();
        Vector2 normal = new Vector2(position.position.x - center.x, position.position.z - center.y);
        float distance = normal.len();
        if (distance == 0 || distance > HEART_OBSTACLE_RADIUS + CreatureSizing.getBodyRadius(creature)) {
            return null;
        }
        return normal.scl(1 / distance);
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
        float radius = creature != null && creature.getAttributes().getWidth() > 0 ? CreatureSizing.getBodyRadius(creature) : DEFAULT_RADIUS;
        ISteerableEntity steerableEntity = new SteerableEntity(entity.getId(), mobile.maxSpeed, radius, position.position.x, position.position.z, -position.rotation);
        if (creature != null) {
            steerableEntity.setMaxAngularSpeed(creature.getTurnSpeed());
        }
        EntitySteeringBehavior steeringBehavior = EntitySteeringFactory.navigateToPoint(navigation.navigationPath, navigation.faceTarget, steerableEntity, navigation.target,
                this::getCrowd, creature != null && creature.getFlags() != null && creature.getFlags().contains(Creature.CreatureFlag.IS_WORKER),
                navigation.pursuingAttackTarget);
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
        return creatureComponent != null && level != null ? level.getCreature(creatureComponent.creatureId) : null;
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

            // Dirty trick to try to see if the navigation has changed. A walk
            // to the tile the creature only turned on needs path following too,
            // and so does a new walk to the same tile after the last one ended:
            // the logic tick asks for it before we see the old one removed
            Navigation navigation = entity.get(Navigation.class);
            ISteerableEntity steerableEntity = steerableEntitiesByEntityId.get(entity.getId());
            // A walk with nothing to do was dropped without any steering
            EntitySteeringBehavior steeringBehavior = steeringBehaviorsBySteerableEntity.get(steerableEntity);
            FollowWaypoints pathFollowing = steeringBehavior != null ? steeringBehavior.getPathFollowing() : null;
            if (!navigation.target.equals(targetPointsByEntityId.get(entity.getId()))
                    || navigation.navigationPath.size() > 1 && (pathFollowing == null || pathFollowing.isFinished())) {
                deleteEntity(entity);
                addEntity(entity);
                continue;
            }

            // We are only prepared for the changes in Mobile
            // Not the position, the position is managed by us only
            Mobile mobile = entity.get(Mobile.class);
            steerableEntity.setMaxLinearSpeed(mobile.maxSpeed);
        }
    }

    private boolean canMoveTo(EntityId id, Vector2 fromPosition, Vector2 candidate) {
        if (creaturesController == null || entityData.getComponent(id, CreatureComponent.class) == null) {
            return true;
        }
        Point from = WorldUtils.vectorToPoint(fromPosition);
        Point to = WorldUtils.vectorToPoint(candidate);
        if (from.equals(to)) {
            return true;
        }
        IMapController map = (IMapController) mapInformation;
        IMapTileInformation destination = map.getMapData().getTile(to);
        var controller = creaturesController.createController(id);
        if (destination != null && positionLookup != null
                && positionLookup.isTileBlocked(to, controller.getOwnerId())) {
            // A route may include breaking this door, or predate its placement.
            // Keep physical walking tile-based and let AI replan/attack it.
            entityData.removeComponent(id, Navigation.class);
            return false;
        }
        return destination != null && controller.getCost(
                map.getMapData().getTile(from), destination, map, positionLookup) != null;
    }

    private void applySteering(EntityId entityId, ISteerableEntity steerableEntity, SteeringAcceleration<Vector2> steering, float tpf) {

        // We are done
        if (steering.isZero()
                && steerableEntity.getLinearVelocity().isZero(steerableEntity.getZeroLinearSpeedThreshold())
                && isZeroAngular(steerableEntity.getAngularVelocity(), steerableEntity.getZeroLinearSpeedThreshold())) {
            FollowWaypoints pathFollowing = steeringBehaviorsBySteerableEntity.get(steerableEntity).getPathFollowing();
            // The factory creates no path follower for zero/one-point paths:
            // those requests only face a target from the current position.
            if (pathFollowing == null || pathFollowing.isFinished()) {
                entityData.removeComponent(entityId, Navigation.class);
            }
            return;
        }

        // trim speed if have no forces
        if (steering.linear.isZero() && !steerableEntity.getLinearVelocity().isZero()) {
            steerableEntity.getLinearVelocity().scl((float) Math.pow(.8f, tpf * 60));
        }
        // apply linear
        steerableEntity.getLinearVelocity().mulAdd(steering.linear, tpf).limit(steerableEntity.getMaxLinearSpeed());
        Vector2 position = steerableEntity.getPosition();
        movementCollision.move(position, steerableEntity.getLinearVelocity(), tpf,
                candidate -> canMoveTo(entityId, position, candidate));

        // Update orientation and angular velocity
        if (INDEPENDENT_FACING
                || steerableEntity.getLinearVelocity().isZero(steerableEntity.getZeroLinearSpeedThreshold())) {
            // trim speed if have no forces
            if (steering.angular == 0 && steerableEntity.getAngularVelocity() != 0) {
                steerableEntity.setAngularVelocity(steerableEntity.getAngularVelocity()
                        * (float) Math.pow(.7f, tpf * 60));
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
            // DKII bounds body yaw independently of instantaneous movement
            // direction. Convert the steering convention to body yaw for the
            // circular turn, then convert back when applying the change.
            float turn = -SteeringUtils.calculateTurnDelta(-steerableEntity.getOrientation(),
                    -newOrientation, steerableEntity.getMaxAngularSpeed() * tpf);
            steerableEntity.setAngularVelocity(tpf > 0 ? turn / tpf : 0);
            steerableEntity.setOrientation((steerableEntity.getOrientation() + turn) % (float) (Math.PI * 2));
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
     * Adds the separation push the logic tick worked out (see
     * {@link SeparationSystem}) to the path following. Near the end of its path
     * a pushed creature stops where it is, since the end point is taken.
     *
     * @param entityId the creature
     * @param steerable its steering state
     * @param pathFollowing its path following, if any
     * @param linear the path following acceleration, the push is added to it
     * @param tpf the length of the tick
     */
    private void separate(EntityId entityId, ISteerableEntity steerable, FollowWaypoints pathFollowing, Vector2 linear, float tpf) {
        SeparationPush separationPush = entityData.getComponent(entityId, SeparationPush.class);
        if (separationPush == null) {
            return;
        }

        Navigation navigation = entityData.getComponent(entityId, Navigation.class);
        if (navigation != null && !navigation.navigationPath.isEmpty()
                && steerable.getPosition().dst(navigation.navigationPath.getLast()) <= END_TOLERANCE) {
            linear.setZero();
            steerable.getLinearVelocity().setZero();
            if (pathFollowing != null) {
                pathFollowing.finish();
            }
            return;
        }

        // Like the path following, the push is a change of the velocity of a tick (0x4D4760)
        linear.add(separationPush.push.x / (tpf * tpf), separationPush.push.y / (tpf * tpf));
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
        creatures.release();
        if (furnitureCollision != null) {
            furnitureCollision.release();
        }
        radiiByTile.clear();
        steeringBehaviors.clear();
        steerableEntitiesByEntityId.clear();
        steerableEntitiesBySteeringBehavior.clear();
        steeringBehaviorsBySteerableEntity.clear();
        steeringOutputsBySteeringBehaviors.clear();
        entityIdsBySteeringBehavior.clear();
        targetPointsByEntityId.clear();
    }

}
