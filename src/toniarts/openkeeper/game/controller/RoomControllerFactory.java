/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.CasinoController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.DoubleQuadController;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.controller.room.HatcheryController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.NormalRoomController;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.game.controller.room.ThreeByThreeController;
import toniarts.openkeeper.game.controller.room.TreasuryController;
import toniarts.openkeeper.game.controller.room.WorkshopController;
import toniarts.openkeeper.tools.convert.map.Variable;

/**
 * A factory class you can use to build buildings
 *
 * @author ArchDemon
 */
public final class RoomControllerFactory {

    private static final Logger logger = Logger.getLogger(RoomControllerFactory.class.getName());

    private RoomControllerFactory() {
        // Nope
    }

    public static IRoomController constructRoom(RoomInstance roomInstance, IObjectsController objectsController,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings) {

        String roomName = roomInstance.getRoom().getName();

        switch (roomInstance.getRoom().getTileConstruction()) {
            case _3_BY_3:
                return new ThreeByThreeController(roomInstance, objectsController);

            case HERO_GATE:
            //return new HeroGateConstructor(assetManager, roomInstance);
            case HERO_GATE_FRONT_END:
            //return new HeroGateFrontEndConstructor(assetManager, roomInstance);
            case HERO_GATE_2_BY_2:
                //return new HeroGateTwoByTwoConstructor(assetManager, roomInstance);
                //        case HERO_GATE_3_BY_1:
                return new NormalRoomController(roomInstance, objectsController) {
                    @Override
                    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {
                        return false;
                    }

                };
            //return new HeroGateThreeByOneConstructor(assetManager, roomInstance);
            case _5_BY_5_ROTATED:
                return new FiveByFiveRotatedController(roomInstance, objectsController, gameSettings);

            case NORMAL:
//                if (roomName.equalsIgnoreCase("Lair")) {
//                    return new Lair(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                } else if (roomName.equalsIgnoreCase("Library")) {
//                    return new Library(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                } else if (roomName.equalsIgnoreCase("Training Room")) {
//                    return new TrainingRoom(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                } else
                if (roomName.equalsIgnoreCase("Work Shop")) {
                    return new WorkshopController(roomInstance, objectsController);
//                } else if (roomName.equalsIgnoreCase("Guard Room")) {
//                    return new GuardRoom(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Casino")) {
                    return new CasinoController(roomInstance, objectsController);
//                } else if (roomName.equalsIgnoreCase("Graveyard")) {
//                    return new Graveyard(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                } else if (roomName.equalsIgnoreCase("Torture Chamber")) {
//                    return new TortureChamber(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Treasury")) {
                    return new TreasuryController(roomInstance, objectsController, gameSettings);
                } else if (roomName.equalsIgnoreCase("Hatchery")) {
                    return new HatcheryController(roomInstance, objectsController);
                }
                return new NormalRoomController(roomInstance, objectsController);

//            case QUAD:
//                if (roomName.equalsIgnoreCase("Hero Stone Bridge") || roomName.equalsIgnoreCase("Stone Bridge")) {
//                    return new StoneBridge(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                }
            // return new QuadConstructor(assetManager, roomInstance);
//
            case DOUBLE_QUAD:
                if (roomName.equalsIgnoreCase("Prison")) {
                    return new PrisonController(roomInstance, objectsController);
                } else if (roomName.equalsIgnoreCase("Combat Pit")) {
                    return new CombatPitController(roomInstance, objectsController);
                }
//                } else if (roomName.equalsIgnoreCase("Temple")) {
//                    return new Temple(assetManager, roomInstance, objectLoader, worldState, effectManager);
//                }
//                // TODO use quad construction for different rooms
                return new DoubleQuadController(roomInstance, objectsController);
            default:

                // TODO
                logger.log(Level.WARNING, "Room {0} not exist", roomName);
                return new NormalRoomController(roomInstance, objectsController);
        }
    }
}
