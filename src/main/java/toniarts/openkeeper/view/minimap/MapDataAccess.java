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

/**
 * The single read-only surface the minimap presentation layer queries,
 * instead of reaching into {@code IMapInformation}/{@code
 * IFogOfWarInformation}/{@code IRoomsInformation}/{@code IPlayerController}/
 * {@code PlayerCamera} directly
 */
public interface MapDataAccess {

    int width();

    int height();

    /**
     * @see toniarts.openkeeper.view.fogofwar.IFogOfWarInformation#isExplored
     */
    boolean isExplored(int x, int y);

    /**
     * @see toniarts.openkeeper.view.fogofwar.IFogOfWarInformation#isPerceived
     */
    boolean isPerceived(int x, int y);

    /**
     * @see toniarts.openkeeper.view.fogofwar.IFogOfWarInformation#isVisible
     */
    boolean isVisible(int x, int y);

    /**
     * In-bounds and resolvable. A placeholder click-acceptance test
     */
    boolean isValidTargetTile(int x, int y);

    /**
     * 0 for an out-of-bounds coordinate.
     */
    short terrainId(int x, int y);

    /**
     * 0 for an out-of-bounds coordinate.
     */
    short ownerId(int x, int y);

    /**
     * The room *instance* occupying the tile, or null if none/out of
     * bounds. Not the room type - resolve that via {@link #roomType}.
     */
    EntityId roomInstanceId(int x, int y);

    boolean isSolidForPathing(int x, int y);

    boolean isClaimableFloor(int x, int y);

    /**
     * The room-type id (e.g. 5 = Dungeon Heart, 9 = Guard Room), or 0 if
     * {@code roomInstanceId} is null or unresolvable.
     */
    short roomType(EntityId roomInstanceId);

    /**
     * The owner of the room instance, or 0 if {@code roomInstanceId} is
     * null or unresolvable.
     */
    short roomOwner(EntityId roomInstanceId);

    short neutralPlayerId();

    /**
     * In this codebase raw player/owner ids already are the design's
     * "player number" (1 hero, 2 neutral, 3..7 keepers)
     */
    short playerNumber(short playerId);

    /**
     * The local viewing player - the one whose fog-of-war/perspective this
     * facade instance is scoped to.
     */
    short viewerId();

    /**
     * The camera's look-at point, in fractional tile-space coordinates.
     */
    Vector2f cameraLookAtTile();

    /**
     * @see MinimapCoordinates#cameraYawRadians
     */
    float cameraYawRadians();

    float dungeonHeartReportingDistanceTiles();

    float guardRoomReportingDistanceTiles();

    /**
     * Write-only and deliberately inert - see the class javadoc.
     */
    void setMapScrollX(int v);

    /**
     * Write-only and deliberately inert - see the class javadoc.
     */
    void setMapScrollY(int v);

}
