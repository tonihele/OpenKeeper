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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creatures torturing. When enough... persuasion has been received... join the player army
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTorturingSystem extends GameTimeCounter {

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IMapController mapController;
    private final GameEventService gameEventService;
    private final EntitySet torturedEntities;
    private final Map<EntityId, IRoomController> chambersByVictim = new HashMap<>();

    public CreatureTorturingSystem(EntityData entityData, ICreaturesController creaturesController, IMapController mapController,
            GameEventService gameEventService) {
        this.entityData = entityData;
        this.gameEventService = gameEventService;
        this.creaturesController = creaturesController;
        this.mapController = mapController;

        // Have the position also here, since the player may move tortured entities between torture rooms, kinda still tortured but not counting towards death at the time
        torturedEntities = entityData.getEntities(CreatureTortured.class, CreatureComponent.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);
        // Add new & remove old
        if (torturedEntities.applyChanges()) {
            processRemovedEntities(torturedEntities.getRemovedEntities());
        }

        // Process ticks
        for (Entity entity : torturedEntities) {
            // Keep track of the chamber on every tick, the room may be gone before the next check
            IRoomController chamber = getChamber(entity);
            CreatureTortured creatureTortured = entity.get(CreatureTortured.class);
            if (chamber != null) {
                playTortureSounds(entity, creatureTortured.device);
            }
            if (creatureTortured.tortureCheckTime >= timeElapsed || chamber == null) {
                continue;
            }

            CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
            Owner owner = entity.get(Owner.class);
            short playerId = chamber.getRoomInstance().getOwnerId();

            if (creatureTortured.timeTortured + tpf >= creatureComponent.tortureTimeToConvert) {
                if (owner.ownerId != playerId) {

                    // Convert!
                    entityData.removeComponent(entity.getId(), CreatureTortured.class);
                    leaveDevice(entity.getId());
                    creaturesController.createController(entity.getId()).convertCreature(playerId);

                    // The mentor tells both keepers, like DK II (CRoom_sub_4F2800)
                    gameEventService.emitToPlayer(GameEventType.TORTURE_CONVERTED_BY_ENEMY, owner.ownerId);
                    gameEventService.emitToPlayer(GameEventType.TORTURE_CONVERTED, playerId);
                    continue;
                }
            }

            entity.set(new CreatureTortured(creatureTortured.timeTortured + tpf, timeElapsed, creatureTortured.healthCheckTime, creatureTortured.device));
        }
    }

    /**
     * A victim on a torture device is heard like in DK II (CreatureInstance_cpp_499300): each shock of the
     * Electric Chair zaps, on the same tick as the victim turns into its skeleton, and on the other devices the
     * victim cries out all the time
     */
    private void playTortureSounds(Entity victim, EntityId device) {
        ObjectComponent deviceObject = device != null ? entityData.getComponent(device, ObjectComponent.class) : null;
        if (deviceObject == null) {
            return;
        }
        if (deviceObject.objectId != CreatureViewSystem.ELECTRIC_CHAIR_OBJECT_ID) {
            gameEventService.emit(GameEventType.TORTURE_WHEEL, victim.getId());
            return;
        }
        CreatureViewState view = entityData.getComponent(victim.getId(), CreatureViewState.class);
        if (view != null && CreatureViewSystem.isShockDue(view, timeElapsed)) {
            gameEventService.emitAt(GameEventType.TORTURE_ELECTROCUTED, GameEvent.ThingFlag.OBJECT, deviceObject.objectId,
                    victim.getId(), victim.get(Position.class).position);
        }
    }

    private IRoomController getChamber(Entity entity) {
        IRoomController chamber = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(entity.get(Position.class).position));
        if (chamber == null || !chamber.hasObjectControl(AbstractRoomController.ObjectType.TORTUREE)) {
            return null;
        }
        chambersByVictim.put(entity.getId(), chamber);
        return chamber;
    }

    /**
     * The victim is no longer tortured: it died, was picked up, or the chamber
     * was sold or destroyed, which sets its victims free
     */
    private void processRemovedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            EntityId victim = entity.getId();
            IRoomController chamber = chambersByVictim.get(victim);
            if (chamber == null) {
                continue;
            }
            leaveDevice(victim);

            if (!chamber.getRoomInstance().isDestroyed()
                    || entityData.getComponent(victim, Death.class) != null
                    || entityData.getComponent(victim, InHand.class) != null
                    || entityData.getComponent(victim, CreatureComponent.class) == null) {
                continue;
            }

            // Set free
            Owner owner = entityData.getComponent(victim, Owner.class);
            entityData.setComponent(victim, new Owner(owner.ownerId, owner.ownerId));
            creaturesController.createController(victim).getStateMachine().changeState(CreatureState.IDLE);
        }
    }

    private void leaveDevice(EntityId victim) {
        IRoomController chamber = chambersByVictim.remove(victim);
        if (chamber != null) {
            chamber.getObjectControl(AbstractRoomController.ObjectType.TORTUREE).removeItem(victim);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        torturedEntities.release();
        chambersByVictim.clear();
    }

}
