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
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.EffectTrigger;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.controller.shot.ShotFlightRules;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Moves the flying shots, one move a turn, and lets them hit. The order is
 * DKII.EXE's shot turn (0x4AB180): the range, a hit on a thing, the move with
 * a hit on a wall, then the shot's lifetime.
 */
public final class ShotSystem implements IGameLogicUpdatable {

    /**
     * How long, in seconds, a triggered effect's entity lives. Long enough for
     * the view to see it, the effect itself plays on as long as it lasts
     */
    private static final double EFFECT_TRIGGER_LIFETIME = 1;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final IShotsController shotsController;
    private final IEntityPositionLookup entityPositionLookup;
    private final IMapInformation<?> mapInformation;
    private final Map<Short, IPlayerController> playerControllers;
    private final EntitySet shots;

    public ShotSystem(EntityData entityData, IKwdFile kwdFile, IGameTimer gameTimer, IShotsController shotsController,
            IEntityPositionLookup entityPositionLookup, IMapInformation<?> mapInformation,
            Map<Short, IPlayerController> playerControllers) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
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
        if (ShotFlightRules.isOutOfRange(flight.start, position, flight.maxRange)) {
            entityData.removeEntity(entity.getId());
            return;
        }

        EntityId thing = findThingHit(shot, flight, position);
        if (thing != null) {
            triggerEffect(shot.getHitThingEffectId(), position);
            shotsController.hit(entity.getId(), thing);
            if (shot.getFlags().contains(Shot.ShotFlag.DIE_WHEN_HIT_THING)) {
                entityData.removeEntity(entity.getId());
                return;
            }
        }

        Move move = move(position, flight.velocity);
        if (move.hitWall()) {
            triggerEffect(shot.getHitSolidEffectId(), move.position());
            if (shot.getFlags().contains(Shot.ShotFlag.DIE_WHEN_HIT_SOLID)) {
                entityData.removeEntity(entity.getId());
                return;
            }
        }

        int health = flight.health;
        if (shot.getFlags().contains(Shot.ShotFlag.DIE_OVER_TIME)) {
            health--;
            if (health < 1) {
                triggerEffect(shot.getDeathEffectId(), move.position());
                entityData.removeEntity(entity.getId());
                return;
            }
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
     * @param hitWall whether the shot hit a wall
     */
    private record Move(Vector3f position, boolean hitWall) {

    }

    /**
     * Moves the shot in steps, so that it doesn't fly through a wall
     *
     * @param position where the shot is
     * @param velocity the shot's velocity
     * @return where the move ends
     */
    private Move move(Vector3f position, Vector3f velocity) {
        int steps = ShotFlightRules.getSteps(velocity);
        Vector3f step = velocity.divide(steps);
        Vector3f result = new Vector3f(position);
        for (int i = 0; i < steps; i++) {
            result.addLocal(step);
            if (isSolid(result)) {
                return new Move(result.subtractLocal(step), true);
            }
        }

        return new Move(result, false);
    }

    /**
     * Sets off the shot's effect where it is, the shot's hit or its death
     * (0x4ABD60, 0x4AB180)
     *
     * @param effectId the effect, 0 for none
     * @param position where to play it
     */
    private void triggerEffect(int effectId, Vector3f position) {
        if (effectId == 0) {
            return;
        }

        EntityId effect = entityData.createEntity();
        entityData.setComponents(effect,
                new Position(0, new Vector3f(position)),
                new EffectTrigger(effectId),
                new Decay(gameTimer.getGameTime(), EFFECT_TRIGGER_LIFETIME));
    }

    private boolean isSolid(Vector3f position) {
        Point tile = WorldUtils.vectorToPoint(position);

        return mapInformation.getMapData().getTile(tile) == null || mapInformation.isSolid(tile);
    }

    /**
     * Looks for a thing the shot touches, on the tiles under the shot. For now
     * the shots hit creatures and traps.
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

        float radius = shot.getRadius();
        Point min = WorldUtils.vectorToPoint(position.x - radius, position.z - radius);
        Point max = WorldUtils.vectorToPoint(position.x + radius, position.z + radius);
        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    if (!entityId.equals(flight.shooter) && isHit(shot, flight, position, entityId)) {
                        return entityId;
                    }
                }
            }
        }

        return null;
    }

    private boolean isHit(Shot shot, ShotFlight flight, Vector3f position, EntityId entityId) {
        Health health = entityData.getComponent(entityId, Health.class);
        Owner owner = entityData.getComponent(entityId, Owner.class);
        Position thingPosition = entityData.getComponent(entityId, Position.class);
        if (health == null || health.health <= 0 || owner == null || thingPosition == null) {
            return false;
        }

        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        if (creatureComponent == null) {
            return isTrapHit(shot, flight, position, entityId, owner, thingPosition);
        }
        if (!collidesWith(shot.getCollideType(), flight.ownerId, owner.ownerId)) {
            return false;
        }

        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);

        return ShotFlightRules.touches(position, shot.getRadius(), thingPosition.position,
                creature.getAttributes().getWidth(), creatureComponent.height);
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

    private boolean collidesWith(Shot.CollideType collideType, short shotOwnerId, short creatureOwnerId) {
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
