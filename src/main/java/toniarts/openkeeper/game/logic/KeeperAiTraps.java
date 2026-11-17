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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.KeeperAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * How the computer player places traps and doors: in the corridors on the
 * edge of a box round its rooms, like a human player does with the trap and
 * door buttons.
 */
final class KeeperAiTraps {

    private static final short ROOM_WORKSHOP = 10;
    private static final short TRAP_BOULDER = 2;

    /**
     * DK II grows the box round the rooms by -2 to 7 tiles
     */
    private static final int BOX_GROWTH_ROLL = 10;
    private static final int BOX_GROWTH_LEAST = -2;

    /**
     * No trap or door goes nearer than this to another of its own
     */
    private static final int TILES_APART = 3;

    /**
     * A Boulder Trap for long corridors looks this far along the corridor
     */
    private static final int LONG_CORRIDOR_TILES = 4;

    /**
     * The rolls of DK II's choice: 4 sided against the door preference and
     * for a Boulder Trap, 5 sided against the trap style for the other
     * traps, and 50 in 100 for each step of the door usage
     */
    private static final int DOOR_ROLL = 4;
    private static final int BOULDER_ROLL = 4;
    private static final int TRAP_ROLL = 5;
    private static final int DOOR_USAGE_STEP_PERCENTAGE = 50;

    private final IKwdFile kwdFile;
    private final IGameWorldController gameWorldController;
    private final IMapController mapController;
    private final KeeperAiBattles.DecisionLog decisionLog;
    private final EntitySet traps;
    private final EntitySet doors;

    KeeperAiTraps(EntityData entityData, IKwdFile kwdFile, IGameWorldController gameWorldController, KeeperAiBattles.DecisionLog decisionLog) {
        this.kwdFile = kwdFile;
        this.gameWorldController = gameWorldController;
        this.mapController = gameWorldController.getMapController();
        this.decisionLog = decisionLog;
        traps = entityData.getEntities(TrapComponent.class, Owner.class, Position.class);
        doors = entityData.getEntities(DoorComponent.class, Owner.class, Position.class);
    }

    void release() {
        traps.release();
        doors.release();
    }

    /**
     * With a Workshop, and a roll against its settings, it goes round the
     * edge of a box round its rooms, made a few tiles bigger or smaller by
     * chance. Each corridor tile on the edge with no trap or door of its own
     * near gets a door or a trap. Of the west edge only the corner is
     * looked at, as in DK II. Each one placed takes an action.
     *
     * @param keeperAi the computer player
     * @param playerController its player
     * @param ai its settings
     * @param actions the actions it has left
     * @return the actions it has left after this
     */
    int placeTrapsAndDoors(KeeperAi keeperAi, IPlayerController playerController, AI ai, int actions) {
        if (!hasRoom(playerController, ROOM_WORKSHOP) || Utils.getRandom().nextInt(100) >= ai.getChanceOfLookingToUseTrapsAndDoors()) {
            return actions;
        }

        // The box round its rooms
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        for (Set<IRoomController> rooms : playerController.getRoomControl().getRoomControllers().values()) {
            for (IRoomController room : rooms) {
                Point centre = room.getRoomInstance().getCenter();
                minX = Math.min(minX, centre.x);
                minY = Math.min(minY, centre.y);
                maxX = Math.max(maxX, centre.x);
                maxY = Math.max(maxY, centre.y);
            }
        }
        if (minX == Integer.MAX_VALUE) {
            return actions;
        }
        traps.applyChanges();
        doors.applyChanges();
        int growth = Utils.getRandom().nextInt(BOX_GROWTH_ROLL) + BOX_GROWTH_LEAST;
        int width = mapController.getMapData().getWidth();
        int height = mapController.getMapData().getHeight();
        minX = clamp(minX - growth, width);
        minY = clamp(minY - growth, height);
        maxX = clamp(maxX + growth, width);
        maxY = clamp(maxY + growth, height);

        List<Point> edge = new ArrayList<>();
        edge.add(new Point(minX, minY));
        for (int y = minY; y <= maxY; y++) {
            edge.add(new Point(maxX, y));
        }
        for (int x = minX; x <= maxX; x++) {
            edge.add(new Point(x, minY));
        }
        for (int x = minX; x <= maxX; x++) {
            edge.add(new Point(x, maxY));
        }
        for (Point tile : edge) {
            if (actions == 0) {
                break;
            }
            if (isCorridor(keeperAi.playerId, tile) && !hasTrapOrDoorNear(keeperAi.playerId, tile)) {
                actions = placeTrapOrDoor(keeperAi, playerController, ai, tile, actions);
            }
        }

        return actions;
    }

    /**
     * A door when a 4 sided die rolls below its door preference, otherwise a
     * trap
     */
    private int placeTrapOrDoor(KeeperAi keeperAi, IPlayerController playerController, AI ai, Point tile, int actions) {
        int gold = playerController.getKeeper().getGold();
        if (Utils.getRandom().nextInt(DOOR_ROLL) < ai.getDoorTrapPreference()) {
            Door door = chooseDoor(playerController, ai);
            if (door == null) {
                return actions;
            }
            gameWorldController.placeDoor(door.getDoorId(), tile, keeperAi.playerId);
            if (playerController.getKeeper().getGold() < gold) {
                decisionLog.log(keeperAi, "place door " + door.getDoorId(), tile, "a corridor on the edge of its dungeon");
                return actions - 1;
            }
        } else {
            Trap trap = chooseTrap(playerController, ai, keeperAi.playerId, tile);
            if (trap == null) {
                return actions;
            }
            gameWorldController.placeTrap(trap.getTrapId(), tile, keeperAi.playerId);
            if (playerController.getKeeper().getGold() < gold) {
                decisionLog.log(keeperAi, "place trap " + trap.getTrapId(), tile, "a corridor on the edge of its dungeon");
                return actions - 1;
            }
        }

        return actions;
    }

