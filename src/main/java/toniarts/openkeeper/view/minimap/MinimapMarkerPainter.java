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
 * The marker overlay (minimap_design.md §5.8), drawn into the raster after
 * the tile colours, before upload. Owns the creature/door/trap
 * {@code EntitySet}s for the life of the session ({@link #update()} must be
 * called once a frame before {@link #paint}; {@link #dispose()} releases
 * them).
 *
 * <p>
 * <b>Rows implemented</b>: 1 (own Dungeon Heart), 4 (own creatures), 5
 * (other players' creatures), 7 (all traps), 9 (all doors), plus the
 * zoomed-mode heart-direction line (§5.9).
 *
 * <p>
 * <b>Rows deliberately not implemented</b> - genuine gaps in this
 * codebase's runtime data, not oversights (confirmed by repo-wide search,
 * minimap_jmonkey.md Step 7's own research pass):
 * <ul>
 * <li>Row 2 (own guard posts) and row 7's guard-post/armed special case:
 * {@code TrapComponent} has no {@code armed}, {@code hiddenFromMinimap}, or
 * guarding-creature field - only {@code trapId}. Row 7 draws every trap
 * with a plain blinking dot instead, which already covers guard posts.
 * <li>Row 3 (own guard rooms): a room instance's tile bounding rectangle
 * (min/max X/Y) is tracked internally by {@code RoomInstance} but has no
 * public accessor anywhere in the room-information chain.
 * <li>Row 6 (event markers), row 8 (player "special position" flag), row
 * 10 (zoom target): none of this world-level UI state
 * (minimap_design.md §2.8) exists anywhere in this codebase - it's pure
 * design-doc spec, not a port of something already implemented elsewhere.
 * </ul>
 *
 * <p>
 * Two more things worth flagging: design §9 item 1 (the even-tick blink
 * colour for other players' creatures/traps, rows 5/7) was never pinned
 * down by the design doc itself - this picks black, the same colour own
 * creatures blink to, as the more consistent-looking guess between the two
 * candidates it lists. And the door glyph orientation (which rotation maps
 * to "horizontal" vs "vertical") is inferred from
 * {@code DoorsController}'s placement logic, not verified against a
 * screenshot - design §9 item 2 already flags the glyph *shape* itself as
 * unconfirmed, and this adds an orientation-mapping guess on top of that.
 */
public final class MinimapMarkerPainter {

    /**
     * design §5.9's own colour, for the heart-direction line only - the
     * own-heart marker itself (row 1) intentionally does not use this; see
     * {@link #OWN_HEART_RING_COLOUR}.
     */
    private static final int HEART_LINE_COLOUR = 0xFFFF9696;

    /**
     * Row 1's own-heart marker: a light blue ring, not design's literal
     * pink filled disc - a deliberate deviation, not a bug or a guess.
     */
    private static final int OWN_HEART_RING_COLOUR = 0xFFADD8E6;

    private static final int BLACK = 0xFF000000;

    private final EntityData entityData;
    private final MinimapPalette palette;
    private final short viewerId;

    private final EntitySet creatures;
    private final EntitySet doors;
    private final EntitySet traps;

    public MinimapMarkerPainter(EntityData entityData, MinimapPalette palette, short viewerId) {
        this.entityData = entityData;
        this.palette = palette;
        this.viewerId = viewerId;
        creatures = entityData.getEntities(Position.class, Owner.class, CreatureComponent.class);
        doors = entityData.getEntities(Position.class, Owner.class, DoorComponent.class);
        traps = entityData.getEntities(Position.class, Owner.class, TrapComponent.class);
    }

    /**
     * Must be called once a frame before {@link #paint}.
     */
    public void update() {
        creatures.applyChanges();
        doors.applyChanges();
        traps.applyChanges();
    }

    public void dispose() {
        creatures.release();
        doors.release();
        traps.release();
    }

    /**
     * @param raster the 128x128 B,G,R raster to draw into, already filled
     * with tile colours
     * @param mapWidth, mapHeight the map size in tiles
     * @param zoom -1 (fit) .. 4 (16px/tile)
     * @param cameraTileX, cameraTileY the camera's look-at tile (zoomed
     * mode only; ignored in fit mode)
     * @param fogOfWarInformation for the "tile explored" visibility checks
     * design §5.8's table uses
     * @param blinkParity the current half of the blink cycle
     * @param dungeonHeartTile the local player's Dungeon Heart tile, or
     * {@code null} if it hasn't been built/located yet
     * @param dungeonHeartReportingDistanceTiles
     * {@code DUNGEON_HEART_REPORTING_DISTANCE_TILES}
     */
    public void paint(byte[] raster, int mapWidth, int mapHeight, int zoom, float cameraTileX, float cameraTileY,
            IFogOfWarInformation fogOfWarInformation, boolean blinkParity, Point dungeonHeartTile,
            float dungeonHeartReportingDistanceTiles) {
        boolean fit = zoom < 0;
        MinimapRasteriser.FitGeometry fitGeometry = fit ? MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight) : null;
        int pixelsPerTile = fit ? 0 : (1 << zoom);
        float pixelsPerTileScale = fit ? fitGeometry.pixelsPerTileScale() : pixelsPerTile;

        // Row 1: own Dungeon Heart.
        if (dungeonHeartTile != null) {
            float px = toPixelX(dungeonHeartTile.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(dungeonHeartTile.y, fit, fitGeometry, cameraTileY, pixelsPerTile);
            int radius = Math.round(dungeonHeartReportingDistanceTiles * pixelsPerTileScale);
            MinimapMarkers.circleOutline(raster, Math.round(px), Math.round(py), radius, OWN_HEART_RING_COLOUR);
        }

        // Row 4: own creatures. Row 5: other players' creatures.
        for (Entity e : creatures) {
            Owner owner = e.get(Owner.class);
            Position position = e.get(Position.class);
            boolean own = owner.ownerId == viewerId;
            if (own) {
                if (entityData.getComponent(e.getId(), InHand.class) != null) {
                    continue; // design §5.8 row 4: not in hand
                }
            } else {
                Point tile = WorldUtils.vectorToPoint(position.position);
                if (!fogOfWarInformation.isExplored(tile)) {
                    continue; // design §5.8 row 5
                }
            }
            float px = toPixelX(position.position.x, fit, fitGeometry, cameraTileX, pixelsPerTile);
            float py = toPixelY(position.position.z, fit, fitGeometry, cameraTileY, pixelsPerTile);
            int colour = MinimapMarkers.blink(wallColour(owner.ownerId), BLACK, blinkParity);
            MinimapMarkers.dot(raster, Math.round(px), Math.round(py), colour);
        }

        // Row 7: all traps (own, or explored - hiddenFromMinimap doesn't
        // exist in this codebase, see this class's own javadoc).
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

        // Row 9: all doors (own, or explored).
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
            // design §5.8 row 9: blink(black, wallColour(owner)) - note the
            // reversed argument order compared to rows 4/5/7's blink(owner
            // colour, black).
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

        // Heart-direction line (design §5.9), zoomed mode only.
        if (!fit && dungeonHeartTile != null) {
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
                    int colour = MinimapMarkers.blink(HEART_LINE_COLOUR, BLACK, blinkParity);
                    MinimapMarkers.line(raster, Math.round(centre), Math.round(centre),
                            Math.round(edge[0]), Math.round(edge[1]), colour);
                }
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
