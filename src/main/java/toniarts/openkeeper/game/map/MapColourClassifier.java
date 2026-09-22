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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The per-tile colour-class decision
 */
public final class MapColourClassifier {

    private final IMapInformation<? extends IMapTileInformation> mapInformation;
    private final IFogOfWarInformation fogOfWarInformation;
    private final IRoomsInformation<? extends IRoomInformation> roomsInformation;

    public MapColourClassifier(IMapInformation<? extends IMapTileInformation> mapInformation,
            IFogOfWarInformation fogOfWarInformation,
            IRoomsInformation<? extends IRoomInformation> roomsInformation) {
        this.mapInformation = mapInformation;
        this.fogOfWarInformation = fogOfWarInformation;
        this.roomsInformation = roomsInformation;
    }

    public short classify(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);
        if (tile == null) {
            return MapColourClass.UNEXPLORED_OR_IMPENETRABLE;
        }

        Point p = new Point(x, y);
        Terrain terrain = mapInformation.getTerrain(tile);

        boolean revealThroughFog = terrain.getFlags().contains(Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR);
        boolean visible = (fogOfWarInformation.isPerceived(p) && revealThroughFog) || fogOfWarInformation.isExplored(p);
        if (!visible) {
            return MapColourClass.UNEXPLORED_OR_IMPENETRABLE;
        }

        IRoomInformation room = resolveRoom(tile.getRoomId());
        if (room != null && room.isDungeonHeart()) {
            return (short) (MapColourClass.DUNGEON_HEART_BASE + PlayerNumbers.playerNumber(room.getOwnerId()));
        }

        short owner = tile.getOwnerId();
        if (owner != Player.NEUTRAL_PLAYER_ID) {
            boolean solid = mapInformation.isSolid(p);
            short base = solid ? MapColourClass.OWNED_SOLID_BASE : MapColourClass.OWNED_FLOOR_BASE;
            return (short) (base + PlayerNumbers.playerNumber(owner));
        }

        if (room != null) {
            return (short) (MapColourClass.OWNED_FLOOR_BASE + PlayerNumbers.playerNumber(room.getOwnerId()));
        }

        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            boolean impenetrable = terrain.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE);
            if (terrain.getGoldValue() != 0) {
                return impenetrable ? MapColourClass.GEMS : MapColourClass.GOLD;
            }
            return impenetrable ? MapColourClass.UNEXPLORED_OR_IMPENETRABLE : MapColourClass.DIGGABLE_ROCK;
        }
        if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            return MapColourClass.WATER;
        }
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            return MapColourClass.LAVA;
        }
        return TerrainClaimability.isClaimableFloor(terrain)
                ? MapColourClass.CLAIMABLE_FLOOR : MapColourClass.NEUTRAL_OWNED_SENTINEL;
    }

    private IRoomInformation resolveRoom(EntityId roomInstanceId) {
        if (roomInstanceId == null) {
            return null;
        }
        IRoomInformation room = roomsInformation.getRoomInformation(roomInstanceId);
        return (room != null && !room.isRemoved()) ? room : null;
    }

}
