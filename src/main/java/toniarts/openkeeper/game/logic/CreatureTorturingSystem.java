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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.KeeperInformation;
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
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creatures torturing. When enough... persuasion has been received... join the player army
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTorturingSystem extends GameTimeCounter {

    /**
     * What a torture victim can tell about its keeper. The numbers are DK
     * II's. OpenKeeper has no panel that shows the keeper information, the
     * victim only tells it once.
     */
    private static final int INFO_ARTISAN = 3;
    private static final int INFO_THINKER = 4;
    private static final int INFO_FIGHTER = 5;
    private static final int INFO_ARTISAN_2 = 7;
    private static final int INFO_FIGHTER_2 = 8;
    private static final int INFO_THINKER_2 = 9;
    private static final int INFO_ANY = 10;
    private static final int INFO_ROOM = 11;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final IMapController mapController;
    private final GameEventService gameEventService;
    private final EntitySet torturedEntities;
    private final EntitySet keeperInformation;
    private final Random random;
    private final Map<EntityId, IRoomController> chambersByVictim = new HashMap<>();

    public CreatureTorturingSystem(EntityData entityData, ICreaturesController creaturesController, IMapController mapController,
            GameEventService gameEventService) {
        this(entityData, creaturesController, mapController, gameEventService, new Random());
    }

    CreatureTorturingSystem(EntityData entityData, ICreaturesController creaturesController, IMapController mapController,
            GameEventService gameEventService, Random random) {
        this.random = random;
        this.entityData = entityData;
        this.gameEventService = gameEventService;
        this.creaturesController = creaturesController;
        this.mapController = mapController;

        // Have the position also here, since the player may move tortured entities between torture rooms, kinda still tortured but not counting towards death at the time
        torturedEntities = entityData.getEntities(CreatureTortured.class, CreatureComponent.class, Position.class, Owner.class);
        keeperInformation = entityData.getEntities(KeeperInformation.class);
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

            // Like in DK II, the torture works once a second
            if ((int) (creatureTortured.timeTortured + tpf) > (int) creatureTortured.timeTortured) {
                tortureForASecond(entity.getId(), owner.ownerId == playerId);
            }

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
     * A second of torture. Like in DK II, the keeper's own victim gets its
     * torture mood change, which a Mistress enjoys. The health change is done
     * by the health system.
     */
    private void tortureForASecond(EntityId victim, boolean ownCreature) {
        if (!ownCreature) {
            return;
        }
        Creature creature = creaturesController.createController(victim).getCreature();
        CreatureMood mood = entityData.getComponent(victim, CreatureMood.class);
        if (creature == null || mood == null || creature.getAttributes().getTortureMoodChange() == 0
                || (creature.getFlags2() != null && creature.getFlags2().contains(Creature.CreatureFlag2.IS_EMOTIONLESS))) {
            return;
        }
        entityData.setComponent(victim, mood.add(CreatureMood.REASON_OTHER, creature.getAttributes().getTortureMoodChange()));
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

            // An enemy tortured to death tells something about its keeper
            Owner owner = entityData.getComponent(victim, Owner.class);
            short playerId = chamber.getRoomInstance().getOwnerId();
            CreatureComponent creatureComponent = entityData.getComponent(victim, CreatureComponent.class);
            if (entityData.getComponent(victim, Death.class) != null && owner != null && creatureComponent != null
                    && owner.ownerId != playerId) {
                revealInformation(playerId, owner.ownerId, creaturesController.createController(victim).getCreature());
            }

            if (!chamber.getRoomInstance().isDestroyed()
                    || entityData.getComponent(victim, Death.class) != null
                    || entityData.getComponent(victim, InHand.class) != null
                    || entityData.getComponent(victim, CreatureComponent.class) == null) {
                continue;
            }

            // Set free
            entityData.setComponent(victim, new Owner(owner.ownerId, owner.ownerId));
            creaturesController.createController(victim).getStateMachine().changeState(CreatureState.IDLE);
        }
    }

    /**
     * Like in DK II, the victim tells one thing its keeper's torturer does not
     * know yet, picked at random. Depending on its job, a creature knows two
     * things about its keeper, any creature one more, and it can always tell
     * where one of the keeper's rooms is. OpenKeeper reveals the room, a
     * room not revealed this way yet. What the torturer knows is kept in a
     * KeeperInformation entity.
     *
     * @param playerId the torturer
     * @param victimOwnerId the keeper of the victim
     * @param creature the victim
     */
    private void revealInformation(short playerId, short victimOwnerId, Creature creature) {
        Entity entity = getKeeperInformation(playerId, victimOwnerId);
        KeeperInformation known = entity.get(KeeperInformation.class);
        List<Integer> info = new ArrayList<>(4);
        for (int item : getInformation(creature)) {
            if (!known.told.contains(item)) {
                info.add(item);
            }
        }
        info.add(INFO_ROOM);

        int item = info.get(random.nextInt(info.size()));
        if (item != INFO_ROOM) {
            Set<Integer> told = new HashSet<>(known.told);
            told.add(item);
            entity.set(new KeeperInformation(playerId, victimOwnerId, told, known.roomsRevealed));
            return;
        }

        for (IRoomController room : mapController.getRoomControllers()) {
            RoomInstance roomInstance = room.getRoomInstance();
            if (roomInstance.getOwnerId() == victimOwnerId && !roomInstance.isDestroyed()
                    && !known.roomsRevealed.contains(room.getEntityId())) {
                Set<EntityId> roomsRevealed = new HashSet<>(known.roomsRevealed);
                roomsRevealed.add(room.getEntityId());
                entity.set(new KeeperInformation(playerId, victimOwnerId, known.told, roomsRevealed));
                mapController.revealTiles(new ArrayList<Point>(roomInstance.getCoordinates()), playerId);
                return;
            }
        }
    }

    private Entity getKeeperInformation(short playerId, short keeperId) {
        keeperInformation.applyChanges();
        for (Entity entity : keeperInformation) {
            KeeperInformation known = entity.get(KeeperInformation.class);
            if (known.playerId == playerId && known.keeperId == keeperId) {
                return entity;
            }
        }

        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new KeeperInformation(playerId, keeperId, Set.of(), Set.of()));
        keeperInformation.applyChanges();
        return keeperInformation.getEntity(entityId);
    }

    private static int[] getInformation(Creature creature) {
        Creature.JobClass jobClass = (creature != null ? creature.getJobClass() : null);
        if (jobClass == null) {
            return new int[]{INFO_ANY};
        }
        return switch (jobClass) {
            case ARTISAN ->
                new int[]{INFO_ARTISAN, INFO_ARTISAN_2, INFO_ANY};
            case THINKER ->
                new int[]{INFO_THINKER, INFO_THINKER_2, INFO_ANY};
            case FIGHTER ->
                new int[]{INFO_FIGHTER, INFO_FIGHTER_2, INFO_ANY};
            default ->
                new int[]{INFO_ANY};
        };
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
        keeperInformation.release();
        chambersByVictim.clear();
    }

}
