/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityId;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IChickenGenerator;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles spawing chickens in hatcheries and coops
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ChickenSpawnSystem implements IGameLogicUpdatable {

    private final IObjectsController objectsController;
    private final int maximumFreerangeChickenCount;
    private final int entranceCooldownTime;
    private final Map<Short, IPlayerController> playerControllersById;
    private final SafeArrayList<IChickenGenerator> entrances = new SafeArrayList<>(IChickenGenerator.class);
    private final KwdFile kwdFile;

    public ChickenSpawnSystem(IObjectsController objectsController, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController) {
        this.objectsController = objectsController;

        // We need the game state just for the variables
        entranceCooldownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.CHICKEN_GENERATION_TIME_PER_HATCHERY).getValue() / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        maximumFreerangeChickenCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_FREE_RANGE_CHICKENS_PER_PLAYER).getValue();
        kwdFile = levelInfo.getLevelData();

        // Populate entrance list
        playerControllersById = new HashMap<>(playerControllers.size(), 1f);
        for (IPlayerController player : playerControllers) {
            playerControllersById.put(player.getKeeper().getId(), player);

            // Add initial rooms
            for (Entry<Room, Set<IRoomController>> keeperRooms : player.getRoomControl().getTypes().entrySet()) {

                // See that should we add
                for (IRoomController genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof IChickenGenerator)) {
                        break;
                    }
                    entrances.add((IChickenGenerator) genericRoom);
                }
            }

            // Add room listener to get notified of the changes
            // They should be quite rare vs the rate in which we iterate on each tick
            mapController.addListener(player.getKeeper().getId(), new EntranceListener(player.getKeeper().getId()));

            // TODO: Listen for the coops
        }
    }

    @Override
    public void processTick(float tpf, double gameTime) {
        for (IChickenGenerator entrance : entrances.getArray()) {
            evaluateAndSpawnCreature(entrance, gameTime);
        }
    }

    private void evaluateAndSpawnCreature(IChickenGenerator entrance, double gameTime) {
        double timeSinceLastSpawn = gameTime - entrance.getLastSpawnTime();
        IPlayerController player = playerControllersById.get(entrance.getRoomInstance().getOwnerId());
        boolean spawned = false;
        EntityId entityId = null;
        if (timeSinceLastSpawn >= entranceCooldownTime && !entrance.isFullCapacity()) {

            // Spawn chicken
            Point entranceCoordinate = entrance.getEntranceCoordinate();
            entityId = objectsController.spawnChicken(entrance.getRoomInstance().getOwnerId(), WorldUtils.pointToVector3f(entranceCoordinate));
            spawned = true;
        }

        if (spawned) {

            // Reset spawn time
            entrance.onSpawn(gameTime, entityId);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private class EntranceListener implements RoomListener {

        private final short playerId;

        public EntranceListener(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public void onBuild(IRoomController room) {
            addRoom(room);
        }

        @Override
        public void onCaptured(IRoomController room) {
            //addRoom(room);
        }

        @Override
        public void onCapturedByEnemy(IRoomController room) {
            //removeRoom(room);
        }

        @Override
        public void onSold(IRoomController room) {
            removeRoom(room);
        }

        private void addRoom(IRoomController room) {
            if (room instanceof IChickenGenerator) {
                entrances.add((IChickenGenerator) room);
            }
        }

        private void removeRoom(IRoomController room) {
            if (room instanceof IChickenGenerator) {
                entrances.remove((IChickenGenerator) room);
            }
        }

    }

}
