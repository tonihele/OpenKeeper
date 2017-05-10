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
package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import java.util.logging.Level;
import java.util.logging.Logger;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.*;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * A factory class you can use to build buildings
 *
 * @author ArchDemon
 */
public final class RoomConstructor {

    private static final Logger logger = Logger.getLogger(RoomConstructor.class.getName());

    private RoomConstructor() {
        // Nope
    }

    public static GenericRoom constructRoom(RoomInstance roomInstance, AssetManager assetManager,
            EffectManagerState effectManager, WorldState worldState, ObjectLoader objectLoader) {

        String roomName = roomInstance.getRoom().getName();

        switch (roomInstance.getRoom().getTileConstruction()) {
            case _3_BY_3:
                return new ThreeByThree(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case HERO_GATE:
                return new HeroGate(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case HERO_GATE_FRONT_END:
                return new HeroGateFrontEnd(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case HERO_GATE_2_BY_2:
                return new HeroGateTwoByTwo(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case HERO_GATE_3_BY_1:
                return new HeroGateThreeByOne(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case _5_BY_5_ROTATED:
                return new FiveByFiveRotated(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case NORMAL:
                if (roomName.equalsIgnoreCase("Lair")) {
                    return new Lair(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Library")) {
                    return new Library(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Training Room")) {
                    return new TrainingRoom(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Work Shop")) {
                    return new Workshop(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Guard Room")) {
                    return new GuardRoom(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Casino")) {
                    return new Casino(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Graveyard")) {
                    return new Graveyard(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Torture Chamber")) {
                    return new TortureChamber(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Treasury")) {
                    return new Treasury(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Hatchery")) {
                    return new Hatchery(assetManager, roomInstance, objectLoader, worldState, effectManager);
                }
                return new Normal(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case QUAD:
                if (roomName.equalsIgnoreCase("Hero Stone Bridge") || roomName.equalsIgnoreCase("Stone Bridge")) {
                    return new StoneBridge(assetManager, roomInstance, objectLoader, worldState, effectManager);
                }
                return new Quad(assetManager, roomInstance, objectLoader, worldState, effectManager);

            case DOUBLE_QUAD:
                if (roomName.equalsIgnoreCase("Prison")) {
                    return new Prison(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Combat Pit")) {
                    return new CombatPit(assetManager, roomInstance, objectLoader, worldState, effectManager);
                } else if (roomName.equalsIgnoreCase("Temple")) {
                    return new Temple(assetManager, roomInstance, objectLoader, worldState, effectManager);
                }
                // TODO use quad construction for different rooms
                return new DoubleQuad(assetManager, roomInstance, objectLoader, worldState, effectManager);

            default:

                // TODO
                logger.log(Level.WARNING, "Room {0} not exist", roomName);
        }
        return null;
    }
}
