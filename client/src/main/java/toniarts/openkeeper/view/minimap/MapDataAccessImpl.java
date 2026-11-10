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
package toniarts.openkeeper.view.minimap;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.game.map.TerrainClaimability;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.PlayerCamera;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * {@link MapDataAccess} wired against the real session interfaces. The
 * two reporting-distance rules are resolved once at construction
 * matching how the rest of this facade treats the world as read-only
 * the caller (whoever owns a {@code GameClientState}) is
 * responsible for resolving
 * {@code Variable.MiscVariable.MiscType.DUNGEON_HEART_REPORTING_DISTANCE_TILES}
 * / {@code GUARD_ROOM_REPORTING_DISTANCE_TILES} and passing the values in,
 * so this class stays free of a session-layer dependency.
 */
public final class MapDataAccessImpl implements MapDataAccess {

    private final IMapInformation<? extends IMapTileInformation> mapInformation;
    private final IFogOfWarInformation fogOfWarInformation;
    private final IRoomsInformation<? extends IRoomInformation> roomsInformation;
    private final PlayerCamera camera;
    private final short viewerId;
    private final float dungeonHeartReportingDistanceTiles;
    private final float guardRoomReportingDistanceTiles;

    // Write-only, deliberately unread - see MapDataAccess's class javadoc.
    private int mapScrollX;
    private int mapScrollY;

    public MapDataAccessImpl(IMapInformation<? extends IMapTileInformation> mapInformation,
            IFogOfWarInformation fogOfWarInformation,
            IRoomsInformation<? extends IRoomInformation> roomsInformation,
            PlayerCamera camera, short viewerId,
            float dungeonHeartReportingDistanceTiles, float guardRoomReportingDistanceTiles) {
        this.mapInformation = mapInformation;
        this.fogOfWarInformation = fogOfWarInformation;
        this.roomsInformation = roomsInformation;
        this.camera = camera;
        this.viewerId = viewerId;
        this.dungeonHeartReportingDistanceTiles = dungeonHeartReportingDistanceTiles;
        this.guardRoomReportingDistanceTiles = guardRoomReportingDistanceTiles;
    }

    private IMapTileInformation getTile(int x, int y) {
        return mapInformation.getMapData().getTile(x, y);
    }

    @Override
    public int width() {
        return mapInformation.getMapData().getWidth();
    }

    @Override
    public int height() {
        return mapInformation.getMapData().getHeight();
    }

    @Override
    public boolean isExplored(int x, int y) {
        return fogOfWarInformation.isExplored(new Point(x, y));
    }

    @Override
    public boolean isPerceived(int x, int y) {
        return fogOfWarInformation.isPerceived(new Point(x, y));
    }

    @Override
    public boolean isVisible(int x, int y) {
        return fogOfWarInformation.isVisible(new Point(x, y));
    }

    @Override
    public boolean isValidTargetTile(int x, int y) {
        return x >= 0 && y >= 0 && x < width() && y < height() && getTile(x, y) != null;
    }

    @Override
    public short terrainId(int x, int y) {
        IMapTileInformation tile = getTile(x, y);
        return tile != null ? tile.getTerrainId() : 0;
    }

    @Override
    public short ownerId(int x, int y) {
        IMapTileInformation tile = getTile(x, y);
        return tile != null ? tile.getOwnerId() : 0;
    }

    @Override
    public EntityId roomInstanceId(int x, int y) {
        IMapTileInformation tile = getTile(x, y);
        return tile != null ? tile.getRoomId() : null;
    }

    @Override
    public boolean isSolidForPathing(int x, int y) {
        return mapInformation.isSolid(new Point(x, y));
    }

    @Override
    public boolean isClaimableFloor(int x, int y) {
        IMapTileInformation tile = getTile(x, y);
        if (tile == null) {
            return false;
        }
        Terrain terrain = mapInformation.getTerrain(tile);
        return TerrainClaimability.isClaimableFloor(terrain);
    }

    @Override
    public short roomType(EntityId roomInstanceId) {
        IRoomInformation room = resolveRoom(roomInstanceId);
        return room != null ? room.getRoomId() : 0;
    }

    @Override
    public short roomOwner(EntityId roomInstanceId) {
        IRoomInformation room = resolveRoom(roomInstanceId);
        return room != null ? room.getOwnerId() : 0;
    }

    private IRoomInformation resolveRoom(EntityId roomInstanceId) {
        if (roomInstanceId == null) {
            return null;
        }
        IRoomInformation room = roomsInformation.getRoomInformation(roomInstanceId);
        return (room != null && !room.isRemoved()) ? room : null;
    }

    @Override
    public short neutralPlayerId() {
        return Player.NEUTRAL_PLAYER_ID;
    }

    @Override
    public short playerNumber(short playerId) {
        return PlayerNumbers.playerNumber(playerId);
    }

    @Override
    public short viewerId() {
        return viewerId;
    }

    @Override
    public Vector2f cameraLookAtTile() {
        return MinimapCoordinates.worldToTile(camera.getLookAt());
    }

    @Override
    public float cameraYawRadians() {
        return MinimapCoordinates.cameraYawRadians(camera.getCamera());
    }

    @Override
    public float dungeonHeartReportingDistanceTiles() {
        return dungeonHeartReportingDistanceTiles;
    }

    @Override
    public float guardRoomReportingDistanceTiles() {
        return guardRoomReportingDistanceTiles;
    }

    @Override
    public void setMapScrollX(int v) {
        this.mapScrollX = v;
    }

    @Override
    public void setMapScrollY(int v) {
        this.mapScrollY = v;
    }

}
