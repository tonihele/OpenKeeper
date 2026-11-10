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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * The marker overlay drawn into the raster after the tile colours, before
 * upload. Owns the creature/door/trap/in-hand {@code EntitySet}s for the life of
 * the session ({@link #update()} must be called once a frame before
 * {@link #paint}; {@link #dispose()} releases them).
 */
public final class MinimapMarkerPainter {

    private static final int HEART_LINE_COLOUR = 0xFFFFFFFF;
    private static final int OWN_HEART_RING_COLOUR = 0xFFADD8E6;

    private static final int BLACK = 0xFF000000;

    private final MinimapPalette palette;
    private final short viewerId;

    private final EntitySet creatures;
    private final EntitySet doors;
    private final EntitySet traps;
    private final EntitySet inHand;

    public MinimapMarkerPainter(EntityData entityData, MinimapPalette palette, short viewerId) {
        this.palette = palette;
        this.viewerId = viewerId;
        creatures = entityData.getEntities(Position.class, Owner.class, CreatureComponent.class);
        doors = entityData.getEntities(Position.class, Owner.class, DoorComponent.class);
        traps = entityData.getEntities(Position.class, Owner.class, TrapComponent.class);
        inHand = entityData.getEntities(InHand.class);
    }

    /**
     * Must be called once a frame before {@link #paint}.
     */
    public void update() {
        creatures.applyChanges();
        doors.applyChanges();
        traps.applyChanges();
        inHand.applyChanges();
    }

    public void dispose() {
        creatures.release();
        doors.release();
        traps.release();
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
     */
    public void paint(byte[] raster, int mapWidth, int mapHeight, int zoom, float cameraTileX, float cameraTileY,
            IFogOfWarInformation fogOfWarInformation, boolean blinkParity, int dashPhase, int rotationPhase,
            Point dungeonHeartTile, float dungeonHeartReportingDistanceTiles) {
        boolean fit = zoom < 0;
        MinimapRasteriser.FitGeometry fitGeometry = fit ? MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight) : null;
        int pixelsPerTile = fit ? 0 : (1 << zoom);

        paintOwnHeartRing(raster, dungeonHeartTile, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile,
                dungeonHeartReportingDistanceTiles);
        paintCreatures(raster, fit, fitGeometry, cameraTileX, cameraTileY, pixelsPerTile, fogOfWarInformation,
                blinkParity, rotationPhase);
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
     * Marker 2: own creatures.
     * Marker 3: other players' creatures.
     */
    private void paintCreatures(byte[] raster, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, IFogOfWarInformation fogOfWarInformation,
            boolean blinkParity, int rotationPhase) {
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
            int colour = MinimapMarkers.blink(baseColour, BLACK, blinkParity);
            MinimapMarkers.dot(raster, Math.round(px), Math.round(py), colour);
        }
    }

    /*
     * Marker 4: all traps (own, or explored - hiddenFromMinimap doesn't
     * exist in this codebase, see this class's own javadoc).
     */
    private void paintTraps(byte[] raster, boolean fit, MinimapRasteriser.FitGeometry fitGeometry,
            float cameraTileX, float cameraTileY, int pixelsPerTile, IFogOfWarInformation fogOfWarInformation,
            boolean blinkParity) {
        for (Entity e : traps) {
            Owner owner = e.get(Owner.class);
            Position position = e.get(Position.class);
            Point tile = WorldUtils.vectorToPoint(position.position);
            boolean own = owner.ownerId == viewerId;
            if (!own && !fogOfWarInformation.isExplored(tile)) {
                continue;
            }
            float px = toPixelX(position.position.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(position.position.z, fit, fitGeometry, cameraTileY, pixelsPerTile);
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
