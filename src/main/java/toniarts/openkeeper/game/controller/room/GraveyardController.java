/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomCorpseControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The Graveyard. Imps bring dead bodies here, one body per tile. The room has
 * a crypt for every ten tiles, each on a tile whose eight neighbours are all
 * graveyard and with no other crypt next to it. A crypt tile can't hold a
 * body.
 */
public final class GraveyardController extends NormalRoomController {

    /**
     * The Graveyard Crypt object
     */
    public static final short OBJECT_CRYPT_ID = 75;
    private static final int TILES_PER_CRYPT = 10;
    private static final Random RANDOM = new Random();

    private final IGameTimer gameTimer;
    private final Set<Point> cryptTiles = new HashSet<>();

    public GraveyardController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.CORPSE);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        addObjectControl(new RoomCorpseControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected Collection<Point> getCoordinates() {
                Collection<Point> coordinates = super.getCoordinates();
                coordinates.removeAll(cryptTiles);
                return coordinates;
            }

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }
        });
    }

    @Override
    protected void constructObjects() {
        super.constructObjects();

        cryptTiles.clear();
        for (Point p : chooseCryptTiles(map, getCryptCount(roomInstance.getCoordinates().size()), RANDOM)) {
            Point tile = new Point(start.x + p.x, start.y + p.y);
            removeFloorFurniture(tile);
            floorFurniture.add(objectsController.loadObject(OBJECT_CRYPT_ID, roomInstance.getOwnerId(), tile.x, tile.y));
            cryptTiles.add(tile);
        }
    }

    private void removeFloorFurniture(Point tile) {
        Iterator<EntityId> iter = floorFurniture.iterator();
        while (iter.hasNext()) {
            EntityId furniture = iter.next();
            Position position = entityData.getComponent(furniture, Position.class);
            if (position != null && tile.equals(WorldUtils.vectorToPoint(position.position))) {
                entityData.removeEntity(furniture);
                iter.remove();
            }
        }
    }

    /**
     * Get the number of crypts a graveyard of given size has
     *
     * @param tileCount the number of tiles in the graveyard
     * @return the number of crypts
     */
    static int getCryptCount(int tileCount) {
        return tileCount / TILES_PER_CRYPT;
    }

    /**
     * Picks the crypt tiles at random. A crypt needs all its eight neighbours
     * to be graveyard, and the tile and the neighbours must be free of other
     * crypts. When no tile qualifies, the rest of the crypts are left out.
     *
     * @param map the room tiles
     * @param count the number of crypts wanted
     * @param random the random generator
     * @return the crypt tiles, in room matrix coordinates
     */
    static List<Point> chooseCryptTiles(boolean[][] map, int count, Random random) {
        List<Point> crypts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            List<Point> candidates = new ArrayList<>();
            for (int x = 0; x < map.length; x++) {
                for (int y = 0; y < map[x].length; y++) {
                    if (isCryptTile(map, crypts, x, y)) {
                        candidates.add(new Point(x, y));
                    }
                }
            }
            if (candidates.isEmpty()) {
                break;
            }
            crypts.add(candidates.get(random.nextInt(candidates.size())));
        }
        return crypts;
    }

    private static boolean isCryptTile(boolean[][] map, List<Point> crypts, int x, int y) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!hasSameTile(map, x + dx, y + dy) || crypts.contains(new Point(x + dx, y + dy))) {
                    return false;
                }
            }
        }
        return true;
    }

}
