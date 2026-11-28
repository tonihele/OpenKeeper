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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.controller.shot.ShotFlightRules;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Moves the flying shots, one move a turn, and lets them hit. The order is
 * DKII.EXE's shot turn (0x4AB180): the range, what the shot does on every
 * turn, an instant shot's move onto its target, a hit on a thing, the damage
 * around the shot, the move with a hit on a wall, then the shot's lifetime.
 */
public final class ShotSystem implements IGameLogicUpdatable {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final GameEventService gameEventService;
    private final IShotsController shotsController;
    private final IEntityPositionLookup entityPositionLookup;
    private final IMapInformation<?> mapInformation;
    private final Map<Short, IPlayerController> playerControllers;
    private final EntitySet shots;
    private final Vector3f halfway = new Vector3f();

    public ShotSystem(EntityData entityData, IKwdFile kwdFile, GameEventService gameEventService, IShotsController shotsController,
            IEntityPositionLookup entityPositionLookup, IMapInformation<?> mapInformation,
            Map<Short, IPlayerController> playerControllers) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameEventService = gameEventService;
        this.shotsController = shotsController;
        this.entityPositionLookup = entityPositionLookup;
        this.mapInformation = mapInformation;
        this.playerControllers = playerControllers;

        shots = entityData.getEntities(ShotFlight.class, Position.class);
    }

    @Override
    public void processTick(float tpf) {
        shots.applyChanges();
        for (Entity entity : shots) {
            processShot(entity);
        }
    }

    private void processShot(Entity entity) {
        ShotFlight flight = entity.get(ShotFlight.class);
        Shot shot = kwdFile.getShotById(flight.shotId);
        Vector3f position = entity.get(Position.class).position;

        // A shot out of its range, or one that dies when it hits a thing, has the rest of its turn and dies at the end
        // of it, it leaves its death shot (0x4AB180)
        int health = flight.health;
        if (ShotFlightRules.isOutOfRange(flight.start, position, flight.maxRange)) {
            health = 0;
        }
        if (shot.getShotProcessFlags().contains(Shot.ShotProcessFlag.CONSTANT)) {
            shotsController.fly(entity.getId());
        }

        // An instant shot, like Hail Storm, goes onto its target and hits it, whatever the shot collides with. With
        // its target gone it dies (0x4AFCB0, 0x4ABD60)
        EntityId thing = null;
        if (shot.getProcessType() == Shot.ProcessType.INSTANTANEOUS_HIT) {
            Position target = flight.target != null ? entityData.getComponent(flight.target, Position.class) : null;
            if (target != null) {
                position = target.position;
                thing = flight.target;
            } else {
                health = 0;
            }
        }
        if (thing == null) {
            thing = findThingHit(shot, flight, position);
        }
        if (thing != null) {
            triggerEffect(shot.getHitThingEffectId(), position);
            createChildShot(entity.getId(), shot.getHitThingShotId(), position);
            shotsController.hit(entity.getId(), thing);
            if (shot.getFlags().contains(Shot.ShotFlag.DIE_WHEN_HIT_THING)) {
                health = 0;
            }
        }
        shotsController.damageArea(entity.getId());

        Vector3f velocity = flight.velocity;
        if (thing == null && findThingHit(shot, flight, halfway.set(velocity).multLocal(0.5f).addLocal(position)) != null) {

            // A thing halfway along the move, the move ends there so that the shot can't step over it (0x4ABD60)
            velocity = velocity.mult(0.5f);
        }
        Move move = move(position, velocity);
        if (move.door() != null) {
            flight = stopAtDoor(entity.getId(), shot, flight, move.door());
        }
        if (move.hitWall()) {
            triggerEffect(shot.getHitSolidEffectId(), move.position());
            createChildShot(entity.getId(), shot.getHitSolidShotId(), move.position());
            if (shot.getFlags().contains(Shot.ShotFlag.DIE_WHEN_HIT_SOLID)) {
                entityData.removeEntity(entity.getId());
                return;
            }
        }

        if (shot.getFlags().contains(Shot.ShotFlag.DIE_OVER_TIME)) {
            health--;
        }
        if (health < 1) {
            triggerEffect(shot.getDeathEffectId(), move.position());
            createChildShot(entity.getId(), shot.getDeathShotId(), move.position());
            entityData.removeEntity(entity.getId());
            return;
        }
        if (health != flight.health) {
            entityData.setComponent(entity.getId(), new ShotFlight(flight, health));
        }
        if (!move.hitWall()) {
            entityData.setComponent(entity.getId(), new Position(entity.get(Position.class).rotation, move.position()));
        }
    }

    /**
     * Where a move of a shot ends
     *
     * @param position the new position, the last one before the wall if the
     * shot hit one
     * @param hitWall whether the shot hit a wall or a shut door
     * @param door the shut door the shot hit, {@code null} if none
     */
    private record Move(Vector3f position, boolean hitWall, EntityId door) {

    }

    /**
     * Moves the shot in steps, so that it doesn't fly through a wall or a shut
     * door. A shot that starts its move in a door isn't stopped by that door
     * (0x4B2030, 0x4529A0).
     *
     * @param position where the shot is
     * @param velocity the shot's velocity
     * @return where the move ends
     */
    private Move move(Vector3f position, Vector3f velocity) {
        int steps = ShotFlightRules.getSteps(velocity);
        Vector3f step = velocity.divide(steps);
        Vector3f result = new Vector3f(position);
        boolean inDoor = getShutDoor(position) != null;
        for (int i = 0; i < steps; i++) {
            result.addLocal(step);
            if (isSolid(result)) {
                return new Move(result.subtractLocal(step), true, null);
            }
            EntityId door = getShutDoor(result);
            if (door != null && !inDoor) {
                return new Move(result.subtractLocal(step), true, door);
            }
            inDoor = door != null;
        }

        return new Move(result, false, null);
    }

    private EntityId getShutDoor(Vector3f position) {
        Point tile = WorldUtils.vectorToPoint(position);
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile.x, tile.y)) {
            if (isShutDoor(entityId)) {
                return entityId;
            }
        }

        return null;
    }

    /**
     * A shot stopped by a shut door stays where it is, and unless it bounces
     * it does its damage to the door, as to a thing it hits (0x4B2030,
     * 0x4AC420)
     *
     * @param shotEntity the shot
     * @param shot the shot's type
     * @param flight the shot's flight
     * @param door the door hit
     * @return the flight of the stopped shot
     */
    private ShotFlight stopAtDoor(EntityId shotEntity, Shot shot, ShotFlight flight, EntityId door) {
        if (!shot.getFlags().contains(Shot.ShotFlag.BOUNCE)) {
            shotsController.dealDamage(shotEntity, door);
        }
        ShotFlight stopped = new ShotFlight(flight.shotId, flight.ownerId, flight.shooter, flight.target, flight.shotData1,
                flight.shotData2, new Vector3f(0, flight.velocity.y, 0), flight.start, flight.maxRange, flight.health);
        entityData.setComponent(shotEntity, stopped);

        return stopped;
    }

    /**
     * Tells the clients to play the shot's effect where it is, the shot's hit
     * or its death (0x4ABD60, 0x4AB180)
     *
     * @param effectId the effect, 0 for none
     * @param position where to play it
     */
    private void triggerEffect(int effectId, Vector3f position) {
        if (effectId != 0) {
            gameEventService.emitEffect(effectId, position, 0);
        }
    }

    /**
     * The shot leaves its hit or death shot where it is, like the cloud of the
     * Gas Missile (0x4ABD60, 0x4AB180)
     *
     * @param shot the shot entity
     * @param shotTypeId the shot to leave, 0 for none
     * @param position where to leave it
     */
    private void createChildShot(EntityId shot, short shotTypeId, Vector3f position) {
        if (shotTypeId != 0) {
            shotsController.createChildShot(shot, shotTypeId, position);
        }
    }

    private boolean isSolid(Vector3f position) {
        Point tile = WorldUtils.vectorToPoint(position);

        return mapInformation.getMapData().getTile(tile) == null || mapInformation.isSolid(tile);
    }

    /**
     * Looks for a thing the shot touches, on the tiles under the shot. For now
     * the shots hit creatures, dead bodies, traps and doors.
     *
     * @param shot the shot
     * @param flight the shot's flight
     * @param position where the shot is
     * @return the thing hit, {@code null} if none
     */
    private EntityId findThingHit(Shot shot, ShotFlight flight, Vector3f position) {
        if (shot.getCollideType() == null || shot.getCollideType() == Shot.CollideType.NONE) {
            return null;
        }

        // A shot never hits its shooter, unless it was cast on its shooter, then it hits nothing else, like the Modify
        // Health a Heal on the caster itself leaves (0x451570)
        boolean castOnShooter = flight.shooter != null && flight.shooter.equals(flight.target);
        float radius = shot.getRadius();
        Point min = WorldUtils.vectorToPoint(position.x - radius, position.z - radius);
        Point max = WorldUtils.vectorToPoint(position.x + radius, position.z + radius);
        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    if (entityId.equals(flight.shooter) == castOnShooter && isHit(shot, flight, position, entityId)) {
                        return entityId;
                    }
                }
            }
        }

        return null;
    }

    private boolean isHit(Shot shot, ShotFlight flight, Vector3f position, EntityId entityId) {
        Position thingPosition = entityData.getComponent(entityId, Position.class);
        if (thingPosition == null) {
            return false;
        }

        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        if (entityData.getComponent(entityId, DoorComponent.class) != null) {

            // A door has no width nor height (0x402AD0 for both in the door's vtable). A shot that collides with dead
            // bodies takes nothing else (0x4B59B0)
            return shot.getCollideType() != Shot.CollideType.DEAD_BODIES && isShutDoor(entityId)
                    && ShotFlightRules.touches(position, shot.getRadius(), thingPosition.position, 0, 0);
        }
        if (creatureComponent == null) {
            Health health = entityData.getComponent(entityId, Health.class);
            Owner owner = entityData.getComponent(entityId, Owner.class);
            return health != null && health.health > 0 && owner != null
                    && isTrapHit(shot, flight, position, entityId, owner, thingPosition);
        }
        if (!collidesWith(shot.getCollideType(), flight.ownerId, entityId)) {
            return false;
        }

        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);

        return ShotFlightRules.touches(position, shot.getRadius(), thingPosition.position,
                creature.getAttributes().getWidth(), creatureComponent.height);
    }

    private boolean collidesWith(Shot.CollideType collideType, short shotOwnerId, EntityId entityId) {
        if (collideType == Shot.CollideType.DEAD_BODIES) {
            return entityData.getComponent(entityId, Death.class) != null;
        }

        Health health = entityData.getComponent(entityId, Health.class);
        Owner owner = entityData.getComponent(entityId, Owner.class);
        if (health == null || health.health <= 0 || owner == null) {
            return false;
        }

        short creatureOwnerId = owner.ownerId;
        return switch (collideType) {
            case OWN_CREATURES ->
                creatureOwnerId == shotOwnerId;
            case ENEMY_CREATURES ->
                isEnemy(shotOwnerId, creatureOwnerId);
            case ALL_CREATURES ->
                creatureOwnerId == shotOwnerId || isEnemy(shotOwnerId, creatureOwnerId);
            default ->
                false;
        };
    }

    /**
     * An armed enemy trap that isn't hidden is hit by the shots aimed at
     * enemies (0x4B5800). A trap has no width nor height (0x402AD0 for both).
     */
    private boolean isTrapHit(Shot shot, ShotFlight flight, Vector3f position, EntityId entityId, Owner owner,
            Position trapPosition) {
        TrapViewState trapViewState = entityData.getComponent(entityId, TrapViewState.class);
        boolean hitsEnemies = shot.getCollideType() == Shot.CollideType.ENEMY_CREATURES
                || shot.getCollideType() == Shot.CollideType.ALL_CREATURES;
        if (trapViewState == null || trapViewState.blueprint || trapViewState.hidden || !hitsEnemies
                || !isEnemy(flight.ownerId, owner.ownerId)) {
            return false;
        }

        return ShotFlightRules.touches(position, shot.getRadius(), trapPosition.position, 0, 0);
    }

    /**
     * A built, shut door that isn't a barricade stops the shots that fly into
     * it (0x4B2030, 0x4529A0) and is hit by every shot that hits things,
     * whoever owns the door: the shots aimed at enemies (0x4B5800) and those
     * aimed at our own (0x4B58E0) both take a door that isn't a barricade
     * (flag 0x40 of the door data), a blueprint (+0x109) or open (+0x10D).
     * The shots fly over a barricade.
     *
     * @param entityId the thing
     * @return whether the thing is such a door
     */
    private boolean isShutDoor(EntityId entityId) {
        DoorComponent doorComponent = entityData.getComponent(entityId, DoorComponent.class);
        if (doorComponent == null || doorComponent.blueprint
                || kwdFile.getDoorById(doorComponent.doorId).getFlags().contains(Door.DoorFlag.IS_BARRICADE)) {
            return false;
        }
        DoorViewState doorViewState = entityData.getComponent(entityId, DoorViewState.class);

        return doorViewState == null || !doorViewState.open;
    }

    private boolean isEnemy(short shotOwnerId, short creatureOwnerId) {
        IPlayerController playerController = playerControllers.get(shotOwnerId);
        if (playerController != null) {
            return playerController.getKeeper().isEnemy(creatureOwnerId);
        }

        return creatureOwnerId != shotOwnerId && creatureOwnerId != Player.NEUTRAL_PLAYER_ID;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        shots.release();
    }

}
