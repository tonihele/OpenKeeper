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

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Quad;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.IRoomInformation;
import toniarts.openkeeper.game.map.IRoomsInformation;
import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.PlayerNumbers;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * What the full-screen map draws on top of the tile raster, in screen space
 * (so it stays sharp and grows with the resolution):
 * <ul>
 * <li>a small square for every visible creature, in its owner's colour</li>
 * <li>the icon of the room under the cursor, on the room's centre</li>
 * <li>a legend of the players in their colours</li>
 * </ul>
 * Everything respects the fog of war.
 */
public final class FullscreenMapOverlay {

    private static final String FONT_PATH = "Interface/Fonts/Default.fnt";

    // In 640x480 reference pixels, scaled to the screen
    private static final float DOT_SIZE = 3;
    private static final float ROOM_ICON_SIZE = 32;
    private static final float LEGEND_PADDING = 8;
    private static final float LEGEND_TEXT_SIZE = 12;
    private static final float LEGEND_LINE_HEIGHT = 16;

    private final AssetManager assetManager;
    private final IRoomsInformation<? extends IRoomInformation> roomsInformation;
    private final IKwdFile kwdFile;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final IFogOfWarInformation fogOfWar;
    private final MinimapPalette palette;

    private final Node node = new Node("FullscreenMapOverlay");
    private final EntitySet creatures;
    private final EntitySet inHand;
    private final Mesh dotMesh = new Mesh();
    private final Geometry dots = new Geometry("Units", dotMesh);
    private final Geometry roomIcon = new Geometry("RoomIcon", new Quad(1, 1));
    private final Map<Short, Material> roomIconMaterials = new HashMap<>();
    private final List<BitmapText> legendLines = new ArrayList<>();

    /**
     * @param parent the node to draw into
     * @param keepers the players for the legend
     * @param playerName the name to show for a player
     */
    public FullscreenMapOverlay(AssetManager assetManager, Node parent, EntityData entityData, IKwdFile kwdFile,
            IMapDataInformation<? extends IMapTileInformation> mapData, IFogOfWarInformation fogOfWar,
            IRoomsInformation<? extends IRoomInformation> roomsInformation,
            MinimapPalette palette, Collection<Keeper> keepers, Function<Keeper, String> playerName) {
        this.assetManager = assetManager;
        this.roomsInformation = roomsInformation;
        this.kwdFile = kwdFile;
        this.mapData = mapData;
        this.fogOfWar = fogOfWar;
        this.palette = palette;
        creatures = entityData.getEntities(Position.class, Owner.class, CreatureComponent.class);
        inHand = entityData.getEntities(InHand.class);

        Material dotMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        dotMaterial.setBoolean("VertexColor", true);
        dots.setMaterial(dotMaterial);
        dots.setQueueBucket(RenderQueue.Bucket.Gui);
        dots.setCullHint(CullHint.Always); // until there is something to show
        node.attachChild(dots);

        roomIcon.setQueueBucket(RenderQueue.Bucket.Gui);
        roomIcon.setCullHint(CullHint.Always);
        roomIcon.setLocalTranslation(0, 0, 1);
        node.attachChild(roomIcon);

        BitmapFont font = assetManager.loadFont(FONT_PATH);
        for (Keeper keeper : keepers) {
            if (keeper.getId() < Player.KEEPER1_ID) {
                continue; // not a player slot, i.e. the heroes and the neutral
            }
            BitmapText line = new BitmapText(font);
            line.setText(playerName.apply(keeper));
            line.setColor(toColor(palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE + PlayerNumbers.playerNumber(keeper.getId())]));
            line.setLocalTranslation(0, 0, 2);
            node.attachChild(line);
            legendLines.add(line);
        }

