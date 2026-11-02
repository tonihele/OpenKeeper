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
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringBehavior;
import toniarts.openkeeper.game.navigation.steering.EntitySteeringFactory;
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
     * DK II's pilot maximum force (1.5 tiles, 0x4D5B30). The separation push is
     * measured against it.
     */
    private static final float MAX_FORCE = 1.5f;

    /**
     * DK II weighs the separation 0.13 and the path following 0.35 (0x4D56E0).
     * Path following has weight 1 here.
     */
    private static final float SEPARATION_WEIGHT = 0x2147 / (float) 0x5999;

    /**
     * DK II counts a path as finished this close to its final waypoint, 0.38
     * tiles (0x4D3C40). A creature pushed away from a taken end point stops
     * within it.
     */
    private static final float END_TOLERANCE = 0x6148 / 65536f;
    
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
    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final Vector2 separation = new Vector2();
    private final Vector2 away = new Vector2();

    public MovementSystem(EntityData entityData, IKwdFile kwdFile, IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;
        movableEntities = entityData.getEntities(Position.class, Mobile.class, Navigation.class);

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
            EntityId entityId = entityIdsBySteeringBehavior.get(steeringBehavior);
            ISteerableEntity steerableEntity = steerableEntitiesBySteeringBehavior.get(steeringBehavior);
            if (!steeringOutput.linear.isZero()) {
                separate(entityId, steerableEntity, steeringOutput.linear);
            }

            // Apply steering acceleration
            applySteering(entityId, steerableEntity, steeringOutput, tpf);
        }
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
        ISteerableEntity steerableEntity = new SteerableEntity(entity.getId(), mobile.maxSpeed, 0.25f, position.position.x, position.position.z, -position.rotation);
        EntitySteeringBehavior steeringBehavior = EntitySteeringFactory.navigateToPoint(navigation.navigationPath, navigation.faceTarget, steerableEntity, navigation.target);
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
        steerableEntity.getPosition().add(steerableEntity.getLinearVelocity().x * tpf, steerableEntity.getLinearVelocity().y * tpf);

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
     * Pushes a walking creature away from the creatures it overlaps, the way
     * DK II's separation steering does (0x4D2DF0, 0x4D2B80). Every creature
     * counts, whoever owns it. Two creatures overlap when they are closer than
     * the sum of their radii, half their widths. The push grows as
     * 1/sqrt(d/R) - 1, is shared by mass, and is stronger when moving away from
     * the other one. Near the end of its path a pushed creature stops where it
     * is, since the end point is taken. The push stays weaker than the path
     * following, so it can't move a creature off its path into a wall.
     *
     * @param entityId the creature
     * @param steerable its steering state
     * @param linear the path following acceleration, the push is added to it
     */
    private void separate(EntityId entityId, ISteerableEntity steerable, Vector2 linear) {
        Creature creature = getCreature(entityId);
        if (creature == null) {
            return;
        }

        Vector2 position = steerable.getPosition();
        Vector2 velocity = steerable.getLinearVelocity();
        float radius = creature.getAttributes().getWidth() / 2;
        float mass = Math.max(1, creature.getUnkea8()); // Mass 1-5, 0x4D2A60
        Point tile = WorldUtils.vectorToPoint(position);
        separation.setZero();
        for (int x = Math.max(0, tile.x - 1); x <= Math.min(mapController.getMapData().getWidth() - 1, tile.x + 1); x++) {
            for (int y = Math.max(0, tile.y - 1); y <= Math.min(mapController.getMapData().getHeight() - 1, tile.y + 1); y++) {
                for (EntityId otherId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Creature other = otherId.equals(entityId) ? null : getCreature(otherId);
                    Position otherPosition = entityData.getComponent(otherId, Position.class);
                    if (other == null || otherPosition == null) {
                        continue;
                    }

                    float reach = radius + other.getAttributes().getWidth() / 2;
                    away.set(position.x - otherPosition.position.x, position.y - otherPosition.position.z);
                    float distance = away.len();
                    if (distance > reach) {
                        continue;
                    }
                    if (distance == 0) {

                        // Exactly on top of each other, split them the same way every time
                        away.set(entityId.getId() < otherId.getId() ? -1 : 1, 0);
                    }
                    away.nor();
                    float ratio = distance / reach;
                    float push = ratio < 0.01f ? MAX_FORCE : (float) (1 / Math.sqrt(ratio)) - 1;
                    float otherMass = Math.max(1, other.getUnkea8());
                    float alignment = (velocity.isZero() ? 0 : away.dot(velocity) / velocity.len()) / 2 + 0.7f;
                    separation.mulAdd(away, push * otherMass / (mass + otherMass) * alignment);
                }
            }
        }
        if (separation.isZero()) {
            return;
        }

        Navigation navigation = entityData.getComponent(entityId, Navigation.class);
        if (navigation != null && !navigation.navigationPath.isEmpty()
                && position.dst(navigation.navigationPath.get(navigation.navigationPath.size() - 1)) <= END_TOLERANCE) {
            linear.setZero();
            velocity.setZero();
            return;
        }

        separation.limit(MAX_FORCE);
        linear.mulAdd(separation, steerable.getMaxLinearAcceleration() * SEPARATION_WEIGHT / MAX_FORCE)
                .limit(steerable.getMaxLinearAcceleration());
    }

    private Creature getCreature(EntityId entityId) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent == null ? null : kwdFile.getCreature(creatureComponent.creatureId);
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
        steeringBehaviors.clear();
        steerableEntitiesByEntityId.clear();
        steerableEntitiesBySteeringBehavior.clear();
        steeringBehaviorsBySteerableEntity.clear();
        steeringOutputsBySteeringBehaviors.clear();
        entityIdsBySteeringBehavior.clear();
        targetPointsByEntityId.clear();
    }

}