    /**
     * The doors it has, in id order, but no barricades. Each is taken with
     * a chance by the door usage: the worst door at once, a random one, or
     * else mostly the last, the best
     */
    private Door chooseDoor(IPlayerController playerController, AI ai) {
        int usage = ai.getDoorUsage() == AI.DoorUsagePolicy.WORST_DOOR ? 2 : ai.getDoorUsage() == AI.DoorUsagePolicy.RANDOM ? 1 : 0;
        Door chosen = null;
        List<Door> doorTypes = new ArrayList<>(kwdFile.getDoors());
        doorTypes.sort(Comparator.comparingInt(Door::getDoorId));
        for (Door door : doorTypes) {
            ResearchableEntity availability = playerController.getDoorControl().getTypes().get(door);
            if (door.getFlags().contains(Door.DoorFlag.IS_BARRICADE) || availability == null || !availability.isDiscovered()) {
                continue;
            }
            chosen = door;
            if (Utils.getRandom().nextInt(100) <= usage * DOOR_USAGE_STEP_PERCENTAGE) {
                break;
            }
        }

        return chosen;
    }

    /**
     * The traps it has, in id order. A Boulder Trap is taken by a roll
     * against the trap style, or, set to go in long corridors, when the
     * corridor goes on. A trap that fires on sight is taken more often by
     * an aggressive style, any other more often by a defensive one. Without
     * a pick, the last it looked at
     */
    private Trap chooseTrap(IPlayerController playerController, AI ai, short playerId, Point tile) {
        Trap chosen = null;
        List<Trap> trapTypes = new ArrayList<>(kwdFile.getTraps());
        trapTypes.sort(Comparator.comparingInt(Trap::getTrapId));
        for (Trap trap : trapTypes) {
            ResearchableEntity availability = playerController.getTrapControl().getTypes().get(trap);
            if (availability == null || !availability.isDiscovered()) {
                continue;
            }
            if (trap.getTrapId() == TRAP_BOULDER && ai.isBoulderTrapsOnLongCorridors()) {
                if (countCorridorTiles(playerId, tile) > 0) {
                    return trap;
                }
                continue;
            }
            chosen = trap;
            boolean taken;
            if (trap.getTrapId() == TRAP_BOULDER) {
                taken = Utils.getRandom().nextInt(BOULDER_ROLL) > ai.getTrapUseStyle();
            } else if (trap.getTriggerType() == Trap.TriggerType.LINE_OF_SIGHT) {
                taken = Utils.getRandom().nextInt(TRAP_ROLL) > ai.getTrapUseStyle();
            } else {
                taken = Utils.getRandom().nextInt(TRAP_ROLL) < ai.getTrapUseStyle();
            }
            if (taken) {
                break;
            }
        }

        return chosen;
    }

    /**
     * The corridor tiles in a row from a tile, up to four each way
     */
    private int countCorridorTiles(short playerId, Point tile) {
        int tiles = 0;
        for (Point direction : new Point[]{new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0)}) {
            for (int i = 1; i <= LONG_CORRIDOR_TILES && isCorridor(playerId, new Point(tile.x + direction.x * i, tile.y + direction.y * i)); i++) {
                tiles++;
            }
        }

        return tiles;
    }

    /**
     * An open tile between two walls, along either axis, that are not
     * tagged to be dug. Not on the edge of the map
     */
    private boolean isCorridor(short playerId, Point tile) {
        if (tile.x < 1 || tile.y < 1 || tile.x >= mapController.getMapData().getWidth() - 1
                || tile.y >= mapController.getMapData().getHeight() - 1 || mapController.isSolid(tile)) {
            return false;
        }
        Point west = new Point(tile.x - 1, tile.y);
        Point east = new Point(tile.x + 1, tile.y);
        if (mapController.isSolid(west) && mapController.isSolid(east)) {
            return !isTagged(playerId, west) && !isTagged(playerId, east);
        }
        Point north = new Point(tile.x, tile.y - 1);
        Point south = new Point(tile.x, tile.y + 1);

        return mapController.isSolid(north) && mapController.isSolid(south) && !isTagged(playerId, north) && !isTagged(playerId, south);
    }

    private boolean isTagged(short playerId, Point tile) {
        return mapController.getMapData().getTile(tile).isSelected(playerId);
    }

    private boolean hasTrapOrDoorNear(short playerId, Point tile) {
        for (EntitySet entities : new EntitySet[]{traps, doors}) {
            for (Entity entity : entities) {
                Point p = WorldUtils.vectorToPoint(entity.get(Position.class).position);
                if (entity.get(Owner.class).ownerId == playerId
                        && (p.x - tile.x) * (p.x - tile.x) + (p.y - tile.y) * (p.y - tile.y) < TILES_APART * TILES_APART) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasRoom(IPlayerController playerController, short roomId) {
        Room room = kwdFile.getRoomById(roomId);

        return room != null && playerController.getRoomControl().getTypeCount(room) > 0;
    }

    /**
     * Keeps a tile off the edge of the map
     */
    private static int clamp(int value, int size) {
        return Math.max(1, Math.min(value, size - 2));
    }

}
