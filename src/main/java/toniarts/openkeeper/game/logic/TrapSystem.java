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
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapFiring;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.trap.ITrapEffects;
import toniarts.openkeeper.game.fogofwar.IBlocksSight;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Fires the armed traps. Once a trap has recharged it looks for a victim: a
 * line of sight trap aims at the nearest enemy creature it can see within its
 * trigger range (in tiles), a pressure or trigger trap goes off under a
 * creature standing on its tile. When a victim stays found for the trap's
 * charge time, the trap pays the mana for the shot, fires and starts to
 * recharge. A trap whose owner lacks the mana doesn't fire and keeps trying.
 * <br>
 * After a shot the trap threatens its enemies for the trap's threat duration,
 * this is what makes the Fear Trap's victims flee.
 *
 * @see ITrapEffects for what the shots do
 */
public final class TrapSystem implements IGameLogicUpdatable {

    /**
     * How far apart, in tiles, the line of sight is checked for walls
     */
    private static final float LINE_OF_SIGHT_STEP = 0.25f;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final IBlocksSight blocksSight;
    private final Map<Short, IPlayerController> playerControllers;
    private final ITrapEffects trapEffects;
    private final EntitySet trapEntities;
    private final EntitySet creatureEntities;

    public TrapSystem(EntityData entityData, IKwdFile kwdFile, IGameTimer gameTimer, IBlocksSight blocksSight,
            Map<Short, IPlayerController> playerControllers, ITrapEffects trapEffects) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        this.blocksSight = blocksSight;
        this.playerControllers = playerControllers;
        this.trapEffects = trapEffects;

