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
package toniarts.openkeeper.common;

import java.util.List;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.game.map.WallSection;
import toniarts.openkeeper.utils.Point;

/**
 * Holds a room instance, series of coordinates that together form a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class RoomInstance extends EntityInstance<Room> {

    private static final short PORTAL_ROOM_ID = 3;
    private static final short DUNGEON_HEART_ROOM_ID = 5;
    private static final short HERO_GATE_ROOM_ID = 18;
    private static final short MERCENARY_GATE_ROOM_ID = 24;
    private static final short HERO_PORTAL_ROOM_ID = 25;

    private List<WallSection> wallSections;
    private final Thing.Room.Direction direction;
    private short ownerId;
    private boolean destroyed = false;
    private int health;
    private int maxHealth;
    private final int initialHealth;

    public RoomInstance(Room room) {
        this(room, null);
    }

    public RoomInstance(Room room, Thing.Room thing) {
        super(room);
        if (thing != null) {
            this.direction = thing.getDirection();
            this.initialHealth = thing.getInitialHealth();
        } else {
            this.direction = null;
            this.initialHealth = 100;
        }
    }

    public Room getRoom() {
        return super.getEntity();
    }

    public void setWallSections(List<WallSection> wallSections) {
        this.wallSections = wallSections;
    }

    public List<WallSection> getWallSections() {
        return wallSections;
    }

    /**
     * Get the room instance direction. Some fixed rooms may have this.
     *
     * @return room direction
     */
    public Thing.Room.Direction getDirection() {
        return direction;
    }

    public void setOwnerId(short ownerId) {
        this.ownerId = ownerId;
    }

    public short getOwnerId() {
        return ownerId;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    /**
     * Get the health the room starts with, in percent. A room placed in the
     * level editor can start damaged.
     *
     * @return the initial health percentage
     */
    public int getInitialHealth() {
        return initialHealth;
    }

    /**
     * Get the tile DK II keeps for a room (0x4E4590, worked out again
     * whenever the room changes). Its computer players drop creatures on it
     * and measure from it. A Portal takes the tile one in diagonally from
     * its first tile, a Dungeon Heart the tile three in, so one past the
     * middle, the hero gates their first tile. Any other room takes the
     * average of its tiles, or when that is not one of them the first of
     * its tiles on a spiral round it: east, south, west, north, every second
     * side one tile longer. The first tile is the top left one, the map is
     * read row by row.
     *
     * @return the reference tile
     */
    public Point getReferenceTile() {
        List<Point> tiles = getCoordinates();
        Point first = tiles.getFirst();
        short roomId = getRoom().getRoomId();
        if (roomId == PORTAL_ROOM_ID || roomId == HERO_PORTAL_ROOM_ID) {
            return new Point(first.x + 1, first.y + 1);
        }
        if (roomId == DUNGEON_HEART_ROOM_ID) {
            return new Point(first.x + 3, first.y + 3);
        }
        if (roomId >= HERO_GATE_ROOM_ID && roomId <= MERCENARY_GATE_ROOM_ID) {
            return first;
        }

        int sumX = 0;
        int sumY = 0;
        for (Point tile : tiles) {
            sumX += tile.x;
            sumY += tile.y;
        }
        Point tile = new Point(sumX / tiles.size(), sumY / tiles.size());
        int dx = 1;
        int dy = 0;
        for (int side = 2, step = 1; !hasCoordinate(tile); step++) {
            tile = new Point(tile.x + dx, tile.y + dy);
            if (step == side / 2) {
                step = 0;
                side++;
                int turn = dx;
                dx = -dy;
                dy = turn;
            }
        }

        return tile;
    }

}