        parent.attachChild(node);
    }

    /**
     * Call once a frame while the map is open
     *
     * @param hoverTile the fractional tile coordinates under the cursor, or
     * {@code null}
     * @param rotationPhase advances by one every raster rebuild, drives the
     * colour of neutral creatures
     */
    public void update(MapLayout layout, Vector2f hoverTile, int rotationPhase) {
        updateLegend(layout);
        updateDots(layout, rotationPhase);
        updateRoomIcon(layout, hoverTile);
    }

    private void updateLegend(MapLayout layout) {
        MapLayout.Rect legend = layout.getLegend();
        float scale = layout.getScale();
        for (int i = 0; i < legendLines.size(); i++) {
            BitmapText line = legendLines.get(i);
            line.setSize(LEGEND_TEXT_SIZE * scale);
            // Text hangs from its position, and a player always keeps their line, even when others are eliminated
            line.setLocalTranslation(legend.x() + LEGEND_PADDING * scale,
                    layout.getScreenHeight() - (legend.y() + (LEGEND_PADDING + i * LEGEND_LINE_HEIGHT) * scale), 2);
        }
    }

    private void updateDots(MapLayout layout, int rotationPhase) {
        creatures.applyChanges();
        inHand.applyChanges();

        float half = Math.max(1f, DOT_SIZE * layout.getScale() / 2f);
        List<Dot> visible = new ArrayList<>();
        for (Entity e : creatures) {
            Vector2f tile = MinimapCoordinates.worldToTile(e.get(Position.class).position);
            if (inHand.containsId(e.getId()) || !fogOfWar.isExplored(new Point((int) tile.x, (int) tile.y))) {
                continue; // carried by a player, or not seen
            }
            Vector2f screen = layout.tileToScreen(tile.x, tile.y, mapData.getWidth(), mapData.getHeight());
            visible.add(new Dot(screen.x, layout.getScreenHeight() - screen.y, unitColour(e.get(Owner.class).ownerId, rotationPhase)));
        }

        dotMesh.clearBuffer(Type.Position);
        dotMesh.clearBuffer(Type.Color);
        dotMesh.clearBuffer(Type.Index);
        dots.setCullHint(visible.isEmpty() ? CullHint.Always : CullHint.Inherit);
        if (visible.isEmpty()) {
            return;
        }

        // One quad per creature
        float[] vertices = new float[visible.size() * 4 * 3];
        float[] colours = new float[visible.size() * 4 * 4];
        int[] indices = new int[visible.size() * 6];
        float[][] corners = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
        for (int i = 0; i < visible.size(); i++) {
            Dot dot = visible.get(i);
            for (int corner = 0; corner < 4; corner++) {
                int vertex = i * 4 + corner;
                vertices[vertex * 3] = dot.x + corners[corner][0];
                vertices[vertex * 3 + 1] = dot.y + corners[corner][1];
                colours[vertex * 4] = dot.colour.r;
                colours[vertex * 4 + 1] = dot.colour.g;
                colours[vertex * 4 + 2] = dot.colour.b;
                colours[vertex * 4 + 3] = 1f;
            }
            System.arraycopy(new int[]{i * 4, i * 4 + 1, i * 4 + 2, i * 4, i * 4 + 2, i * 4 + 3}, 0, indices, i * 6, 6);
        }
        dotMesh.setBuffer(Type.Position, 3, vertices);
        dotMesh.setBuffer(Type.Color, 4, colours);
        dotMesh.setBuffer(Type.Index, 3, indices);
        dotMesh.updateBound();
        dotMesh.updateCounts();
    }

    /**
     * A creature to draw, at its screen position (jME origin)
     */
    private record Dot(float x, float y, ColorRGBA colour) {

    }

    /**
     * The owner's darker (wall) colour
     */
    private ColorRGBA unitColour(short ownerId, int rotationPhase) {
        if (ownerId == Player.NEUTRAL_PLAYER_ID) {
            return toColor(MinimapMarkers.neutralRotationColour(rotationPhase));
        }
        return toColor(palette.rawArgb()[MapColourClass.OWNED_SOLID_BASE + PlayerNumbers.playerNumber(ownerId)]);
    }

    /**
     * Shows the icon of the room under the cursor, on the room's centre
     */
    private void updateRoomIcon(MapLayout layout, Vector2f hoverTile) {
        roomIcon.setCullHint(CullHint.Always);
        if (hoverTile == null) {
            return;
        }
        EntityId roomEntity = mapData.getTile((int) hoverTile.x, (int) hoverTile.y).getRoomId();
        IRoomInformation room = roomEntity != null ? roomsInformation.getRoomInformation(roomEntity) : null;
        if (room == null || room.isRemoved() || !fogOfWar.isExplored(room.getLocation())) {
            return;
        }
        Point location = room.getLocation();
        Material material = getRoomIconMaterial(room.getRoomId());
        if (material == null) {
            return;
        }

        float size = ROOM_ICON_SIZE * layout.getScale();
        Vector2f screen = layout.tileToScreen(location.x + 0.5f, location.y + 0.5f, mapData.getWidth(), mapData.getHeight());
        roomIcon.setMaterial(material);
        roomIcon.setLocalScale(size, size, 1);
        roomIcon.setLocalTranslation(screen.x - size / 2f, layout.getScreenHeight() - screen.y - size / 2f, 1);
        roomIcon.setCullHint(CullHint.Inherit);
    }

    /**
     * @return the material with the room's icon, or {@code null} if it has none
     */
    private Material getRoomIconMaterial(short roomId) {
        if (!roomIconMaterials.containsKey(roomId)) {
            roomIconMaterials.put(roomId, loadRoomIconMaterial(roomId));
        }
        return roomIconMaterials.get(roomId);
    }

    private Material loadRoomIconMaterial(short roomId) {
        Room room = kwdFile.getRoomById(roomId);
        if (room == null || room.getGuiIcon() == null) {
            return null;
        }
        try {
            Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setTexture("ColorMap", assetManager.loadTexture(AssetUtils.getCanonicalAssetKey(
                    AssetsConverter.TEXTURES_FOLDER + File.separator + room.getGuiIcon().getName() + ".png")));
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            return material;
        } catch (AssetNotFoundException e) {
            return null;
        }
    }

    private static ColorRGBA toColor(int argb) {
        return new ColorRGBA(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, 1f);
    }

    public void dispose() {
        creatures.release();
        inHand.release();
        node.removeFromParent();
    }
}
