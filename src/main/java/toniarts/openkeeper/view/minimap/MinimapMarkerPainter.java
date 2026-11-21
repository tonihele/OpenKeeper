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

import com.jme3.math.FastMath;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The marker overlay drawn into the raster after the tile colours, before
 * upload. Owns the creature/door/trap/room/in-hand {@code EntitySet}s for the life of
 * the session ({@link #update()} must be called once a frame before
 * {@link #paint}; {@link #dispose()} releases them).
 */
public final class MinimapMarkerPainter {

    private static final int HEART_LINE_COLOUR = 0xFFFFFFFF;
    private static final int OWN_HEART_RING_COLOUR = 0xFFADD8E6;

    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;
    /**
     * Rebuilds a possession pulse takes to grow from the creature to the full
     * Dungeon Heart reporting radius before it restarts
     */
    private static final int POSSESSION_PULSE_STEPS = 10;

    private final MinimapPalette palette;
    private final short viewerId;
    private final Set<Short> guardPostTrapIds;
    private final float guardPostReportingDistanceTiles;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final int guardRoomReportingDistanceTiles;

    private final EntitySet creatures;
    private final EntitySet doors;
    private final EntitySet traps;
    private final EntitySet rooms;
    private final EntitySet inHand;

    /**
     * @param entityData the entity data to read the markers from
     * @param palette the minimap palette
     * @param viewerId the local player
     * @param guardPostTrapIds the trap types that are guard posts
     * @param guardPostReportingDistanceTiles
     * {@code GUARD_POST_REPORTING_DISTANCE_TILES}, the radius of the ring
     * drawn around the viewer's own guard posts
     * @param mapData the map tiles, to find the tiles of the viewer's own
     * Guard Rooms
     * @param guardRoomReportingDistanceTiles
     * {@code GUARD_ROOM_REPORTING_DISTANCE_TILES}, how far the outline drawn
     * around the viewer's own Guard Rooms is widened
     */
    public MinimapMarkerPainter(EntityData entityData, MinimapPalette palette, short viewerId,
            Set<Short> guardPostTrapIds, float guardPostReportingDistanceTiles,
            IMapDataInformation<? extends IMapTileInformation> mapData, float guardRoomReportingDistanceTiles) {
        this.palette = palette;
        this.viewerId = viewerId;
        this.guardPostTrapIds = guardPostTrapIds;
        this.guardPostReportingDistanceTiles = guardPostReportingDistanceTiles;
        this.mapData = mapData;
        this.guardRoomReportingDistanceTiles = (int) guardRoomReportingDistanceTiles;
        creatures = entityData.getEntities(Position.class, Owner.class, CreatureComponent.class);
        doors = entityData.getEntities(Position.class, Owner.class, DoorComponent.class);
        traps = entityData.getEntities(Position.class, Owner.class, TrapViewState.class);
        rooms = entityData.getEntities(RoomComponent.class, Owner.class);
        inHand = entityData.getEntities(InHand.class);
    }

    /**
     * Must be called once a frame before {@link #paint}.
     */
    public void update() {
        creatures.applyChanges();
        doors.applyChanges();
        traps.applyChanges();
        rooms.applyChanges();
        inHand.applyChanges();
    }

    public void dispose() {
        creatures.release();
        doors.release();
        traps.release();
        rooms.release();
        inHand.release();
    }

    /**
     * @param raster the 128x128 B,G,R raster to draw into, already filled
     * with tile colours
     * @param mapWidth, mapHeight the map size in tiles
     * @param zoom -1 (fit) .. 4 (16px/tile)
     * @param cameraTileX, cameraTileY the camera's look-at tile (zoomed
     * mode only; ignored in fit mode)
     * @param fogOfWarInformation for the "tile explored" visibility checks
     * @param blinkParity the current half of the blink cycle
     * @param dashPhase an incrementing counter (any unit is fine - only its
     * changing value matters) driving the heart-direction line's marching
     * dash animation
     * @param rotationPhase advances by one every rebuild; drives neutral-
     * owned creatures' rotating marker colour (see {@link
     * MinimapMarkers#neutralRotationColour} and {@link
     * MinimapRasteriser}'s identical treatment of neutral-owned rooms)
     * @param dungeonHeartTile the local player's Dungeon Heart tile, or
     * {@code null} if it hasn't been built/located yet
     * @param dungeonHeartReportingDistanceTiles
     * {@code DUNGEON_HEART_REPORTING_DISTANCE_TILES}
     * @param possessedCreature the creature the viewer is possessing, or
     * {@code null}; it blinks white and gets an expanding pulse circle
     */
    public void paint(byte[] raster, int mapWidth, int mapHeight, int zoom, float cameraTileX, float cameraTileY,
            IFogOfWarInformation fogOfWarInformation, boolean blinkParity, int dashPhase, int rotationPhase,
            Point dungeonHeartTile, float dungeonHeartReportingDistanceTiles, EntityId possessedCreature) {
        boolean fit = zoom < 0;
        MinimapRasteriser.FitGeometry fitGeometry = fit ? MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight) : null;
        int pixelsPerTile = fit ? 0 : (1 << zoom);

        paintOwnHeartRing(raster, dungeonHeartTile, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile,
                dungeonHeartReportingDistanceTiles);
        paintOwnGuardRooms(raster, mapWidth, mapHeight, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile);
        paintCreatures(raster, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile, fogOfWarInformation,
                blinkParity, rotationPhase, dungeonHeartReportingDistanceTiles * 1.5f, possessedCreature);
        paintTraps(raster, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile, fogOfWarInformation, blinkParity);
        paintDoors(raster, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile, fogOfWarInformation, blinkParity);
        paintHeartDirectionLine(raster, fit, dungeonHeartTile, cameraTileX, cameraTileY, pixelsPerTile, dashPhase);
    }

    /*
     * Marker 1: own Dungeon Heart.
     */
    private void paintOwnHeartRing(byte[] raster, Point dungeonHeartTile, boolean fit,
            MinimapRasteriser.FitGeometry fitGeometry, float cameraTileX, float cameraTileY, int pixelsPerTile,
            float dungeonHeartReportingDistanceTiles) {
        if (dungeonHeartTile == null) {
            return;
        }
        float pixelsPerTileScale = fit ? fitGeometry.pixelsPerTileScale() : pixelsPerTile;
        float px = toPixelX(dungeonHeartTile.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
        float py = toPixelY(dungeonHeartTile.y, fit, fitGeometry, cameraTileY, pixelsPerTile);
        int radius = Math.round(dungeonHeartReportingDistanceTiles * pixelsPerTileScale);
        MinimapMarkers.circleOutline(raster, Math.round(px), Math.round(py), radius, OWN_HEART_RING_COLOUR);
    }

    /*
     * Own Guard Rooms: the room's bounds widened by the guard room reporting
     * distance, in the Dungeon Heart ring's colour, like DK II.
     */
    private void paintOwnGuardRooms(byte[] raster, int mapWidth, int mapHeight, boolean fit,
            MinimapRasteriser.FitGeometry fitGeometry, float cameraTileX, float cameraTileY, int pixelsPerTile) {
        Map<EntityId, int[]> bounds = new HashMap<>();
        for (Entity e : rooms) {
            RoomComponent room = e.get(RoomComponent.class);
            if (e.get(Owner.class).ownerId == viewerId && !room.destroyed
                    && room.defaultStorageType == ObjectType.GUARD) {
                bounds.put(e.getId(), new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE});
            }
        }
        if (bounds.isEmpty()) {
            return;
        }
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                IMapTileInformation tile = mapData.getTile(x, y);
                int[] b = tile != null && tile.getRoomId() != null ? bounds.get(tile.getRoomId()) : null;
                if (b != null) {
                    b[0] = Math.min(b[0], x);
                    b[1] = Math.max(b[1], x);
                    b[2] = Math.min(b[2], y);
                    b[3] = Math.max(b[3], y);
                }
            }
        }
        for (int[] b : bounds.values()) {
            if (b[0] > b[1]) {
                continue; // none of its tiles are on the map yet
            }
            int x0 = Math.clamp(b[0] - guardRoomReportingDistanceTiles, 1, mapWidth - 1);
            int x1 = Math.clamp(b[1] + guardRoomReportingDistanceTiles, 1, mapWidth - 1);
            int y0 = Math.clamp(b[2] - guardRoomReportingDistanceTiles, 1, mapHeight - 1);
            int y1 = Math.clamp(b[3] + guardRoomReportingDistanceTiles, 1, mapHeight - 1);
            MinimapMarkers.rectOutline(raster,
                    Math.round(toPixelX(x0, fit, fitGeometry, cameraTileX, pixelsPerTile)),
                    Math.round(toPixelY(y0, fit, fitGeometry, cameraTileY, pixelsPerTile)),
                    Math.round(toPixelX(x1, fit, fitGeometry, cameraTileX, pixelsPerTile)),
                    Math.round(toPixelY(y1, fit, fitGeometry, cameraTileY, pixelsPerTile)),
                    OWN_HEART_RING_COLOUR);
        }
    }

    /*
     * Marker 2: own creatures.
     * Marker 3: other players' creatures.
     */
    private void paintCreatures(byte[] raster, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, IFogOfWarInformation fogOfWarInformation,
            boolean blinkParity, int rotationPhase, float pulseMaxRadiusTiles,
            EntityId possessedCreature) {
        float pixelsPerTileScale = fit ? fitGeometry.pixelsPerTileScale() : pixelsPerTile;
        for (Entity e : creatures) {
            Owner owner = e.get(Owner.class);
            Position position = e.get(Position.class);
            boolean own = owner.ownerId == viewerId;
            if (own) {
                if (inHand.containsId(e.getId())) {
                    continue; // in hand, not on the map
                }
            } else {
                Point tile = WorldUtils.vectorToPoint(position.position);
                if (!fogOfWarInformation.isExplored(tile)) {
                    continue;
                }
            }
            float px = toPixelX(position.position.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(position.position.z, fit, fitGeometry, cameraTileY, pixelsPerTile);
            int baseColour = owner.ownerId == Player.NEUTRAL_PLAYER_ID
                    ? MinimapMarkers.neutralRotationColour(rotationPhase) : wallColour(owner.ownerId);
            // the possessed creature blinks white so it stands out as "you"
            boolean possessed = e.getId().equals(possessedCreature);
            int blinkColour = possessed ? WHITE : BLACK;
            int colour = MinimapMarkers.blink(baseColour, blinkColour, blinkParity);
            MinimapMarkers.dot(raster, Math.round(px), Math.round(py), colour);
            if (possessed) {
                // white circle expanding from the possessed creature up to
                // the Dungeon Heart circle's size, then starting over
                int step = rotationPhase % POSSESSION_PULSE_STEPS + 1;
                int radius = Math.round(pulseMaxRadiusTiles * pixelsPerTileScale * step / POSSESSION_PULSE_STEPS);
                MinimapMarkers.circleOutline(raster, Math.round(px), Math.round(py), radius, WHITE);
            }
        }
    }

    /*
     * Marker 4: all traps (own, or explored - hiddenFromMinimap doesn't
     * exist in this codebase, see this class's own javadoc). Hidden traps and
     * blueprints only for their owner.
     */
    private void paintTraps(byte[] raster, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, IFogOfWarInformation fogOfWarInformation,
            boolean blinkParity) {
        for (Entity e : traps) {
            Owner owner = e.get(Owner.class);
            Position position = e.get(Position.class);
            Point tile = WorldUtils.vectorToPoint(position.position);
            boolean own = owner.ownerId == viewerId;
            if (!own && !fogOfWarInformation.isExplored(tile)
                    || !e.get(TrapViewState.class).isShownTo(owner.ownerId, viewerId)) {
                continue;
            }
            float px = toPixelX(position.position.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(position.position.z, fit, fitGeometry, cameraTileY, pixelsPerTile);
            TrapViewState trap = e.get(TrapViewState.class);
            if (own && !trap.blueprint && guardPostTrapIds.contains(trap.trapId)) {
                // Same colour as the Dungeon Heart's ring, like DK II
                float pixelsPerTileScale = fit ? fitGeometry.pixelsPerTileScale() : pixelsPerTile;
                int radius = Math.round(guardPostReportingDistanceTiles * pixelsPerTileScale);
                MinimapMarkers.circleOutline(raster, Math.round(px), Math.round(py), radius, OWN_HEART_RING_COLOUR);
            }
            int colour = MinimapMarkers.blink(wallColour(owner.ownerId), BLACK, blinkParity);
            MinimapMarkers.dot(raster, Math.round(px), Math.round(py), colour);
        }
    }

    /*
     * Marker 5: all doors (own, or explored).
     */
    private void paintDoors(byte[] raster, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, IFogOfWarInformation fogOfWarInformation,
            boolean blinkParity) {
        for (Entity e : doors) {
            Owner owner = e.get(Owner.class);
            Position position = e.get(Position.class);
            Point tile = WorldUtils.vectorToPoint(position.position);
            boolean own = owner.ownerId == viewerId;
            if (!own && !fogOfWarInformation.isExplored(tile)) {
                continue;
            }
            float px = toPixelX(position.position.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(position.position.z, fit, fitGeometry, cameraTileY, pixelsPerTile);

            int colour = MinimapMarkers.blink(BLACK, wallColour(owner.ownerId), blinkParity);
            // DoorsController rotates a door -HALF_PI when the tiles north
            // and south of it are walls (an east-west corridor), leaving
            // the door itself spanning north-south - drawn as the vertical
            // glyph; the untouched rotation=0 default spans east-west.
            boolean wallsNorthSouth = Math.abs(position.rotation - (-FastMath.HALF_PI)) < 0.01f;
            int rx = Math.round(px);
            int ry = Math.round(py);
            if (wallsNorthSouth) {
                MinimapMarkers.doorGlyphV(raster, rx, ry, colour);
            } else {
                MinimapMarkers.doorGlyphH(raster, rx, ry, colour);
            }
        }
    }

    /*
     * Heart-direction line, zoomed mode only.
     */
    private void paintHeartDirectionLine(byte[] raster, boolean fit, Point dungeonHeartTile, float cameraTileX,
            float cameraTileY, int pixelsPerTile, int dashPhase) {
        if (fit || dungeonHeartTile == null) {
            return;
        }
        float px = toPixelX(dungeonHeartTile.x, false, null, cameraTileX, pixelsPerTile);
        float py = toPixelY(dungeonHeartTile.y, false, null, cameraTileY, pixelsPerTile);
        int size = MinimapRasteriser.RASTER_SIZE;
        if (px < 0 || py < 0 || px >= size || py >= size) {
            float centre = size / 2f;
            float dx = px - centre;
            float dy = py - centre;
            float length = FastMath.sqrt(dx * dx + dy * dy);
            if (length > 0.0001f) {
                float[] edge = MinimapMarkers.clipToEdge(centre, centre, dx / length, dy / length, size);
                MinimapMarkers.dottedLine(raster, Math.round(centre), Math.round(centre),
                        Math.round(edge[0]), Math.round(edge[1]), HEART_LINE_COLOUR, 3, dashPhase);
            }
        }
    }

    private int wallColour(short ownerId) {
        short playerNumber = PlayerNumbers.playerNumber(ownerId);
        return palette.rawArgb()[MapColourClass.OWNED_SOLID_BASE + playerNumber];
    }

    private static float toPixelX(float tileX, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, int pixelsPerTile) {
        return fit ? fitGeometry.pixelX(tileX) : MinimapMarkers.zoomedPixelX(tileX, cameraTileX, pixelsPerTile);
    }

    private static float toPixelY(float tileY, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileY, int pixelsPerTile) {
        return fit ? fitGeometry.pixelY(tileY) : MinimapMarkers.zoomedPixelY(tileY, cameraTileY, pixelsPerTile);
    }

}
