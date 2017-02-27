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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.Attraction;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.room.GenericRoom;
import toniarts.openkeeper.world.room.ICreatureEntrance;

/**
 * Handles creatures spawning, from Portals, Dungeon Hearts...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureSpawnLogicState extends AbstractAppState implements IGameLogicUpdateable {

    private final ThingLoader thingLoader;
    private final int minimunImpCount;
    private final int entranceCoolDownTime;
    private final int initialPortalCapacity;
    private final int additionalPortalCapacity;
    private final int freeImpCoolDownTime;
    private final Map<Keeper, Map<GenericRoom, Float>> entranceSpawnTimes;
    private final KwdFile kwdFile;

    public CreatureSpawnLogicState(ThingLoader thingLoader, Collection<Keeper> players, GameState gameState) {
        this.thingLoader = thingLoader;

        // We need the game state just for the variables
        entranceCoolDownTime = (int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.ENTRANCE_GENERATION_SPEED_SECONDS);
        minimunImpCount = (int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD);
        initialPortalCapacity = (int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL);
        additionalPortalCapacity = (int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL);
        freeImpCoolDownTime = (int) gameState.getLevelVariable(Variable.MiscVariable.MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS);
        kwdFile = gameState.getLevelData();

        // Create entrance counters for all players
        entranceSpawnTimes = new HashMap<>(players.size());
        for (Keeper player : players) {
            entranceSpawnTimes.put(player, new HashMap<>());
        }
    }

    @Override
    public void processTick(float tpf, Application app) {

        // FIXME: really should use the listeners, I think this just produces unnecessary garbage
        // Maintain the players entrance registry
        for (Entry<Keeper, Map<GenericRoom, Float>> keeperRoomTimes : entranceSpawnTimes.entrySet()) {

            // Remove the obsolete ones & advance timers
            Iterator<Entry<GenericRoom, Float>> roomTimesIter = keeperRoomTimes.getValue().entrySet().iterator();
            while (roomTimesIter.hasNext()) {
                Entry<GenericRoom, Float> entry = roomTimesIter.next();
                if (entry.getKey().isDestroyed() || entry.getKey().getRoomInstance().getOwnerId() != keeperRoomTimes.getKey().getId()) {
                    roomTimesIter.remove();
                } else {
                    // TODO: Should we advance the timer if the entrances are not open?
                    if (entry.getValue() + tpf <= Float.MAX_VALUE) {
                        entry.setValue(entry.getValue() + tpf);
                    }

                    evaluateAndSpawnCreature(keeperRoomTimes.getKey(), entry.getKey(), app);
                }
            }

            // Add new ones
            for (Entry<Room, Set<GenericRoom>> keeperRooms : keeperRoomTimes.getKey().getRoomControl().getTypes().entrySet()) {

                // See that should we add
                for (GenericRoom genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof ICreatureEntrance)) {
                        break;
                    }

                    if (!keeperRoomTimes.getValue().containsKey(genericRoom)) {
                        keeperRoomTimes.getValue().put(genericRoom, Float.MAX_VALUE);

                        evaluateAndSpawnCreature(keeperRoomTimes.getKey(), genericRoom, app);
                    }
                }
            }
        }

    }

    private void evaluateAndSpawnCreature(Keeper player, GenericRoom room, Application app) {
        float spawnTime = entranceSpawnTimes.get(player).get(room);
        boolean spawned = false;
        if (spawnTime >= freeImpCoolDownTime && isDungeonHeart(room)) {
            if (player.getCreatureControl().getImpCount() < minimunImpCount) {

                // Spawn imp
                spawnCreature(kwdFile.getImp().getCreatureId(), player.getId(), (short) 1, app, thingLoader, ((ICreatureEntrance) room).getEntranceCoordinate(), false);
                spawned = true;
            }
        } else if (spawnTime >= Math.max(entranceCoolDownTime, entranceCoolDownTime * player.getCreatureControl().getTypeCount() * 0.5) && player.getRoomControl().isPortalsOpen() && !isCreatureLimitReached(player)) {

            // Evaluate what creature can we spawn
            Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getId());
            List<Creature> possibleCreatures = new ArrayList<>(player.getCreatureControl().getTypesAvailable());
            Iterator<Creature> iter = possibleCreatures.iterator();
            while (iter.hasNext()) {
                Creature creature = iter.next();
                if (!isCreatureAvailableFromPool(creature, player, pool) || !isCreatureRequirementsSatisfied(creature, player)) {
                    iter.remove();
                }
            }

            // Spawn random?
            if (!possibleCreatures.isEmpty()) {
                spawnCreature(Utils.getRandomItem(possibleCreatures).getCreatureId(), player.getId(), (short) 1, app, thingLoader, ((ICreatureEntrance) room).getEntranceCoordinate(), true);
                spawned = true;
            }
        }

        if (spawned) {

            // Reset spawn time
            entranceSpawnTimes.get(player).put(room, 0f);
        }
    }

    private boolean isDungeonHeart(GenericRoom room) {
        return room.isDungeonHeart();
    }

    public static void spawnCreature(short creatureId, short playerId, short level,
            Application app, ThingLoader thingLoader, Point tile, boolean entrance) {

        // Spawn the creature
        thingLoader.spawnCreature(creatureId, playerId, level, WorldUtils.pointToVector2f(tile), entrance, app);
    }

    private boolean isCreatureLimitReached(Keeper player) {
        return player.getCreatureControl().getTypeCount() >= (initialPortalCapacity + (player.getRoomControl().getTypeCount(kwdFile.getPortal()) - 1) * additionalPortalCapacity);
    }

    private boolean isCreatureRequirementsSatisfied(Creature creature, Keeper player) {
        for (Attraction attraction : creature.getAttractions()) {
            short roomId = (short) attraction.getRoomId();
            if (roomId > 0) {
                Room room = kwdFile.getRoomById(roomId);
                if (player.getRoomControl().getTypeCount(room) > 0) {

                    // Ok, we have these, see sizes, I recon we really need a room that size, not summed up tiles
                    if (attraction.getRoomSize() > 0) {
                        boolean roomFound = false;
                        for (GenericRoom genericRoom : new ArrayList<>(player.getRoomControl().getTypes().get(room))) {
                            if (attraction.getRoomSize() <= genericRoom.getRoomInstance().getCoordinates().size()) {
                                roomFound = true;
                                break; // Ok
                            }
                        }
                        if (roomFound) {
                            continue;
                        }
                    } else {
                        continue; // We have the room
                    }

                    return false;
                }

                return false;
            }
        }
        return true;
    }

    private boolean isCreatureAvailableFromPool(Creature creature, Keeper player, Map<Integer, CreaturePool> pool) {
        CreaturePool creaturePool = pool.get(Short.valueOf(creature.getCreatureId()).intValue());
        if (creaturePool != null) {
            int creatures = player.getCreatureControl().getTypeCount(creature);
            return creaturePool.getValue() > creatures;
        }
        return false;
    }

}
