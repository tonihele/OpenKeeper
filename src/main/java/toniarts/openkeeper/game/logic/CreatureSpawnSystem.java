/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalAttracted;
import toniarts.openkeeper.game.component.PortalCreatureLimit;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Handles creatures spawning, from Portals, Dungeon Hearts...<br>
 * Each Portal keeps its own generation clock and generated-since-capture count.
 * Portals with equal clocks can therefore attract creatures on the same tick.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureSpawnSystem extends GameTimeCounter {

    /**
     * A hero portal the keeper has captured, the "Mercenary Portal", counts as a portal for the creature limit
     */
    public static final short HERO_PORTAL_ROOM_ID = 25;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final GameEventService gameEventService;
    private final int minimumImpCount;
    private final int entranceCooldownTime;
    private final int initialPortalCapacity;
    private final int additionalPortalCapacity;
    private final int freeImpCoolDownTime;
    private final Map<Short, IPlayerController> playerControllersById;
    private final SafeArrayList<ICreatureEntrance> entrances = new SafeArrayList<>(ICreatureEntrance.class);
    private final IKwdFile kwdFile;
    private final PortalCreatureChoice creatureChoice;
    private boolean uniqueLayouts = false;
    private final Map<Short, EntityId> portalLimitEntities = new HashMap<>();
    private final EntitySet attractedCreatures;

    public CreatureSpawnSystem(EntityData entityData, ICreaturesController creaturesController, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController, GameEventService gameEventService) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.creatureChoice = new PortalCreatureChoice(entityData, Utils.getRandom());
        this.gameEventService = gameEventService;

        // We need the game state just for the variables
        entranceCooldownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.ENTRANCE_GENERATION_SPEED_SECONDS).getValue();
        minimumImpCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD).getValue();
        initialPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL).getValue();
        additionalPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL).getValue();
        freeImpCoolDownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS).getValue();
        kwdFile = levelInfo.getLevelData();

        // The living attracted creatures, to notice when they die, leave or change sides
        attractedCreatures = entityData.getEntities(Owner.class, PortalAttracted.class, Health.class);

        // Populate entrance list
        playerControllersById = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController player : playerControllers) {
            playerControllersById.put(player.getKeeper().getId(), player);

            // The keeper's own entity for the portal numbers, for the limit and the tooltip
            short keeperId = player.getKeeper().getId();
            EntityId limitEntity = entityData.createEntity();
            entityData.setComponents(limitEntity, new Owner(keeperId, keeperId), new PortalCreatureLimit());
            portalLimitEntities.put(keeperId, limitEntity);

            // Add initial rooms
            for (Entry<Room, Set<IRoomController>> keeperRooms : player.getRoomControl().getRoomControllers().entrySet()) {

                // See that should we add
                for (IRoomController genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof ICreatureEntrance)) {
                        break;
                    }
                    entrances.add((ICreatureEntrance) genericRoom);
                }
            }

            // Add room listener to get notified of the changes
            // They should be quite rare vs the rate in which we iterate on each tick
            mapController.addListener(player.getKeeper().getId(), new EntranceListener(player.getKeeper().getId()));
            updatePortalLimit(player);
        }
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        for (ICreatureEntrance entrance : entrances.getArray()) {
            evaluateAndSpawnCreature(entrance);
        }

        // After the spawns, so that a new creature is in the set before it can die
        if (attractedCreatures.applyChanges()) {
            for (IPlayerController player : playerControllersById.values()) {
                updatePortalLimit(player);
            }
        }
    }

    /** Dispatches each live entrance to its independent spawn policy. */
    private void evaluateAndSpawnCreature(ICreatureEntrance entrance) {
        // TODO: listen for destruction so the entrance can be removed from the set.
        if (entrance.isDestroyed()) {
            return;
        }
        double timeSinceLastSpawn = timeElapsed - entrance.getLastSpawnTime();
        IPlayerController player = playerControllersById.get(entrance.getOwnerId());
        if (entrance.isDungeonHeart()) {
            if (timeSinceLastSpawn >= freeImpCoolDownTime
                    && player.getCreatureControl().getImpCount() < minimumImpCount
                    && !player.getKeeper().isDestroyed()) {
                spawnFreeImp(entrance, player);
            }
        } else {
            evaluatePortalSpawn(entrance, player, timeSinceLastSpawn);
        }
    }

    /** Creates a Heart's free Imp and announces it before restarting its cooldown. */
    private void spawnFreeImp(ICreatureEntrance entrance, IPlayerController player) {
        Point coordinate = entrance.getEntranceCoordinate();
        Vector2f spot = new Vector2f(coordinate.x, coordinate.y);
        EntityId entityId = creaturesController.spawnCreature(kwdFile.getImp().getCreatureId(),
                player.getKeeper().getId(), 1, spot, ICreaturesController.SpawnType.PLACE);
        gameEventService.emit(GameEventType.IMPS_CREATED, WorldUtils.vector2fToVector3(spot));
        entrance.onSpawn(timeElapsed, entityId);
    }

    /** Advances due Portal cycles even when attraction is closed or no creature qualifies. */
    private void evaluatePortalSpawn(ICreatureEntrance entrance, IPlayerController player,
            double timeSinceLastSpawn) {
        double cooldown = entranceCooldownTime
                + (double) entrance.getGeneratedCount() * entrance.getGeneratedCount();
        if (timeSinceLastSpawn < cooldown) {
            preparePortalSpawn(entrance, player, cooldown);
            return;
        }
        entrance.onGenerationCycle(timeElapsed);
        if (!player.getRoomControl().isPortalsOpen() || isCreatureLimitReached(player)) {
            return;
        }
        Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
        Creature creature = uniqueLayouts
                ? creatureChoice.chooseUniqueCreature(player, playerControllersById.values(), creaturesController, kwdFile)
                : null;
        if (creature == null) {
            creature = creatureChoice.chooseCreature(player, entrance, creaturesController,
                    type -> isCreatureAvailableFromPool(type, player, pool) && isCreatureRequirementsSatisfied(type, player));
        }
        if (creature == null) {
            return;
        }
        short creatureId = creature.getCreatureId();
        Point coordinate = entrance.getEntranceCoordinate();
        EntityId entityId = creaturesController.spawnCreature(creatureId,
                player.getKeeper().getId(), 1, new Vector2f(coordinate.x, coordinate.y),
                ICreaturesController.SpawnType.ENTRANCE);
        if (entityId != null) {
            entityData.setComponent(entityId, new PortalAttracted());
            updatePortalLimit(player);
            entrance.onSpawn(timeElapsed, entityId);
        }
    }

    /** Starts the authored preparation three seconds before an eligible Portal cycle. */
    private void preparePortalSpawn(ICreatureEntrance entrance, IPlayerController player, double cooldown) {
        double preparationTime = entrance.getLastSpawnTime() + cooldown - 3;
        if (timeElapsed >= preparationTime && !isCreatureLimitReached(player)
                && canPreparePortalSpawn(entrance, player)) {
            if (entrance.onPrepareSpawn(preparationTime)) {
                gameEventService.emitAt(GameEventType.PORTAL_PREPARING, GameEvent.ThingFlag.ROOM,
                        entrance.getRoom().getRoomId(), entrance.getEntityId(),
                        WorldUtils.pointToVector3f(entrance.getEntranceCoordinate()));
            }
        }
    }

    private boolean canPreparePortalSpawn(ICreatureEntrance entrance, IPlayerController player) {
        if (uniqueLayouts && creatureChoice.canChooseUniqueCreature(player,
                playerControllersById.values(), creaturesController, kwdFile)) {
            return true;
        }
        Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
        return creatureChoice.canChooseCreature(player, entrance, creaturesController,
                type -> isCreatureAvailableFromPool(type, player, pool) && isCreatureRequirementsSatisfied(type, player));
    }

    private boolean isCreatureLimitReached(IPlayerController player) {
        PortalCreatureLimit limit = entityData.getComponent(portalLimitEntities.get(player.getKeeper().getId()), PortalCreatureLimit.class);
        return limit.attracted >= limit.capacity;
    }

    private int getPortalCapacity(IPlayerController player) {
        int portals = player.getRoomControl().getTypeCount(kwdFile.getPortal());
        Room heroPortal = kwdFile.getRoomById(HERO_PORTAL_ROOM_ID);
        if (heroPortal != null) {
            portals += player.getRoomControl().getTypeCount(heroPortal);
        }
        return getPortalCapacity(portals, initialPortalCapacity, additionalPortalCapacity);
    }

    /**
     * Update the keeper's portal numbers when something changes them, only touching the entity when they change
     */
    private void updatePortalLimit(IPlayerController player) {
        short keeperId = player.getKeeper().getId();
        EntityId limitEntity = portalLimitEntities.get(keeperId);
        PortalCreatureLimit current = entityData.getComponent(limitEntity, PortalCreatureLimit.class);
        int attracted = getAttractedCreatureCount(entityData, keeperId);
        int capacity = getPortalCapacity(player);
        if (current == null || current.attracted != attracted || current.capacity != capacity) {
            entityData.setComponent(limitEntity, new PortalCreatureLimit(attracted, capacity));
        }
    }

    /**
     * Get the number of creatures the portals can attract. As in the original, the first portal supports
     * CREATURES_SUPPORTED_BY_FIRST_PORTAL creatures and each further portal
     * CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL more.
     *
     * @param portals the number of portals the keeper has, captured hero portals included
     * @param firstPortal the creatures the first portal supports
     * @param perAdditionalPortal the creatures each further portal supports
     * @return the number of creatures the portals can attract
     */
    public static int getPortalCapacity(int portals, int firstPortal, int perAdditionalPortal) {
        if (portals <= 0) {
            return 0;
        }
        return firstPortal + (portals - 1) * perAdditionalPortal;
    }

    /**
     * Get the number of the keeper's living creatures that came in through a portal. As in the original, the
     * creatures the keeper got otherwise, like the ones in the level from the start, don't count towards the
     * portal limit.
     *
     * @param entityData the entities
     * @param playerId the keeper
     * @return the number of attracted creatures
     */
    public static int getAttractedCreatureCount(EntityData entityData, short playerId) {
        int count = 0;
        for (EntityId creature : entityData.findEntities(new FieldFilter(Owner.class, "ownerId", playerId),
                CreatureComponent.class, Owner.class, PortalAttracted.class)) {
            if (entityData.getComponent(creature, Death.class) == null) {
                count++;
            }
        }
        return count;
    }

    private boolean isCreatureRequirementsSatisfied(Creature creature, IPlayerController player) {
        return CreatureAttraction.isSatisfied(creature, player, kwdFile);
    }

    private static boolean isCreatureAvailableFromPool(Creature creature, IPlayerController player, Map<Integer, CreaturePool> pool) {
        CreaturePool creaturePool = pool.get(Short.valueOf(creature.getCreatureId()).intValue());
        if (creaturePool != null) {
            int creatures = player.getCreatureControl().getTypeCount(creature);
            return creaturePool.getValue() > creatures;
        }
        return false;
    }

    @Override
    public void start() {
        uniqueLayouts = PortalCreatureChoice.isUniqueLayoutLevel(kwdFile.getGameLevel().getLvlFlags());

        // The creature types the keepers can attract from the start are not favoured later
        for (IPlayerController player : playerControllersById.values()) {
            if (!isCreatureLimitReached(player)) {
                Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
                creatureChoice.start(player, creaturesController,
                        type -> isCreatureAvailableFromPool(type, player, pool) && isCreatureRequirementsSatisfied(type, player));
            }
        }
    }

    @Override
    public void stop() {
        attractedCreatures.release();
    }

    /**
     * In DK 2 it is not possible to place spawn points in game, but but we don't know that
     */
    private final class EntranceListener implements RoomListener {

        private final short playerId;

        public EntranceListener(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public void onBuild(IRoomController room) {
            addRoom(room);
            updatePortalLimit(playerControllersById.get(playerId));
        }

        @Override
        public void onCaptured(IRoomController room) {
            //addRoom(room);
            updatePortalLimit(playerControllersById.get(playerId));
        }

        @Override
        public void onCapturedByEnemy(IRoomController room) {
            //removeRoom(room);
            updatePortalLimit(playerControllersById.get(playerId));
        }

        @Override
        public void onSold(IRoomController room) {
            removeRoom(room);
            updatePortalLimit(playerControllersById.get(playerId));
        }

        private void addRoom(IRoomController room) {
            if (room instanceof ICreatureEntrance iCreatureEntrance) {
                entrances.add(iCreatureEntrance);
            }
        }

        private void removeRoom(IRoomController room) {
            if (room instanceof ICreatureEntrance iCreatureEntrance) {
                entrances.remove(iCreatureEntrance);
            }
        }

    }

}
