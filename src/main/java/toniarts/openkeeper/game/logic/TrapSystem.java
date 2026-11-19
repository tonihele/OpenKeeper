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
import com.simsilica.es.filter.FieldFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapFiring;
import toniarts.openkeeper.game.component.TrapTarget;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.TriggerWave;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.Variable;
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
 * <br>
 * A Trigger Trap sets off its owner's traps around it. A trap next to an armed
 * Trigger Trap leaves the triggering to it and doesn't trigger on its own.
 * When the Trigger Trap fires, a wave spreads from it at the level's trigger
 * speed to one tile, the four tiles next to it, and every recharged trap of
 * the owner it reaches fires. A trap that can see the Trigger Trap's victim
 * fires at it, the others fire without a target.
 * <br>
 * A hidden trap shows itself to the other players when it fires, if it is
 * revealed when fired. Creatures that reveal traps reveal the hidden traps of
 * their enemies on their tile and the eight tiles around it.
 * <br>
 * A door with a trap, the Magic Door, fires the trap's shot at the first
 * creature of another keeper, an ally's too, it has a clear line to in the
 * {@value #DOOR_TRAP_RANGE} tiles around it, but only while it is closed. Its owner pays the trap's mana for the shot
 * and the door recharges like a trap (0x4A55F0). The door doesn't charge,
 * threaten or show itself like a trap does.
 *
 * @see IShotsController for what the shots do
 */
public final class TrapSystem implements IGameLogicUpdatable {

    /**
     * How far, in tiles, a Trigger Trap's wave reaches. One tile, so it reaches
     * the four tiles next to the Trigger Trap but not the diagonal ones
     * (0x4A8E70)
     */
    private static final float TRIGGER_TRAP_RANGE = 1;

    /**
     * Where a Trigger Trap needs to be to take over another trap's triggering.
     * Only the four tiles next to it, not the diagonal ones
     */
    private static final int[][] TRIGGER_TRAP_TAKEOVER_TILES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * How far, in tiles, a creature that reveals traps reveals them. Its own
     * tile and the eight around it
     */
    private static final int TRAP_REVEAL_RANGE = 1;

    /**
     * How far, in tiles, a door with a trap looks for a victim. The 5 x 5
     * tiles around it
     */
    private static final int DOOR_TRAP_RANGE = 2;

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IGameTimer gameTimer;
    private final IEntityPositionLookup entityPositionLookup;
    private final Map<Short, IPlayerController> playerControllers;
    private final IShotsController shotsController;
    private final GameEventService gameEventService;
    private final float triggerSpeed;
    private final EntitySet trapEntities;
    private final EntitySet hiddenTrapEntities;
    private final EntitySet triggerWaves;
    private final EntitySet doorTrapEntities;

    /**
     * The trap system
     *
     * @param entityData the entity data
     * @param kwdFile the level data, for the traps and creatures
     * @param gameTimer the game time
     * @param entityPositionLookup finds the things on a tile and tells what
     * a trap sees
     * @param playerControllers the players, for their mana and enemies
     * @param shotsController fires the shots
     * @param gameEventService shows the effects
     */
    public TrapSystem(EntityData entityData, IKwdFile kwdFile, IGameTimer gameTimer, IEntityPositionLookup entityPositionLookup,
            Map<Short, IPlayerController> playerControllers, IShotsController shotsController, GameEventService gameEventService) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.gameTimer = gameTimer;
        this.entityPositionLookup = entityPositionLookup;
        this.playerControllers = playerControllers;
        this.shotsController = shotsController;
        this.gameEventService = gameEventService;
        triggerSpeed = kwdFile.getVariables().get(
                Variable.MiscVariable.MiscType.TRIGGER_TRAP_TRIGGER_SPEED_TILES_PER_SECOND).getValue();

        trapEntities = entityData.getEntities(TrapComponent.class, TrapViewState.class, Owner.class, Position.class, Health.class);
        hiddenTrapEntities = entityData.getEntities(new FieldFilter<>(TrapViewState.class, "hidden", true),
                TrapViewState.class, Owner.class, Position.class, Health.class);
        triggerWaves = entityData.getEntities(TriggerWave.class);
        doorTrapEntities = entityData.getEntities(TrapComponent.class, DoorComponent.class, DoorViewState.class, Owner.class,
                Position.class, Health.class);
    }

    @Override
    public void processTick(float tpf) {
        trapEntities.applyChanges();

        revealHiddenTraps();

        double gameTime = gameTimer.getGameTime();
        for (Entity trapEntity : trapEntities) {
            if (!trapEntity.get(TrapViewState.class).blueprint) {
                processTrap(trapEntity, gameTime, tpf);
            }
        }

        doorTrapEntities.applyChanges();
        for (Entity doorEntity : doorTrapEntities) {
            if (!doorEntity.get(DoorComponent.class).blueprint) {
                processDoorTrap(doorEntity, gameTime);
            }
        }

        // A wave can start new waves, those spread from the next tick on
        triggerWaves.applyChanges();
        for (Entity waveEntity : triggerWaves) {
            TriggerWave triggerWave = waveEntity.get(TriggerWave.class);
            Set<EntityId> reachedTraps = new HashSet<>(triggerWave.reachedTraps);
            if (spreadTriggerWave(triggerWave, reachedTraps, gameTime)) {
                entityData.removeEntity(waveEntity.getId());
            } else if (reachedTraps.size() != triggerWave.reachedTraps.size()) {
                waveEntity.set(new TriggerWave(triggerWave.triggerTrap, triggerWave.ownerId, triggerWave.origin,
                        triggerWave.target, triggerWave.startTime, reachedTraps));
            }
        }
    }

    private void processTrap(Entity trapEntity, double gameTime, float tpf) {
        TrapFiring firing = getFiring(trapEntity.getId());
        if (gameTime >= firing.threatEndTime && entityData.getComponent(trapEntity.getId(), Threat.class) != null) {
            entityData.removeComponent(trapEntity.getId(), Threat.class);
        }
        if (gameTime < firing.rechargedTime) {
            return;
        }

        Trap trap = kwdFile.getTrapById(trapEntity.get(TrapComponent.class).trapId);
        if (trap.getShotTypeId() == 0) {

            // Nothing to fire, the Guard Post for one
            return;
        }
        EntityId target = isTriggeredByTriggerTrap(trapEntity, trap) ? null : findTarget(trapEntity, trap, gameTime, tpf);
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

    private void processDoorTrap(Entity doorEntity, double gameTime) {
        if (gameTime < getFiring(doorEntity.getId()).rechargedTime) {
            return;
        }

        Trap trap = kwdFile.getTrapById(doorEntity.get(TrapComponent.class).trapId);
        short ownerId = doorEntity.get(Owner.class).ownerId;
        EntityId target = findTargetAroundDoor(doorEntity, ownerId);
        if (target == null || doorEntity.get(DoorViewState.class).open) {
            return;
        }
        if (!payForShot(trap, ownerId)) {

            // Without the mana the door shows its trap's powerless effect and tries again on the next tick (0x4A55F0)
            if (trap.getPowerlessEffectId() != 0) {
                gameEventService.emitEffect(trap.getPowerlessEffectId(), doorEntity.get(Position.class).position, 0);
            }
            return;
        }

        // The shot starts half a tile above the door
        Vector3f shotStart = doorEntity.get(Position.class).position.add(0, WorldUtils.TILE_HEIGHT / 2, 0);
        shotsController.fireShot(trap.getShotTypeId(), trap.getShotData1(), trap.getShotData2(), ownerId, shotStart, target,
                doorEntity.getId(), 0);
        entityData.setComponent(doorEntity.getId(), new TrapFiring(gameTime + trap.getRechargeTime(), TrapFiring.NOT_CHARGING, 0));
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

        Vector3f trapPosition = trapEntity.get(Position.class).position;
        if (!fireFlyingShot(trapEntity, trap, ownerId, trapPosition, target)) {
            shotsController.createShot(trap.getShotTypeId(), trap.getShotData1(), trap.getShotData2(), ownerId,
                    trapPosition.clone(), target, null);
        }
        if (trap.getFlags().contains(Trap.TrapFlag.REVEAL_WHEN_FIRED)) {
            reveal(trapEntity);
        }

        // Every shot threatens the enemies anew
        double threatEndTime = firing.threatEndTime;
        if (trap.getThreat() > 0 && trap.getThreatDuration() > 0) {
            entityData.setComponent(trapEntity.getId(), new Threat(trap.getThreat()));
            threatEndTime = gameTime + trap.getThreatDuration();
        }
        entityData.setComponent(trapEntity.getId(), new TrapFiring(gameTime + trap.getRechargeTime(), TrapFiring.NOT_CHARGING,
                threatEndTime));

        if (trap.getTriggerType() == Trap.TriggerType.TRIGGER && target != null) {
            entityData.setComponent(entityData.createEntity(), new TriggerWave(trapEntity.getId(), ownerId,
                    trapEntity.get(Position.class).position.clone(), target, gameTime, Set.of()));
        }

        return true;
    }

    /**
     * A line of sight trap, like the Sentry Trap, sends a flying shot after its
     * victim (0x4A7FD0). A pressure trap's shot hits the creature on the trap
     * at once (0x4A80E0).
     *
     * @param trapEntity the trap
     * @param trap the trap data
     * @param ownerId the trap's owner
     * @param trapPosition where the trap is
     * @param target the victim, can be {@code null}
     * @return whether the shot flies
     */
    private boolean fireFlyingShot(Entity trapEntity, Trap trap, short ownerId, Vector3f trapPosition, EntityId target) {
        if (trap.getTriggerType() != Trap.TriggerType.LINE_OF_SIGHT || target == null) {
            return false;
        }

        return shotsController.fireShot(trap.getShotTypeId(), trap.getShotData1(), trap.getShotData2(), ownerId,
                getShotStart(trap, trapPosition, target), target, trapEntity.getId(), 0);
    }

    /**
     * Where the trap's shot starts. Like a creature's, the shot offset is in
     * front of the trap, the trap turned to its victim, and above the floor.
     *
     * @param trap the trap data
     * @param trapPosition where the trap is
     * @param target the victim
     * @return the start of the shot
     */
    private Vector3f getShotStart(Trap trap, Vector3f trapPosition, EntityId target) {
        Vector3f result = new Vector3f(trapPosition);
        Entity targetEntity = getCreature(target);
        if (targetEntity != null) {
            Vector3f forward = targetEntity.get(Position.class).position.subtract(trapPosition).setY(0);
            if (forward.lengthSquared() > 0) {
                result.addLocal(forward.normalizeLocal().multLocal(-trap.getShotOffset().y));
            }
        }
        result.y += trap.getShotOffset().z;

        return result;
    }

    /**
     * Whether an armed Trigger Trap of the same owner is next to the trap.
     * Then the trap leaves the triggering to the Trigger Trap
     *
     * @param trapEntity the trap
     * @param trap the trap data
     * @return does a Trigger Trap trigger this trap
     */
    private boolean isTriggeredByTriggerTrap(Entity trapEntity, Trap trap) {
        if (trap.getTriggerType() == Trap.TriggerType.TRIGGER) {
            return false;
        }

        short ownerId = trapEntity.get(Owner.class).ownerId;
        Point trapTile = WorldUtils.vectorToPoint(trapEntity.get(Position.class).position);
        for (int[] offset : TRIGGER_TRAP_TAKEOVER_TILES) {
            for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(trapTile.x + offset[0], trapTile.y + offset[1])) {
                Entity otherTrapEntity = trapEntities.getEntity(entityId);
                if (otherTrapEntity != null && otherTrapEntity.get(Owner.class).ownerId == ownerId
                        && !otherTrapEntity.get(TrapViewState.class).blueprint
                        && kwdFile.getTrapById(otherTrapEntity.get(TrapComponent.class).trapId).getTriggerType() == Trap.TriggerType.TRIGGER) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Fires the recharged traps the Trigger Trap's wave reaches
     *
     * @param triggerWave the wave
     * @param reachedTraps the traps the wave has reached, the traps it reaches
     * now are added
     * @param gameTime the current game time
     * @return whether the wave has reached as far as it goes
     */
    private boolean spreadTriggerWave(TriggerWave triggerWave, Set<EntityId> reachedTraps, double gameTime) {
        float radius = (float) Math.min(TRIGGER_TRAP_RANGE, (gameTime - triggerWave.startTime) * triggerSpeed);
        Entity victim = getCreature(triggerWave.target);
        for (Entity trapEntity : trapEntities) {
            Vector3f trapPosition = trapEntity.get(Position.class).position;
            if (trapEntity.get(TrapViewState.class).blueprint || trapEntity.get(Owner.class).ownerId != triggerWave.ownerId
                    || trapEntity.getId().equals(triggerWave.triggerTrap)
                    || distanceSquared(triggerWave.origin, trapPosition) > radius * radius
                    || !reachedTraps.add(trapEntity.getId())) {
                continue;
            }

            TrapFiring firing = getFiring(trapEntity.getId());
            if (gameTime < firing.rechargedTime) {
                continue;
            }
            Trap trap = kwdFile.getTrapById(trapEntity.get(TrapComponent.class).trapId);
            if (trap.getShotTypeId() == 0) {
                continue;
            }
            EntityId target = null;
            if (victim != null && (trap.getTriggerType() == Trap.TriggerType.TRIGGER
                    || entityPositionLookup.hasLineOfSight(trapPosition, victim.get(Position.class).position))) {
                target = victim.getId();
            }
            fire(trapEntity, trap, target, firing, gameTime);
        }

        return radius >= TRIGGER_TRAP_RANGE;
    }

    /**
     * Shows the hidden traps to the other players
     *
     * @param trapEntity the trap
     */
    private void reveal(Entity trapEntity) {
        TrapViewState viewState = trapEntity.get(TrapViewState.class);
        if (viewState.hidden) {
            entityData.setComponent(trapEntity.getId(), new TrapViewState(viewState.trapId, viewState.blueprint, false));
        }
    }

    /**
     * The creatures that reveal traps reveal the hidden traps of their
     * enemies around them. There are fewer hidden traps than creatures, so
     * this looks around the hidden traps
     */
    private void revealHiddenTraps() {
        hiddenTrapEntities.applyChanges();
        for (Entity trapEntity : hiddenTrapEntities) {
            if (isRevealed(trapEntity)) {
                reveal(trapEntity);
            }
        }
    }

    private boolean isRevealed(Entity trapEntity) {
        Point trapTile = WorldUtils.vectorToPoint(trapEntity.get(Position.class).position);
        short ownerId = trapEntity.get(Owner.class).ownerId;
        for (int x = trapTile.x - TRAP_REVEAL_RANGE; x <= trapTile.x + TRAP_REVEAL_RANGE; x++) {
            for (int y = trapTile.y - TRAP_REVEAL_RANGE; y <= trapTile.y + TRAP_REVEAL_RANGE; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Entity creatureEntity = getCreature(entityId);
                    if (creatureEntity != null && isEnemy(creatureEntity.get(Owner.class).ownerId, ownerId)
                            && revealsTraps(creatureEntity)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Gets a creature, if the entity is one and still has everything the
     * traps need from it
     *
     * @param entityId the entity ID
     * @return the creature, or {@code null} if it is not a creature (any more)
     */
    private Entity getCreature(EntityId entityId) {
        Entity entity = entityData.getEntity(entityId, CreatureComponent.class, Owner.class, Position.class, Health.class);
        if (entity == null || entity.get(CreatureComponent.class) == null || entity.get(Owner.class) == null
                || entity.get(Position.class) == null || entity.get(Health.class) == null) {
            return null;
        }
        return entity;
    }

    private boolean revealsTraps(Entity creatureEntity) {
        Creature creature = kwdFile.getCreature(creatureEntity.get(CreatureComponent.class).creatureId);

        return creature != null && creature.getFlags().contains(Creature.CreatureFlag.REVEALS_ADJACENT_TRAPS)
                && canSetOffTraps(creatureEntity, false);
    }

    private boolean payForShot(Trap trap, short ownerId) {

        // DK II gives the good player unlimited mana (0x4B9EC0), so it never pays
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

    private EntityId findTarget(Entity trapEntity, Trap trap, double gameTime, float tpf) {
        if (trap.getTriggerType() == null) {
            return null;
        }

        short ownerId = trapEntity.get(Owner.class).ownerId;
        Point trapTile = WorldUtils.vectorToPoint(trapEntity.get(Position.class).position);
        return switch (trap.getTriggerType()) {
            case LINE_OF_SIGHT ->
                findTargetInSight(trapEntity, trap.getTriggerData(), gameTime, tpf);
            case PRESSURE ->
                findTargetOnTile(ownerId, trapTile, true);
            case TRIGGER ->
                findTargetOnTile(ownerId, trapTile, false);
            default ->
                null;
        };
    }

    /**
     * The nearest enemy creature in the trap's sight. Like DK II (0x4A9400),
     * the creature the trap aimed at on the tick before stays its target while
     * it is in sight and not beyond the range, the edge of the range included.
     * Another creature takes over only if it is strictly nearer, and strictly
     * inside the range.
     *
     * @param trapEntity the trap
     * @param range the trigger range in tiles, a new victim needs to be closer
     * @param gameTime the current game time
     * @param tpf the length of a tick
     * @return the victim, {@code null} if none is in sight
     */
    private EntityId findTargetInSight(Entity trapEntity, int range, double gameTime, float tpf) {
        Vector3f trapPosition = trapEntity.get(Position.class).position;
        Point trapTile = WorldUtils.vectorToPoint(trapPosition);
        short ownerId = trapEntity.get(Owner.class).ownerId;
        float closestDistanceSquared = range * range;
        EntityId target = null;
        TrapTarget last = entityData.getComponent(trapEntity.getId(), TrapTarget.class);
        if (last != null && gameTime > last.time && gameTime - last.time < tpf * 1.5f) {
            Entity kept = getCreature(last.target);
            if (kept != null && canSetOffTraps(kept, true)) {
                Vector3f keptPosition = kept.get(Position.class).position;
                float distanceSquared = distanceSquared(trapPosition, keptPosition);
                if (distanceSquared <= closestDistanceSquared && entityPositionLookup.hasLineOfSight(trapPosition, keptPosition)) {
                    closestDistanceSquared = distanceSquared;
                    target = last.target;
                }
            }
        }
        for (int x = trapTile.x - range; x <= trapTile.x + range; x++) {
            for (int y = trapTile.y - range; y <= trapTile.y + range; y++) {
                for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Entity creatureEntity = getCreature(entityId);
                    if (creatureEntity == null || !isEnemy(ownerId, creatureEntity.get(Owner.class).ownerId)
                            || !canSetOffTraps(creatureEntity, true)) {
                        continue;
                    }

                    Vector3f creaturePosition = creatureEntity.get(Position.class).position;
                    float distanceSquared = distanceSquared(trapPosition, creaturePosition);
                    if (distanceSquared < closestDistanceSquared
                            && entityPositionLookup.hasLineOfSight(trapPosition, creaturePosition)) {
                        closestDistanceSquared = distanceSquared;
                        target = entityId;
                    }
                }
            }
        }

        if (target != null) {
            entityData.setComponent(trapEntity.getId(), new TrapTarget(target, gameTime));
        } else if (last != null) {
            entityData.removeComponent(trapEntity.getId(), TrapTarget.class);
        }
        return target;
    }

    /**
     * The first creature the door finds on a tile it has a clear line to. The
     * door goes through the tiles row by row, not nearest first
     *
     * @param doorEntity the door
     * @param ownerId the door's owner
     * @return the victim, {@code null} if there is none
     */
    private EntityId findTargetAroundDoor(Entity doorEntity, short ownerId) {
        Vector3f doorPosition = doorEntity.get(Position.class).position;
        Point doorTile = WorldUtils.vectorToPoint(doorPosition);
        for (int y = doorTile.y - DOOR_TRAP_RANGE; y <= doorTile.y + DOOR_TRAP_RANGE; y++) {
            for (int x = doorTile.x - DOOR_TRAP_RANGE; x <= doorTile.x + DOOR_TRAP_RANGE; x++) {
                if (entityPositionLookup.hasLineOfSight(doorPosition, WorldUtils.pointToVector3f(x, y))) {
                    EntityId target = findTargetOnTile(ownerId, new Point(x, y), false);
                    if (target != null) {
                        return target;
                    }
                }
            }
        }

        return null;
    }

    /**
     * A creature standing on a tile. Any creature but the owner's and the
     * neutral player's will do
     *
     * @param ownerId the owner of the trap or door
     * @param tile the tile
     * @param checkCreatureType whether creature types that don't set off traps
     * are left out, trigger traps and doors don't care
     * @return the victim, {@code null} if nobody stands on the tile
     */
    private EntityId findTargetOnTile(short ownerId, Point tile, boolean checkCreatureType) {
        for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
            Entity creatureEntity = getCreature(entityId);
            if (creatureEntity == null) {
                continue;
            }
            short creatureOwnerId = creatureEntity.get(Owner.class).ownerId;
            if (creatureOwnerId != ownerId && creatureOwnerId != Player.NEUTRAL_PLAYER_ID
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
     * (dead, knocked out or in a keeper's hand) don't, nor do prisoners and
     * creatures being tortured, as in DK II (0x45A8B0, 0x4A9400). Neither does
     * a creature type with the data flag OpenKeeper calls {@code IS_HORNY}, the
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
                || entityData.getComponent(entityId, Death.class) != null
                || entityData.getComponent(entityId, CreatureImprisoned.class) != null
                || entityData.getComponent(entityId, CreatureTortured.class) != null) {
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

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        trapEntities.release();
        hiddenTrapEntities.release();
        triggerWaves.release();
        doorTrapEntities.release();
    }

}