        trapEntities = entityData.getEntities(TrapComponent.class, TrapViewState.class, Owner.class, Position.class, Health.class);
        creatureEntities = entityData.getEntities(CreatureComponent.class, Owner.class, Position.class, Health.class);
    }

    @Override
    public void processTick(float tpf) {
        trapEntities.applyChanges();
        creatureEntities.applyChanges();

        double gameTime = gameTimer.getGameTime();
        for (Entity trapEntity : trapEntities) {
            if (!trapEntity.get(TrapViewState.class).blueprint) {
                processTrap(trapEntity, gameTime);
            }
        }
    }

    private void processTrap(Entity trapEntity, double gameTime) {
        TrapFiring firing = getFiring(trapEntity.getId());
        if (gameTime >= firing.threatEndTime && entityData.getComponent(trapEntity.getId(), Threat.class) != null) {
            entityData.removeComponent(trapEntity.getId(), Threat.class);
        }
        if (gameTime < firing.rechargedTime) {
            return;
        }

        Trap trap = kwdFile.getTrapById(trapEntity.get(TrapComponent.class).trapId);
        EntityId target = findTarget(trapEntity, trap);
        if (target == null) {

            // Losing the victim starts the charging over
            if (firing.chargeStartTime != TrapFiring.NOT_CHARGING) {
                entityData.setComponent(trapEntity.getId(), new TrapFiring(firing.rechargedTime, TrapFiring.NOT_CHARGING,
                        firing.threatEndTime));
            }
            return;
        }

        double chargeStartTime = firing.chargeStartTime == TrapFiring.NOT_CHARGING ? gameTime : firing.chargeStartTime;
        if (gameTime - chargeStartTime < trap.getChargeTime() || !fire(trapEntity, trap, target, firing, gameTime)) {
            if (firing.chargeStartTime != chargeStartTime) {
                entityData.setComponent(trapEntity.getId(), new TrapFiring(firing.rechargedTime, chargeStartTime,
                        firing.threatEndTime));
            }
        }
    }

    private TrapFiring getFiring(EntityId trapId) {
        TrapFiring firing = entityData.getComponent(trapId, TrapFiring.class);
        if (firing == null) {
            return new TrapFiring(0, TrapFiring.NOT_CHARGING, 0);
        }

        return firing;
    }

    /**
     * Fires the trap if its owner can pay for the shot
     *
     * @param trapEntity the trap
     * @param trap the trap data
     * @param target the victim, can be {@code null}
     * @param firing the trap's firing state
     * @param gameTime the current game time
     * @return whether the trap fired
     */
    private boolean fire(Entity trapEntity, Trap trap, EntityId target, TrapFiring firing, double gameTime) {
        short ownerId = trapEntity.get(Owner.class).ownerId;
        if (!payForShot(trap, ownerId)) {
            return false;
        }

        trapEffects.fire(trap, ownerId, trapEntity.get(Position.class).position.clone(), target);

        // Every shot threatens the enemies anew
        double threatEndTime = firing.threatEndTime;
        if (trap.getThreat() > 0 && trap.getThreatDuration() > 0) {
            entityData.setComponent(trapEntity.getId(), new Threat(trap.getThreat()));
            threatEndTime = gameTime + trap.getThreatDuration();
        }
        entityData.setComponent(trapEntity.getId(), new TrapFiring(gameTime + trap.getRechargeTime(), TrapFiring.NOT_CHARGING,
                threatEndTime));

        return true;
    }

    private boolean payForShot(Trap trap, short ownerId) {

        // The good player has no mana, its traps fire for free
        if (!trap.getFlags().contains(Trap.TrapFlag.REQUIRE_MANA) || ownerId == Player.GOOD_PLAYER_ID) {
            return true;
        }
        IPlayerController playerController = playerControllers.get(ownerId);
        if (playerController == null) {
            return true;
        }

        int manaCost = trap.getManaCostToFire();
        if (playerController.getKeeper().getMana() < manaCost) {
            return false;
        }
        playerController.getManaControl().addMana(-manaCost);

        return true;
    }

    private EntityId findTarget(Entity trapEntity, Trap trap) {
        if (trap.getTriggerType() == null) {
            return null;
        }

        return switch (trap.getTriggerType()) {
            case LINE_OF_SIGHT ->
                findTargetInSight(trapEntity, trap.getTriggerData());
            case PRESSURE ->
                findTargetOnTrap(trapEntity, true);
            case TRIGGER ->
                findTargetOnTrap(trapEntity, false);
            default ->
                null;
        };
    }

    /**
     * The nearest enemy creature in the trap's sight
     *
     * @param trapEntity the trap
     * @param range the trigger range in tiles, the victim needs to be closer
     * @return the victim, {@code null} if none is in sight
     */
    private EntityId findTargetInSight(Entity trapEntity, int range) {
        Vector3f trapPosition = trapEntity.get(Position.class).position;
        short ownerId = trapEntity.get(Owner.class).ownerId;
        float closestDistanceSquared = range * range;
        EntityId target = null;
        for (Entity creatureEntity : creatureEntities) {
            if (!isEnemy(ownerId, creatureEntity.get(Owner.class).ownerId) || !canSetOffTraps(creatureEntity, true)) {
                continue;
            }

            Vector3f creaturePosition = creatureEntity.get(Position.class).position;
            float distanceSquared = distanceSquared(trapPosition, creaturePosition);
            if (distanceSquared < closestDistanceSquared && hasLineOfSight(blocksSight, trapPosition, creaturePosition)) {
                closestDistanceSquared = distanceSquared;
                target = creatureEntity.getId();
            }
        }

        return target;
    }

    /**
     * A creature standing on the trap. Any creature but the owner's and the
     * neutral player's will do
     *
     * @param trapEntity the trap
     * @param checkCreatureType whether creature types that don't set off traps
     * are left out, trigger traps don't care
     * @return the victim, {@code null} if nobody stands on the trap
     */
    private EntityId findTargetOnTrap(Entity trapEntity, boolean checkCreatureType) {
        Point trapTile = WorldUtils.vectorToPoint(trapEntity.get(Position.class).position);
        short ownerId = trapEntity.get(Owner.class).ownerId;
        for (Entity creatureEntity : creatureEntities) {
            short creatureOwnerId = creatureEntity.get(Owner.class).ownerId;
            if (creatureOwnerId != ownerId && creatureOwnerId != Player.NEUTRAL_PLAYER_ID
                    && trapTile.equals(WorldUtils.vectorToPoint(creatureEntity.get(Position.class).position))
                    && canSetOffTraps(creatureEntity, checkCreatureType)) {
                return creatureEntity.getId();
            }
        }

        return null;
    }

    private boolean isEnemy(short ownerId, short creatureOwnerId) {
        IPlayerController playerController = playerControllers.get(ownerId);
        if (playerController != null) {
            return playerController.getKeeper().isEnemy(creatureOwnerId);
        }

        return creatureOwnerId != ownerId && creatureOwnerId != Player.NEUTRAL_PLAYER_ID;
    }

    /**
     * Whether the creature can set off a trap. Creatures out of the world
     * (dead, knocked out or in a keeper's hand) don't, and neither does a
     * creature type with the data flag OpenKeeper calls {@code IS_HORNY}, the
     * Horned Reaper
     *
     * @param creatureEntity the creature
     * @param checkCreatureType whether to check the creature type
     * @return can the creature set off a trap
     */
    private boolean canSetOffTraps(Entity creatureEntity, boolean checkCreatureType) {
        EntityId entityId = creatureEntity.getId();
        if (creatureEntity.get(Health.class).health <= 0
                || entityData.getComponent(entityId, Unconscious.class) != null
                || entityData.getComponent(entityId, InHand.class) != null
                || entityData.getComponent(entityId, Death.class) != null) {
            return false;
        }
        if (checkCreatureType) {
            Creature creature = kwdFile.getCreature(creatureEntity.get(CreatureComponent.class).creatureId);
            return creature == null || !creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY);
        }

        return true;
    }

    private static float distanceSquared(Vector3f from, Vector3f to) {
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        return dx * dx + dz * dz;
    }

    /**
     * Whether there are no sight blocking tiles between two points on the map.
     * The tiles the points are on don't count
     *
     * @param blocksSight tells the tiles that block sight
     * @param from the point to look from
     * @param to the point to look at
     * @return is there a line of sight
     */
    static boolean hasLineOfSight(IBlocksSight blocksSight, Vector3f from, Vector3f to) {
        Point startTile = WorldUtils.vectorToPoint(from);
        Point endTile = WorldUtils.vectorToPoint(to);
        float dx = to.x - from.x;
        float dz = to.z - from.z;
        int steps = (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dz)) / LINE_OF_SIGHT_STEP);
        for (int i = 1; i < steps; i++) {
            float progress = (float) i / steps;
            Point tile = WorldUtils.vectorToPoint(from.x + dx * progress, from.z + dz * progress);
            if (!tile.equals(startTile) && !tile.equals(endTile) && blocksSight.blocksSight(tile.x, tile.y)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        trapEntities.release();
        creatureEntities.release();
    }

}
