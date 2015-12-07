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
import java.awt.Point;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.DOUBLE_QUAD;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.HERO_GATE_2_BY_2;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.HERO_GATE_3_BY_1;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.HERO_GATE_FRONT_END;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.NORMAL;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction.QUAD;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction._3_BY_3;
import static toniarts.openkeeper.tools.convert.map.Room.TileConstruction._5_BY_5_ROTATED;
import toniarts.openkeeper.tools.convert.map.Thing;

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

    public static GenericRoom constructRoom(RoomInstance roomInstance, AssetManager assetManager, KwdFile kwdFile) {
        String roomName = roomInstance.getRoom().getName();
        switch (roomInstance.getRoom().getTileConstruction()) {
            case _3_BY_3:
                return new ThreeByThree(assetManager, roomInstance, null);

            case HERO_GATE_FRONT_END:
                return new HeroGateFrontEnd(assetManager, roomInstance, null);

            case HERO_GATE_2_BY_2:
                return new HeroGateTwoByTwo(assetManager, roomInstance, null);

            case HERO_GATE_3_BY_1:
                Thing.Room.Direction direction = Thing.Room.Direction.NORTH;
                for (Thing thing : kwdFile.getThings()) {
                    if (thing instanceof Thing.Room) {
                        Point p = new Point(((Thing.Room) thing).getPosX(), ((Thing.Room) thing).getPosY());
                        if (roomInstance.hasCoordinate(p)) {
                            direction = ((Thing.Room) thing).getDirection();
                        }
                    }
                }

                return new HeroGateThreeByOne(assetManager, roomInstance, direction);

            case _5_BY_5_ROTATED:
                return new FiveByFiveRotated(assetManager, roomInstance, null);

            case NORMAL:
                if (roomName.equalsIgnoreCase("Lair")) {
                    return new Lair(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Library")) {
                    return new Library(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Training Room")) {
                    return new TrainingRoom(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Work Shop")) {
                    return new Workshop(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Guard Room")) {
                    return new GuardRoom(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Casino")) {
                    return new Casino(assetManager, roomInstance, null);
                }
                if (roomName.equalsIgnoreCase("Graveyard")) {
                    return new Graveyard(assetManager, roomInstance, null);
                }
                return new Normal(assetManager, roomInstance, null);

            case QUAD:
                if (roomName.equalsIgnoreCase("Hero Stone Bridge") || roomName.equalsIgnoreCase("Stone Bridge")) {
                    return new StoneBridge(assetManager, roomInstance, null);
                } else {
                    return new WoodenBridge(assetManager, roomInstance, null);
                }

            case DOUBLE_QUAD:
                if (roomName.equalsIgnoreCase("Prison")) {
                    return new Prison(assetManager, roomInstance, null);
                } else if (roomName.equalsIgnoreCase("Combat Pit")) {
                    return new CombatPit(assetManager, roomInstance, null);
                } else if (roomName.equalsIgnoreCase("Temple")) {
                    return new Temple(assetManager, roomInstance, null);
                }
                // TODO use quad construction for different rooms
                // root.attachChild(DoubleQuad.construct(assetManager, roomInstance));
                break;

            default:

                // TODO
                logger.log(Level.WARNING, "Room {0} not exist", roomName);
        }
        return null;
    }
}
