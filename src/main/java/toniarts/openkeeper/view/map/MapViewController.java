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
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
 import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.*;
import com.jme3.texture.Texture;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.*;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Color;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.control.TorchControl;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.lighting.IFogOfWarQuery;
import toniarts.openkeeper.view.lighting.LightConstants;
import toniarts.openkeeper.view.lighting.LightRules;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.map.WallSection.WallDirection;
import toniarts.openkeeper.view.map.construction.RoomConstructor;
import toniarts.openkeeper.view.map.construction.SingleQuadConstructor;
import toniarts.openkeeper.view.map.construction.WaterConstructor;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * Loads whole maps, and handles the maps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class MapViewController implements ILoader<IKwdFile> {

    private static final Logger logger = System.getLogger(MapViewController.class.getName());

    /**
     * Used where no fog of war applies (main menu map preview, model viewer):
     * everything is simply always visible.
     */
    private static final IFogOfWarInformation ALWAYS_VISIBLE = new IFogOfWarInformation() {
        @Override
        public boolean isVisible(Point p) {
            return true;
        }

        @Override
        public boolean isExplored(Point p) {
            return true;
        }

        @Override
        public boolean isPerceived(Point p) {
            return true;
        }

        @Override
        public boolean isHighlightable(Point p) {
            return true;
        }

        @Override
        public boolean isPendingTagged(Point p) {
            return false;
        }
    };

    public final static ColorRGBA COLOR_FLASH = new ColorRGBA(0.8f, 0, 0, 1);
    /**
     * The original's tile highlight (tile_tagging.md section 4): a base colour
     * added to the tile's lighting, plus a flat additive overlay pass. Tagged =
     * (128, 128, 200) and the BlueTexture (63, 64, 255); flashing = pure red
     * and the RedTexture (255, 64, 65).
     */
    private final static ColorRGBA TAG_BASE = new ColorRGBA(128 / 255f, 128 / 255f, 200 / 255f, 1);
    private final static ColorRGBA TAG_OVERLAY = new ColorRGBA(63 / 255f, 64 / 255f, 1f, 1);
    private final static ColorRGBA FLASH_BASE = new ColorRGBA(1f, 0f, 0f, 1);
    private final static ColorRGBA FLASH_OVERLAY = new ColorRGBA(1f, 64 / 255f, 65 / 255f, 1);
    private final static int PAGE_SQUARE_SIZE = 8; // Divide the terrain to square "pages"
    private final static int FLOOR_INDEX = 0;
    private final static int WALL_INDEX = 1;
    private final static int TOP_INDEX = 2;
    private final static String MAP_NODE = "Map";
    private final static String TERRAIN_NODE = "Terrain";
    private final static String ROOM_NODE = "Rooms";
    private static final List<WallDirection> NO_TORCH_DIRECTIONS = List.of();
    private static final List<WallDirection> HORIZONTAL_TORCH_DIRECTIONS = List.of(WallDirection.NORTH, WallDirection.SOUTH);
    private static final List<WallDirection> VERTICAL_TORCH_DIRECTIONS = List.of(WallDirection.WEST, WallDirection.EAST);
    private static final List<WallDirection> ALL_TORCH_DIRECTIONS = List.of(WallDirection.NORTH, WallDirection.WEST,
            WallDirection.SOUTH, WallDirection.EAST);
    /**
     * Terrain type 38, "Claim Scan" -- the stand-in for a tile the local
     * player has perceived but not yet explored, when the tile is owned or
     * a room (lighting_design.md §6.3, claim_scan_terrain.md).
     */
    private static final short CLAIM_SCAN_TERRAIN_ID = 38;
    /**
     * Room ids the portal light (lighting_design.md §6.5) is hard-coded
     * to -- {@code KwdFile.ROOM_PORTAL_ID} is private, so this is its own
     * copy rather than reaching into that class.
     */
    private static final short ROOM_PORTAL_ID = 3;
    private static final short ROOM_HERO_PORTAL_ID = 25;
    private final boolean torchesEnabled;
    private List<Node> pages;
    private final IKwdFile kwdFile;
    private Node map;
    private final AssetManager assetManager;
    private final IMapInformation mapClientService;
    private final IFogOfWarInformation fogOfWarInformation;
    private Terrain unexploredPlaceholderTerrain;
    private Terrain claimScanTerrain;
    private boolean claimScanTerrainResolved;
    private Node roomsNode;
    private final short playerId;
    private final Set<Point> flashedTiles = new HashSet<>();
    private final List<EntityInstance<Terrain>> waterBatches = new ArrayList<>(); // Lakes and rivers
    private final List<EntityInstance<Terrain>> lavaBatches = new ArrayList<>(); // Lakes and rivers, but hot
    private Spatial waterSurface; // Currently attached merged water mesh, if any
    private Spatial lavaSurface; // Currently attached merged lava mesh, if any
    private final Map<Point, RoomInstance> roomCoordinates = new HashMap<>(); // A quick glimpse whether room at specific coordinates is already "found"
    private final Map<RoomInstance, Spatial> roomNodes = new HashMap<>(); // Room instances by node
    private final Map<Point, Thing.Room> roomThings = new HashMap<>();
    private final Map<RoomInstance, RoomConstructor> roomActuals = new HashMap<>(); // Rooms by room constructor
    private final Map<Point, EntityInstance<Terrain>> terrainBatchCoordinates = new HashMap<>(); // A quick glimpse whether terrain batch at specific coordinates is already "found"
    private final Map<String, Material> randomTextureMaterials = new HashMap<>(); // Alternative terrain materials by asset name, configured once and reused
    private final Map<Node, List<Object>> pageBatchRegionKeys = new IdentityHashMap<>(); // Region keys registered per page, removed on re-batch
    private final LightRules lightRules;

    protected MapViewController(AssetManager assetManager, IKwdFile kwdFile, IMapInformation mapClientService, short playerId) {
        this(assetManager, kwdFile, mapClientService, ALWAYS_VISIBLE, playerId, true);
    }

    protected MapViewController(AssetManager assetManager, IKwdFile kwdFile, IMapInformation mapClientService,
            IFogOfWarInformation fogOfWarInformation, short playerId) {
        this(assetManager, kwdFile, mapClientService, fogOfWarInformation, playerId, true);
    }

    protected MapViewController(AssetManager assetManager, IKwdFile kwdFile, IMapInformation mapClientService,
            short playerId, boolean torchesEnabled) {
        this(assetManager, kwdFile, mapClientService, ALWAYS_VISIBLE, playerId, torchesEnabled);
    }

    protected MapViewController(AssetManager assetManager, IKwdFile kwdFile, IMapInformation mapClientService,
            IFogOfWarInformation fogOfWarInformation, short playerId, boolean torchesEnabled) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.mapClientService = mapClientService;
        this.fogOfWarInformation = fogOfWarInformation;
        this.playerId = playerId;
        this.torchesEnabled = torchesEnabled;

        // Lighting consults the same local-viewer fog as the renderer
        // (cursor colour, design 7.3; terrain-light selection, 6.3)
        this.lightRules = new LightRules(new IFogOfWarQuery() {
            @Override
            public boolean isExplored(Point tile) {
                return fogOfWarInformation.isExplored(tile);
            }

            @Override
            public boolean isPerceived(Point tile) {
                return fogOfWarInformation.isPerceived(tile);
            }
        });
    }

    public LightRules getLightRules() {
        return lightRules;
    }

    @Override
    public Spatial load(AssetManager assetManager, IKwdFile object) {

        long startTime = System.nanoTime();
        
        lightRules.initAmbientMap(getMapData().getWidth(), getMapData().getHeight());

        //Create a root
        map = new Node(MAP_NODE);
        Node terrain = new Node(TERRAIN_NODE);
        generatePages(terrain);
        roomsNode = new Node(ROOM_NODE);
        terrain.attachChild(roomsNode);

        // Go through the fixed rooms and construct them
        // We might not need the room list on the client ever, we can draw them without
        for (Thing.Room room : kwdFile.getThings(Thing.Room.class)) {
            Point p = new Point(room.getPosX(), room.getPosY());
            handleRoom(p, kwdFile.getRoomByTerrain(getMapData().getTile(p).getTerrainId()), room);
        }

        // Go through the map
        int index = 0;
        int tilesCount = getMapData().getSize();

        for (IMapTileInformation tile : getMapData()) {

            try {
                handleTile(tile, terrain);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Failed to handle tile at " + tile.getLocation() + "!", e);
            }

            // Update progress
            updateProgress((float) ++index / tilesCount);
        }

        // Batch the terrain pages
        for (Node page : pages) {
            ((BatchNode) page.getChild(FLOOR_INDEX)).batch();
            ((BatchNode) page.getChild(WALL_INDEX)).batch();
            ((BatchNode) page.getChild(TOP_INDEX)).batch();
            registerPageBatchRegions(page);
        }
        map.attachChild(terrain);

        // Create the water and lava surfaces
        refreshWaterAndLavaSurfaces(true, true);

        long loadTimeMs = (System.nanoTime() - startTime) / 1_000_000L;
        logger.log(Level.INFO, "Map {0} loaded in {1} ms", new Object[]{object.getGameLevel().getName(), loadTimeMs});

        return map;
    }

    public IMapDataInformation<IMapTileInformation> getMapData() {
        return mapClientService.getMapData();
    }

    /**
     * Resolves the terrain to render for a tile. An unexplored tile normally
     * renders as generic, undifferentiated solid rock - regardless of what
     * it actually is - matching the classic Dungeon Keeper look: what you
     * haven't dug into yet reads as plain rock, diggable like any other,
     * until you actually reveal it. The one exception is
     * {@link #isClaimScanTile(IMapTileInformation)}: a perceived-but-
     * unexplored tile that is owned or a room substitutes the Claim Scan
     * terrain instead, so it still looks like anonymous rock but glows
     * (handleTerrainLight) to signal something is there.
     */
    private Terrain getTerrain(IMapTileInformation tile) {
        if (!fogOfWarInformation.isVisible(tile.getLocation())) {
            if (isClaimScanTile(tile)) {
                Terrain claimScan = getClaimScanPlaceholderTerrain();
                if (claimScan != null) {
                    return claimScan;
                }
            }
            Terrain placeholder = getUnexploredPlaceholderTerrain();
            if (placeholder != null) {
                return placeholder;
            }
        }
        return kwdFile.getTerrain(tile.getTerrainId());
    }

    /**
     * claim_scan_terrain.md §4/§6.3: a tile the local player has perceived
     * (but not explored) shows the Claim Scan stand-in instead of plain rock
     * when it's owned by someone, or its real terrain is a room - i.e. when
     * the rock placeholder would otherwise be hiding a claimed area.
     */
    private boolean isClaimScanTile(IMapTileInformation tile) {
        if (!fogOfWarInformation.isPerceived(tile.getLocation())) {
            return false;
        }
        if (tile.getOwnerId() != Player.NEUTRAL_PLAYER_ID) {
            return true;
        }
        return kwdFile.getTerrain(tile.getTerrainId()).getFlags().contains(Terrain.TerrainFlag.ROOM);
    }

    private Terrain getClaimScanPlaceholderTerrain() {
        if (!claimScanTerrainResolved) {
            claimScanTerrain = kwdFile.getTerrain(CLAIM_SCAN_TERRAIN_ID);
            claimScanTerrainResolved = true;
        }
        return claimScanTerrain;
    }

    private Terrain getUnexploredPlaceholderTerrain() {
        if (unexploredPlaceholderTerrain == null) {
            unexploredPlaceholderTerrain = findUnexploredPlaceholderTerrain();
        }
        return unexploredPlaceholderTerrain;
    }

    /**
     * Finds the plain, undug rock terrain (solid, not ownable, not
     * impenetrable, not a room, no gold) to stand in for anything unexplored.
     * Explicitly avoids any special/neighbour-aware construction type (quad,
     * water) - the placeholder must always resolve to the simple, static
     * "just load this one model" path in {@link #handleTop}, never the
     * auto-tiling machinery meant for claimed surfaces.
     */
    private Terrain findUnexploredPlaceholderTerrain() {
        Terrain fallback = null;
        for (Terrain candidate : kwdFile.getTerrainList()) {
            if (!candidate.getFlags().contains(Terrain.TerrainFlag.SOLID)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.ROOM)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.WATER)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.LAVA)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)
                    || candidate.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)
                    || candidate.getGoldValue() > 0) {
                continue;
            }
            if (fallback == null) {
                fallback = candidate;
            }
            if ("rock".equalsIgnoreCase(candidate.getName())) {
                return candidate;
            }
        }
        return fallback;
    }

    /**
     * (Re)builds the merged water and/or lava surface mesh from the current
     * {@link #waterBatches}/{@link #lavaBatches} and attaches it, replacing
     * whatever was attached before. {@code Water.construct(...)} merges an
     * entire batch list into one mesh, so this must re-run in full whenever
     * fog reveals a new batch (see {@link #findTerrainBatch}) - there's no
     * incremental variant to append to.
     */
    private void refreshWaterAndLavaSurfaces(boolean waterChanged, boolean lavaChanged) {
        if (waterChanged) {
            waterSurface = replaceSurface(waterSurface, waterBatches);
        }
        if (lavaChanged) {
            lavaSurface = replaceSurface(lavaSurface, lavaBatches);
        }
    }

    private Spatial replaceSurface(Spatial oldSurface, List<EntityInstance<Terrain>> batches) {
        if (oldSurface != null) {
            oldSurface.removeFromParent();
        }
        if (batches.isEmpty()) {
            return null;
        }
        Spatial surface = Water.construct(assetManager, batches);
        if (surface instanceof Geometry geometry && AssetUtils.isDkTerrainMaterial(geometry.getMaterial())) {
            lightRules.registerRegionMaterials(surface, Vector3f.ZERO, 0f, List.of(geometry.getMaterial()));
        }
        map.attachChild(surface);
        return surface;
    }

    /**
     * Update the selected tiles (and neighbouring tiles if needed)
     *
     * @param points tile coordinates to update
     */
    public void updateTiles(Point... points) {
        Set<Point> pointsToUpdate = new HashSet<>();

        // FIXME: This is really quite heavy and unneeded, just a quick "fix"
        // If this touches any rooms, reconstruct them and add the neighbouring tiles to update list
        for (Point point : points) {
            pointsToUpdate.add(point);
            RoomInstance roomInstance = roomCoordinates.get(point);
            if (roomInstance != null) {
                pointsToUpdate.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(getMapData(), point, true)));
                for (Point roomCoordinate : roomInstance.getCoordinates()) {
                    pointsToUpdate.addAll(Arrays.asList(WorldUtils.getSurroundingTiles(getMapData(), roomCoordinate, true)));
                    pointsToUpdate.add(roomCoordinate);
                }
                removeRoomInstances(roomInstance);
            } else {
                Point[] surroundingTiles = WorldUtils.getSurroundingTiles(getMapData(), point, true);
                pointsToUpdate.addAll(Arrays.asList(surroundingTiles));

                // Also remove the surrounding rooms, if this room needs to be merged to other rooms
                for (Point surroundingPoint : surroundingTiles) {
                    roomInstance = roomCoordinates.get(surroundingPoint);
                    if (roomInstance != null) {
                        removeRoomInstances(roomInstance);
                    }
                }
            }
        }

        // Reconstruct all tiles in the area
        int waterBatchesBefore = waterBatches.size();
        int lavaBatchesBefore = lavaBatches.size();
        Set<BatchNode> nodesNeedBatching = new HashSet<>();
        Node terrainNode = (Node) map.getChild(TERRAIN_NODE);
        for (Point point : pointsToUpdate) {
            IMapTileInformation tile = getMapData().getTile(point);

            // Reconstruct and mark for patching
            // The tile node needs to created anew, somehow the BatchNode just doesn't get it if I remove children from subnode
            Node pageNode = getPageNode(point, terrainNode);
            Node tileNode = getTileNode(point, (Node) pageNode.getChild(FLOOR_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(FLOOR_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(FLOOR_INDEX));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(WALL_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(WALL_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(WALL_INDEX));
            }
            tileNode = getTileNode(point, (Node) pageNode.getChild(TOP_INDEX));
            if (!tileNode.getChildren().isEmpty()) {
                tileNode.removeFromParent();
                ((BatchNode) pageNode.getChild(TOP_INDEX)).attachChildAt(new Node(tileNode.getName()), getTileNodeIndex(point));
                nodesNeedBatching.add((BatchNode) pageNode.getChild(TOP_INDEX));
            }

            // Reconstruct
            handleTile(tile, (Node) map.getChild(TERRAIN_NODE));
        }

        // Batch
        for (BatchNode batchNode : nodesNeedBatching) {
            batchNode.batch();
            // Re-batching creates new batched geometries/materials in place,
            // so the page's region registration must be refreshed
            if (batchNode.getParent() instanceof Node page) {
                registerPageBatchRegions(page);
            }
        }

        // A newly fog-revealed water/lava batch needs its merged surface rebuilt
        refreshWaterAndLavaSurfaces(waterBatches.size() != waterBatchesBefore,
                lavaBatches.size() != lavaBatchesBefore);
    }

    /**
     * Sets the right material to tile (selected / decayed...)
     *
     * @param node
     */
    private void setTileMaterialToGeometries(final IMapTileInformation tile, final Node node) {

        // Change the material on geometries
        Terrain terrain = getTerrain(tile);
        final boolean tagged = tile.isSelected(playerId) || fogOfWarInformation.isPendingTagged(tile.getLocation());
        if (!isFlashing(tile) && !tagged
                && !terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {
            return;
        }

        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry)) {
                    return;
                }

                Geometry geometry = (Geometry) spatial;

                // For RANDOM_TEXTURE terrain (rock/gold/gems), setRandomTexture() has
                // already replaced this geometry's material with one from
                // randomTextureMaterials - a cache explicitly shared and reused across
                // every tile with the same texture variant. Mutating that material in
                // place (as below) would tint every other tile sharing the cached
                // instance, not just this one. Clone it into a tile-private material
                // before making any per-tile change.
                // A geometry still associated with its page's BatchNode (already
                // merged into a batch mesh) throws on setMaterial() - it must be
                // ungrouped first. This can happen here even on a supposedly-fresh
                // rebuild, so check unconditionally rather than assuming it never is.
                if (geometry.isGrouped()) {
                    geometry.unassociateFromGroupNode();
                }
                Material material = geometry.getMaterial().clone();
                geometry.setMaterial(material);

                // Decay
                if (terrain.getFlags().contains(Terrain.TerrainFlag.DECAY)) {

                    List<String> textures = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES);
                    if (textures != null) {

                        // FIXME: This doesn't sit well with the material thinking (meaning we produce the actual material files)
                        // Now we have a random starting texture...
                        int textureIndex = Math.round((textures.size() - 1) * (1 - tile.getHealthPercent() / 100f));
                        String diffuseTexture = textures.get(textureIndex);
                        String diffuseTextureKey = AssetUtils.getCanonicalAssetKey(diffuseTexture);
                        String currentTexture = material.getTextureParam("DiffuseMap").getTextureValue().getKey().getName();
                        if(!diffuseTextureKey.equals(currentTexture)) {
                            try {
                                Texture texture = assetManager.loadTexture(new TextureKey(diffuseTextureKey, false));
                                material.setTexture("DiffuseMap", texture);

                                AssetUtils.assignMapsToMaterial(assetManager, material);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Error applying decay texture: {0} to {1} terrain! ({2})", new Object[]{diffuseTexture, terrain.getName(), e.getMessage()});
                            }
                        }
                    }
                }
                // DkTerrain's Ambient is replaced by the per-tile ambient map,
                // so the highlight goes through its own additive Tint instead
                boolean dkTerrain = AssetUtils.isDkTerrainMaterial(material);
                if (dkTerrain) {
                    // A flashing tile overrides a tagged one (as in the original)
                    if (isFlashing(tile)) {
                        material.setColor("Tint", FLASH_BASE);
                        material.setColor("TintOverlay", FLASH_OVERLAY);
                    } else if (tagged) {
                        material.setColor("Tint", TAG_BASE);
                        material.setColor("TintOverlay", TAG_OVERLAY);
                    }
                } else if (isFlashing(tile)) {
                    material.setColor("Ambient", COLOR_FLASH);
                    // DkTerrain (jME's CommonMat-parameter-free def) has no
                    // UseMaterialColors; guard like AssetUtils does so the flag
                    // is only flipped on defs that declare it
                    if (material.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                        material.setBoolean("UseMaterialColors", true);
                    }
                }
                if (!dkTerrain && tagged) {
                    material.setColor("Ambient", TAG_BASE);
                    if (material.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
                        material.setBoolean("UseMaterialColors", true);
                    }
                }

            }

        });
    }

    private boolean isFlashing(final IMapTileInformation tile) {
        return flashedTiles.contains(tile.getLocation());
    }

    /**
     * Generate the page nodes
     *
     * @param root where to generate pages on
     */
    private void generatePages(Node root) {
        pages = new ArrayList<>(((int) Math.ceil(getMapData().getHeight() / (float) PAGE_SQUARE_SIZE))
                * ((int) Math.ceil(getMapData().getWidth() / (float) PAGE_SQUARE_SIZE)));
        for (int y = 0; y < (int) Math.ceil(getMapData().getHeight() / (float) PAGE_SQUARE_SIZE); y++) {
            for (int x = 0; x < (int) Math.ceil(getMapData().getWidth() / (float) PAGE_SQUARE_SIZE); x++) {
                Node page = new Node(x + "_" + y);

                // Create batch nodes for ceiling, floor and walls
                BatchNode floor = new BatchNode("floor");
                floor.setShadowMode(RenderQueue.ShadowMode.Receive); // Floors don't cast
                generateTileNodes(floor, x, y);
                page.attachChild(floor);
                BatchNode wall = new BatchNode("wall");
                wall.setShadowMode(RenderQueue.ShadowMode.CastAndReceive); // Walls cast and receive shadows
                generateTileNodes(wall, x, y);
                page.attachChild(wall);
                BatchNode ceiling = new BatchNode("ceiling");
                ceiling.setShadowMode(RenderQueue.ShadowMode.Off); // No lights above ceilings
                generateTileNodes(ceiling, x, y);
                page.attachChild(ceiling);

                pages.add(page);
                root.attachChild(page);
            }
        }
    }

    /**
     * Create tile nodes inside a page
     *
     * @param pageBatch the page
     * @param pageX page x
     * @param pageY page y
     */
    private void generateTileNodes(BatchNode pageBatch, int pageX, int pageY) {
        for (int y = 0; y < PAGE_SQUARE_SIZE; y++) {
            for (int x = 0; x < PAGE_SQUARE_SIZE; x++) {
                pageBatch.attachChild(new Node((x + pageX * PAGE_SQUARE_SIZE) + "_" + (y + pageY * PAGE_SQUARE_SIZE)));
            }
        }
    }

    /**
     * Registers the batched geometry of a whole page as lighting regions
     */
    private void registerPageBatchRegions(Node page) {
        if (page == null) {
            return;
        }
        List<Object> keys = pageBatchRegionKeys.get(page);
        if (keys != null) {
            for (Object key : keys) {
                lightRules.unregisterRegionMaterials(key);
            }
        }
        keys = new ArrayList<>();

        String[] pageCoords = page.getName().split("_");
        if (pageCoords.length < 2) {
            return;
        }
        int pageX;
        int pageY;
        try {
            pageX = Integer.parseInt(pageCoords[0]);
            pageY = Integer.parseInt(pageCoords[1]);
        } catch (NumberFormatException e) {
            return;
        }
        float minX = pageX * PAGE_SQUARE_SIZE - 0.5f;
        float minZ = pageY * PAGE_SQUARE_SIZE - 0.5f;
        float maxX = (pageX + 1) * PAGE_SQUARE_SIZE + 0.5f;
        float maxZ = (pageY + 1) * PAGE_SQUARE_SIZE + 0.5f;
        Vector3f[] pageBounds = new com.jme3.math.Vector3f[]{
            new com.jme3.math.Vector3f(minX, 1f, minZ),
            new com.jme3.math.Vector3f(maxX, 2f, maxZ)
        };
        // The merged batch geometries are the batch node's direct Geometry
        Map<Material, Vector3f[]> materialBounds = new IdentityHashMap<>();
        for (int i = 0; i <= TOP_INDEX; i++) {
            Spatial batch = page.getChild(i);
            if (!(batch instanceof BatchNode batchNode)) {
                continue;
            }
            for (Spatial child : batchNode.getChildren()) {
                if (!(child instanceof Geometry geometry) || geometry.getMesh() == null) {
                    continue;
                }
                Material material = geometry.getMaterial();
                // Only DkTerrain materials take a per-vertex light array
                // (NumLights etc.); DkFlat things are lit CPU-side per thing
                if (material == null || !AssetUtils.isDkTerrainMaterial(material)) {
                    continue;
                }
                materialBounds.putIfAbsent(material, pageBounds);
            }
        }
        int regionIndex = 0;
        for (Map.Entry<Material, Vector3f[]> entry : materialBounds.entrySet()) {
            Vector3f center = entry.getValue()[0].add(entry.getValue()[1]).multLocal(0.5f);
            float radius = center.distance(entry.getValue()[0]);
            Object key = page.getName() + ":batch:" + regionIndex;
            lightRules.registerRegionMaterials(key, center, radius, List.of(entry.getKey()));
            keys.add(key);
            regionIndex++;
        }
        pageBatchRegionKeys.put(page, keys);
    }

    private Spatial getWallSpatial(IMapTileInformation tile, WallDirection direction) {
        Terrain terrain = getTerrain(tile);
        ArtResource artResource = terrain.getSideResource();
        String modelName = artResource.getName();
        Point p = tile.getLocation();
        IMapTileInformation neigbourTile;
        neigbourTile = switch (direction) {
            case NORTH ->
                getMapData().getTile(p.x, p.y - 1);
            case SOUTH ->
                getMapData().getTile(p.x, p.y + 1);
            case EAST ->
                getMapData().getTile(p.x + 1, p.y);
            default ->
                getMapData().getTile(p.x - 1, p.y); // WEST
        };

        // Check for out of bounds
        if (neigbourTile == null) {
            return loadModel(modelName, artResource);
        }

        // getTerrain() already substitutes the generic rock placeholder for an
        // unexplored neighbour, so it reads as SOLID here exactly like real rock would
        if (getTerrain(neigbourTile).getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            return null;
        }

        if (!(terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS))) {
            return loadModel(modelName, artResource);
        } else if (hasRoomWalls(neigbourTile)) {
            return getRoomWall(neigbourTile, direction);
        }

        return loadModel(modelName, artResource);
    }

    private Spatial getRoomWall(IMapTileInformation tile, WallDirection direction) {
        Point p = tile.getLocation();
        Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
        RoomInstance roomInstance = handleRoom(p, room, null);
        RoomConstructor rc = roomActuals.get(roomInstance);
        return rc.getWallSpatial(p, direction);
    }

    /**
     * Sets random material (from the list) to all the geometries that have been
     * tagged for this in this spatial
     *
     * @param spatial the spatial
     * @param tile the tile
     */
    private void setRandomTexture(final Spatial spatial, final IMapTileInformation tile) {

        // Check the data on geometry, basically we could allow the players to play with different skins
        spatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                List<String> textures = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES);
                if (textures != null) {

                    // The principle is bit wrong, the random texture is tied to the tile, and not material etc.
                    // But it is probably just the tops of few tiles, so...
                    // ROCK TOP.kmf contains 2 texture alternatives while Rock declares 3 textureFrames
                    // in Terrain.kwd. Wrap the third weighted slot to the default texture instead of
                    // trying to load the nonexistent T_Rock_Top2T_Rock_top2_2.j3m material.
                    int tex = Math.floorMod(tile.getRandomTextureIndex(), textures.size());
                    if (tex != 0) { // 0 is the default anyway
                        Geometry g = (Geometry) spatial;
                        Material m = g.getMaterial();
                        String asset = m.getAssetName();

                        // Load new material, but only configure each alternative material once and reuse it
                        String alternativeAsset = asset.substring(0,
                                asset.lastIndexOf(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURE_SUFFIX_SEPARATOR) + 1).concat(tex + ".j3m");
                        Material newMaterial = randomTextureMaterials.get(alternativeAsset);
                        if (newMaterial == null) {
                            AssetInfo newMaterialInfo = assetManager.locateAsset(new AssetKey<>(alternativeAsset));
                            if (newMaterialInfo != null) {
                                try {
                                    newMaterial = assetManager.loadMaterial(newMaterialInfo.getKey().getName());
                                    AssetUtils.assignMapsToMaterial(assetManager, newMaterial);
                                    randomTextureMaterials.put(alternativeAsset, newMaterial);
                                } catch (Exception e) {

                                    logger.log(Level.WARNING, "Failed to load a random texture to terrain id " + tile.getTerrainId() + ", texture index " + tex + "!", e);
                                }
                            }
                        }
                        if (newMaterial != null) {
                            g.setMaterial(newMaterial);
                        }
                    }
                }
            }
        });
    }

    private Spatial loadModel(final String model, final ArtResource artResource) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, artResource);

        return spatial;
    }

    /**
     * Handle single tile from the map, represented by the X & Y coordinates
     *
     * @param tile tile to handle
     * @param root the root node
     */
    private void handleTile(IMapTileInformation tile, Node root) {

        // Get the terrain (an unexplored tile substitutes plain rock here, see getTerrain())
        Terrain terrain = getTerrain(tile);
        Point p = tile.getLocation();
        Node pageNode = getPageNode(p, root);

        // This tile is about to be rebuilt -- drop its old torch/terrain
        // lights and material registrations before (re)creating them below
        // (lighting_design.md §6.2); registerTerrainTile appends, so this
        // also prevents the registry from growing unbounded across rebuilds.
        lightRules.clearTileLights(p);
        lightRules.unregisterTerrainTile(p);

        handleSlabLight(p, terrain, tile.getOwnerId());

        // Per-tile ambient and explored flag into the texture the shader blends across tile borders
        lightRules.setTileAmbient(p.x, p.y, getVertexAmbient(terrain, tile.getOwnerId()),
                fogOfWarInformation.isExplored(p));

        // Torch (see https://github.com/tonihele/OpenKeeper/issues/128)
        if (torchesEnabled && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            handleTorch(tile, pageNode);
        }

        // Room
        boolean isRoomTile = terrain.getFlags().contains(Terrain.TerrainFlag.ROOM);
        RoomInstance roomInstance = null;
        if (isRoomTile) {

            // Construct the actual room
            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            roomInstance = handleRoom(p, room, null);

            // Swap the terrain if this is a bridge
            terrain = kwdFile.getTerrainBridge(tile.getBridgeTerrainType(), room);
            if (terrain == null) {
                return;
            }
        }

        handleTerrainLight(tile, terrain, isRoomTile, roomInstance);

        handleTop(tile, terrain, pageNode);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            handleSide(tile, pageNode, terrain);
        }
    }

    private void handleTorch(IMapTileInformation tile, Node pageNode) {

        // The rooms actually contain the torch model resource, but it is always the same,
        // and sometimes even null and there is still a torch. So I don't think they are used
        // DKII places at most one torch per tile. At corners, the direction order makes the
        // perpendicular wall skip this tile and resume on the next eligible tile in its run.
        for (WallDirection direction : getTorchDirections(tile.getX(), tile.getY())) {
            Point neighbor = switch (direction) {
                case NORTH -> new Point(tile.getX(), tile.getY() - 1);
                case WEST -> new Point(tile.getX() - 1, tile.getY());
                case SOUTH -> new Point(tile.getX(), tile.getY() + 1);
                case EAST -> new Point(tile.getX() + 1, tile.getY());
            };
            Terrain wallTerrain = getTorchTerrain(neighbor.x, neighbor.y);
            if (wallTerrain != null) {
                addTorch(tile, pageNode, direction, wallTerrain);
                return;
            }
        }
    }

    static List<WallDirection> getTorchDirections(int x, int y) {
        boolean horizontal = x % 2 != 0;
        boolean vertical = y % 2 == 0;

        if (horizontal) {
            return vertical ? ALL_TORCH_DIRECTIONS : HORIZONTAL_TORCH_DIRECTIONS;
        }
        return vertical ? VERTICAL_TORCH_DIRECTIONS : NO_TORCH_DIRECTIONS;
    }

    private void addTorch(IMapTileInformation tile, Node pageNode, WallDirection direction, Terrain wallTerrain) {
        String name = "Torch1";
        float height = wallTerrain.getLightHeight();
        float angleY;
        Vector3f position;

        switch (direction) {
            case NORTH -> {
                angleY = -FastMath.HALF_PI;
                position = new Vector3f(0, height, -WorldUtils.TILE_WIDTH / 2);
            }
            case WEST -> {
                angleY = 0;
                position = new Vector3f(-WorldUtils.TILE_WIDTH / 2, height, 0);
            }
            case SOUTH -> {
                angleY = FastMath.HALF_PI;
                position = new Vector3f(0, height, WorldUtils.TILE_WIDTH / 2);
            }
            case EAST -> {
                angleY = FastMath.PI;
                position = new Vector3f(WorldUtils.TILE_WIDTH / 2, height, 0);
            }
            default -> throw new IllegalStateException("Unexpected torch direction: " + direction);
        }

        // Torch colour/intensity/radius (lighting_design.md §6.2): the room's
        // own values if this tile is in a room, else the global defaults.
        int red;
        int green;
        int blue;
        float intensity;
        float radius;
        float lightHeight;
        float maxRadius = LightConstants.TORCH_LIGHT_MAX_RADIUS_TILES;

        // Room wall torches are defined by the room type. Resolve the room directly from
        // terrain because ordinary RoomInstances are registered after torches are handled.
        if (getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            Room room = kwdFile.getRoomByTerrain(tile.getTerrainId());
            // A room without its own torch art falls back to the default mesh
            // (design §6.2); only torchColor == 0 (below) removes torches
            ArtResource torch = room.getTorch();
            if (torch != null && torch.getName() != null && !torch.getName().isEmpty()) {
                name = torch.getName();
            }
            position.y = room.getTorchHeight();
            lightHeight = room.getTorchHeight();

            Color torchColor = room.getTorchColor();
            if (torchColor == null) {
                return;
            }
            // Each TORCH_COLOR_* room flag adds its channel's full value on top of the stored colour
            int torchRed = torchColor.getRed() + (room.getFlags().contains(Room.RoomFlag.TORCH_COLOR_RED) ? LightConstants.TORCH_OVER_BRIGHT_BONUS : 0);
            int torchGreen = torchColor.getGreen() + (room.getFlags().contains(Room.RoomFlag.TORCH_COLOR_GREEN) ? LightConstants.TORCH_OVER_BRIGHT_BONUS : 0);
            int torchBlue = torchColor.getBlue() + (room.getFlags().contains(Room.RoomFlag.TORCH_COLOR_BLUE) ? LightConstants.TORCH_OVER_BRIGHT_BONUS : 0);
            if (torchRed == 0 && torchGreen == 0 && torchBlue == 0) {
                return; // A room with torchColor == 0 has no torches (design §6.2)
            }
            // Only an unowned room shows its own (mostly green) torch colour
            red = torchRed;
            green = torchGreen;
            blue = torchBlue;
            intensity = room.getTorchIntensity() / 128f;
            radius = room.getTorchRadius();
            maxRadius = LightConstants.TORCH_LIGHT_MAX_RADIUS_TILES;
        } else {
            red = LightConstants.getDefaultTorchLightRed(kwdFile);
            green = LightConstants.getDefaultTorchLightGreen(kwdFile);
            blue = LightConstants.getDefaultTorchLightBlue(kwdFile);
            intensity = LightConstants.getDefaultTorchLightIntensity(kwdFile);
            radius = LightConstants.getDefaultTorchLightRadiusTiles(kwdFile);
            lightHeight = LightConstants.getDefaultTorchLightHeightTiles(kwdFile);
        }

        Spatial spatial = AssetUtils.loadModel(assetManager, name, null);
        spatial.addControl(new TorchControl(kwdFile, assetManager, angleY));
        spatial.rotate(0, angleY, 0);
        Vector3f worldPosition = WorldUtils.pointToVector3f(tile.getLocation()).addLocal(position);
        spatial.setLocalTranslation(worldPosition);

        Node parent = (Node) getTileNode(tile.getLocation(), (Node) pageNode.getChild(WALL_INDEX));
        // This tile node can still carry a translation left over from when this
        // exact tile was rendered as SOLID (either genuinely solid terrain, or -
        // just as often - the unexplored fog-rock placeholder while this tile
        // hadn't been explored yet): handleSide() positions a solid tile's wall
        // geometry by translating the WHOLE tile node via
        // AssetUtils.translateToTile(), not by offsetting the geometry itself.
        // A torch is only ever placed once this host tile is confirmed non-solid
        // (real floor terrain), and fog exploration is sticky - it will never
        // become solid again - so it's safe, and necessary, to zero that out
        // before attaching anything positioned in absolute (tile.getLocation())
        // terms here, or the tile's own offset ends up applied twice.
        parent.setLocalTranslation(Vector3f.ZERO);
        parent.attachChild(spatial);

        // Design 6.2: the light sits inside the floor tile, hugging the wall face
        // (the model is placed separately above), and stays small
        lightRules.addTorchLight(tile.getLocation(), getTorchLightPosition(tile.getLocation(), direction, lightHeight),
                red, green, blue, intensity, Math.min(radius, maxRadius));
    }

    /**
     * Where a torch light goes (lighting_design.md &sect;6.2): inside the floor
     * tile {@code floor}, next to the wall face it hangs on. The design gives
     * the offsets in the floor tile's own space (origin in its north-west
     * corner, y growing south): wall to the north (0.5, 0.125), east
     * (0.875, 0.5), south (0.5, 0.875), west (0.125, 0.5). A tile is centred
     * on whole world coordinates here, so the corner is half a tile back.
     *
     * @param floor the floor tile the torch stands in
     * @param direction which side of {@code floor} the wall is on
     * @param height the light height
     * @return the light position in world space
     */
    static Vector3f getTorchLightPosition(Point floor, WallDirection direction, float height) {
        float u;
        float v;
        switch (direction) {
            case NORTH -> {
                u = 0.5f;
                v = 0.125f;
            }
            case EAST -> {
                u = 0.875f;
                v = 0.5f;
            }
            case SOUTH -> {
                u = 0.5f;
                v = 0.875f;
            }
            case WEST -> {
                u = 0.125f;
                v = 0.5f;
            }
            default -> throw new IllegalStateException("Unexpected torch direction: " + direction);
        }
        return new Vector3f(floor.x - 0.5f + u, height, floor.y - 0.5f + v);
    }

    private Terrain getTorchTerrain(int x, int y) {
        IMapTileInformation tile = getMapData().getTile(x, y);
        if (tile == null) {
            return null;
        }
        Terrain terrain = getTerrain(tile);
        return hasTorches(terrain) ? terrain : null;
    }

    /**
     * Design §6.2: a {@code TORCH} terrain still opts out when it declares
     * both a side and a top resource, unless it is impenetrable (no torches on
     * an ordinary diggable wall with full artwork). Invisible on stock data.
     */
    static boolean hasTorches(Terrain terrain) {
        EnumSet<Terrain.TerrainFlag> flags = terrain.getFlags();
        if (!flags.contains(Terrain.TerrainFlag.TORCH)) {
            return false;
        }
        if (flags.contains(Terrain.TerrainFlag.IMPENETRABLE)) {
            return true;
        }
        return !(hasResource(terrain.getSideResource()) && hasResource(terrain.getTopResource()));
    }

    private static boolean hasResource(ArtResource resource) {
        return resource != null && resource.getName() != null && !resource.getName().isEmpty();
    }

    private RoomInstance handleRoom(Point p, Room room, Thing.Room thing) {
        if (roomCoordinates.containsKey(p)) {
            RoomInstance roomInstance = roomCoordinates.get(p);
            return roomInstance;
        }

        RoomInstance roomInstance = new RoomInstance(room, thing != null ? thing : roomThings.get(p));
        findRoom(p, roomInstance, thing);
        findRoomWallSections(roomInstance);

        Spatial roomNode = handleRoom(roomInstance);
        if (roomNode != null) {
            roomsNode.attachChild(roomNode);
        }

        // Add to registry
        roomNodes.put(roomInstance, roomNode);

        // Portal light (lighting_design.md §6.5)
        if (room.getRoomId() == ROOM_PORTAL_ID || room.getRoomId() == ROOM_HERO_PORTAL_ID) {
            Point center = getRoomCenterTile(roomInstance);
            // Tile (x, y) is centred on (x, y) in world space, not (x + 0.5, y + 0.5)
            Vector3f position = new Vector3f(center.x, 0.49f, center.y);
            //lightRules.addPortalLight(roomInstance, position);
        }

        return roomInstance;
    }

    /**
     * The centroid of a room's coordinates, used as its "centre tile"
     * (lighting_design.md §6.5).
     */
    /**
     * The room tile closest to the centroid: for a non-convex room the plain
     * centroid can fall outside the room, but the portal light belongs on a
     * real room tile.
     */
    static Point getRoomCenterTile(RoomInstance roomInstance) {
        Point centroid = getRoomCenter(roomInstance);
        Point best = centroid;
        double bestDistance = Double.MAX_VALUE;
        for (Point coordinate : roomInstance.getCoordinates()) {
            double distance = centroid.distance(coordinate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = coordinate;
            }
        }
        return best;
    }

    private static Point getRoomCenter(RoomInstance roomInstance) {
        long sumX = 0;
        long sumY = 0;
        List<Point> coordinates = roomInstance.getCoordinates();
        for (Point coordinate : coordinates) {
            sumX += coordinate.x;
            sumY += coordinate.y;
        }
        int count = coordinates.size();
        return new Point((int) (sumX / count), (int) (sumY / count));
    }

    /**
     * Handle top construction on the tile
     *
     * @param tile this tile
     * @param terrain DO NOT REMOVE. Need for construct water bed
     * @param pageNode page node
     */
    private void handleTop(IMapTileInformation tile, Terrain terrain, Node pageNode) {

        ArtResource model = terrain.getCompleteResource();
        Point p = tile.getLocation();
        Spatial spatial;
        // For water construction type (lava & water), there are 8 pieces (0-7 suffix) in complete resource
        // And in the top resource there is the actual lava/water
        if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

            // Store the batch instance
            if (!terrainBatchCoordinates.containsKey(p)) {
                EntityInstance<Terrain> entityInstance = new EntityInstance<>(terrain);
                findTerrainBatch(p, entityInstance);
                if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
                    lavaBatches.add(entityInstance);
                } else {
                    waterBatches.add(entityInstance);
                }
            }

            spatial = new WaterConstructor(kwdFile).construct(getMapData(), p.x, p.y, terrain, assetManager, model.getName());

        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            // If this resource is type quad, parse it together. With fixed Hero Lair
            String modelName = (model == null && terrain.getTerrainId() == 35) ? "hero_outpost_floor" : model.getName();
            spatial = new SingleQuadConstructor(kwdFile).construct(getMapData(), p.x, p.y, terrain, assetManager, modelName);

        } else {

            if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                model = terrain.getTopResource();
            }
            spatial = loadModel(model.getName(), model);
        }

        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            setRandomTexture(spatial, tile);
        }

        Node topTileNode;
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(TOP_INDEX));
        } else {
            topTileNode = getTileNode(p, (Node) pageNode.getChild(FLOOR_INDEX));
        }

        topTileNode.attachChild(spatial);
        applyTerrainLighting(topTileNode, terrain, tile.getOwnerId());
        // setTileMaterialToGeometries (tag/flash/decay tint) may clone a
        // fresh Material past this point - register whatever's actually on
        // the geometries afterwards, not applyTerrainLighting's now possibly
        // stale list, or LightRules would keep pushing light updates onto an
        // orphaned Material no longer attached to anything on screen.
        setTileMaterialToGeometries(tile, topTileNode);
        lightRules.registerTerrainTile(p, collectMaterials(topTileNode));
        AssetUtils.translateToTile(topTileNode, p);
    }

    private void handleSide(IMapTileInformation tile, Node pageNode, Terrain terrain) {
        Point p = tile.getLocation();
        Node sideTileNode = getTileNode(p, (Node) pageNode.getChild(WALL_INDEX));

        for (WallDirection direction : WallDirection.values()) {
            Spatial wall = getWallSpatial(tile, direction);
            if (wall != null) {
                wall.rotate(0, direction.getAngle(), 0);
                sideTileNode.attachChild(wall);
            }
        }

        applyTerrainLighting(sideTileNode, terrain, tile.getOwnerId());
        setTileMaterialToGeometries(tile, sideTileNode);
        lightRules.registerTerrainTile(p, collectMaterials(sideTileNode));
        AssetUtils.translateToTile(sideTileNode, p);
    }

    public void flashTile(boolean enabled, List<Point> points) {
        if (enabled) {
            flashedTiles.addAll(points);
        } else {
            flashedTiles.removeAll(points);
        }

        updateTiles(points.toArray(Point[]::new));
    }

    /**
     * Get the terrain tile node
     *
     * @param p the tile coordinates
     * @param page the page node
     * @return tile node
     */
    private Node getTileNode(Point p, Node page) {
        return (Node) page.getChild(getTileNodeIndex(p));
    }

    /**
     * Get index for the tile node, where it should be
     *
     * @param p the tile coordinates
     * @return the index inside the page
     */
    private int getTileNodeIndex(Point p) {
        int tileX = p.x - ((int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        int tileY = p.y - ((int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE)) * PAGE_SQUARE_SIZE;
        return tileY * PAGE_SQUARE_SIZE + tileX;
    }

    /**
     * Get the terrain "page" we are on
     *
     * @param p the tile coordinates
     * @param root the root node
     * @return page node
     */
    protected Node getPageNode(Point p, Node root) {
        int pageX = (int) Math.floor(p.x / (float) PAGE_SQUARE_SIZE);
        int pageY = (int) Math.floor(p.y / (float) PAGE_SQUARE_SIZE);

        // Get the page index
        int index = pageX;
        if (pageY > 0) {
            int pagesPerRow = (int) Math.ceil(getMapData().getWidth() / (float) PAGE_SQUARE_SIZE);
            index += pagesPerRow * pageY;
        }
        return (Node) root.getChild(index);
    }

    /**
     * Checks if this terrain piece is actually a room and the room type has
     * walls
     *
     * @param tile the terrain tile
     * @return true if this is a room and it has its own walls
     */
    private boolean hasRoomWalls(IMapTileInformation tile) {
        Terrain terrain = getTerrain(tile);
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            Room room = kwdFile.getRoomByTerrain(terrain.getTerrainId());
            return hasRoomWalls(room);
        }
        return false;
    }

    public static boolean hasRoomWalls(Room room) {
        return room.getFlags().contains(Room.RoomFlag.HAS_WALLS)
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_FRONT_END
                || room.getTileConstruction() == Room.TileConstruction.HERO_GATE_3_BY_1;
    }

    /**
     * Find the room starting from a certain point, rooms are never diagonally
     * attached
     *
     * @param p starting point
     * @param roomInstance the room instance
     * @param thing fixed room item
     */
    private void findRoom(Point p, RoomInstance roomInstance, Thing.Room thing) {
        IMapTileInformation tile = getMapData().getTile(p);

        // Get the terrain
        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

            if (!roomCoordinates.containsKey(p)) {
                if (roomInstance.getRoom().equals(kwdFile.getRoomByTerrain(terrain.getTerrainId()))) {

                    // Add the coordinate
                    roomCoordinates.put(p, roomInstance);
                    roomInstance.addCoordinate(p);
                    if (thing != null) {
                        roomThings.put(p, thing);
                    }

                    // Find north
                    findRoom(new Point(p.x, p.y - 1), roomInstance, thing);

                    // Find east
                    findRoom(new Point(p.x + 1, p.y), roomInstance, thing);

                    // Find south
                    findRoom(new Point(p.x, p.y + 1), roomInstance, thing);

                    // Find west
                    findRoom(new Point(p.x - 1, p.y), roomInstance, thing);
                }
            }
        }
    }

    /**
     * Find a terrain batch starting from a certain point, they are never
     * diagonally attached
     *
     * @param p starting point
     * @param entityInstance the batch instance
     */
    private void findTerrainBatch(Point p, EntityInstance<Terrain> entityInstance) {
        IMapTileInformation tile = getMapData().getTile(p);

        if (!terrainBatchCoordinates.containsKey(p)) {

            // Get the terrain
            Terrain terrain = getTerrain(tile);
            Terrain bridgeTerrain = kwdFile.getTerrainBridge(tile.getBridgeTerrainType(), terrain);
            if (bridgeTerrain != null) {
                terrain = bridgeTerrain;
            }

            if (entityInstance.getEntity().equals(terrain)) {

                // Add the coordinate
                terrainBatchCoordinates.put(p, entityInstance);
                entityInstance.addCoordinate(p);

                // Find north
                findTerrainBatch(new Point(p.x, p.y - 1), entityInstance);

                // Find east
                findTerrainBatch(new Point(p.x + 1, p.y), entityInstance);

                // Find south
                findTerrainBatch(new Point(p.x, p.y + 1), entityInstance);

                // Find west
                findTerrainBatch(new Point(p.x - 1, p.y), entityInstance);
            }
        }
    }

    /**
     * Constructs the given room
     *
     * @param roomInstance the room instance
     */
    private Spatial handleRoom(RoomInstance roomInstance) {
        RoomConstructor roomConstructor = RoomFactory.constructRoom(roomInstance, assetManager, kwdFile);
        roomActuals.put(roomInstance, roomConstructor);
        updateRoomWalls(roomInstance);
        if (roomConstructor == null) {
            return null;
        }

        // Vertex ambient (design §8.1): the room's own terrain type, used
        // by RoomConstructor to pre-swap its floor/wall materials to
        // DkTerrain BEFORE it batches them (jME forbids re-materialling
        // after batching).
        Terrain roomTerrain = kwdFile.getTerrain(roomInstance.getRoom().getTerrainId());
        roomConstructor.setAmbient(getVertexAmbient(roomTerrain, roomInstance.getOwnerId()));

        Spatial roomNode = roomConstructor.construct(fogOfWarInformation);
        List<Material> materials = roomConstructor.getLightingMaterials();
        if (!materials.isEmpty()) {
            Point center = getRoomCenter(roomInstance);
            float radius = 0.5f;
            for (Point coordinate : roomInstance.getCoordinates()) {
                radius = Math.max(radius, (float) center.distance(coordinate) + 1f);
            }
            lightRules.registerRegionMaterials(roomInstance,
                    new Vector3f(center.x + 0.5f, 0, center.y + 0.5f), radius, materials);
        }
        return roomNode;
    }

    /**
     * Handle the terrain-light source for a tile (lighting_design.md
     * &sect;6.3): a positioned {@code LightSource}, not a material
     * constant, so it illuminates its surroundings like a torch does. The
     * interior-slab rule (D3) is unverified in the reference design and
     * should be checked in game against a 3x3/5x5 room per the design's
     * own test description.
     * <p>
     * {@code terrain} is whatever {@link #getTerrain(IMapTileInformation)}
     * already resolved for this tile's mesh (real terrain, the Claim Scan
     * stand-in, or the plain rock stand-in) - the fog decision lives there,
     * not here, so mesh and light can never disagree about what a tile is.
     */
    private void handleTerrainLight(IMapTileInformation tile, Terrain terrain, boolean isRoomTile, RoomInstance roomInstance) {
        Point p = tile.getLocation();
        Terrain lightingTerrain = isRoomTile
                ? (isInteriorRoomSlab(p, roomInstance) ? terrain : null)
                : terrain;
        boolean useClaimScanColour = false;
        boolean perceivedUnexplored = false;

        // Perceived-but-unexplored, non-room tile (design §6.3): terrains that
        // reveal through fog keep their own light, everything else glows as
        // Claim Scan. The mesh keeps its rock stand-in (see getTerrain()); only
        // the light looks at the real terrain. Tests explored, not isVisible():
        // isVisible() is already true for a perceived reveal-through-fog tile
        // (Impenetrable Rock, water, lava), which must glow like any other.
        if (!isRoomTile && !fogOfWarInformation.isExplored(p) && fogOfWarInformation.isPerceived(p)) {
            perceivedUnexplored = true;
            Terrain real = kwdFile.getTerrain(tile.getTerrainId());
            lightingTerrain = real.getFlags().contains(Terrain.TerrainFlag.REVEAL_THROUGH_FOG_OF_WAR)
                    ? real
                    : getClaimScanPlaceholderTerrain();
            // Gold/gems (goldValue != 0) keep their own colour through the rock;
            // the rest take the CLAIM_SCAN_LIGHT_* colour.
            useClaimScanColour = lightingTerrain != null && lightingTerrain.getGoldValue() == 0;
        }

        if (lightingTerrain == null || !lightingTerrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_LIGHT)) {
            return;
        }

        // Only the claim-scan / perceived glow is kept; room and ordinary
        // terrain lights are switched off (see LightConstants)
        if (isRoomTile && !LightConstants.ROOM_LIGHTING_ENABLED) {
            return;
        }
        if (!isRoomTile && !perceivedUnexplored && !LightConstants.EXPLORED_TERRAIN_LIGHTS_ENABLED) {
            return;
        }

        int red;
        int green;
        int blue;
        if (useClaimScanColour || lightingTerrain.getTerrainId() == CLAIM_SCAN_TERRAIN_ID) {
            // claim_scan_terrain.md §5: the stand-in's own baked colour is
            // always overridden by these level variables.
            red = LightConstants.getClaimScanLightRed(kwdFile);
            green = LightConstants.getClaimScanLightGreen(kwdFile);
            blue = LightConstants.getClaimScanLightBlue(kwdFile);
        } else {
            Color c = lightingTerrain.getTerrainLight();
            red = c.getRed() + (lightingTerrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_RED) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
            green = c.getGreen() + (lightingTerrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_GREEN) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
            blue = c.getBlue() + (lightingTerrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_BLUE) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
        }

        // Tile (x, y) is centred on (x, y) in world space
        Vector3f position = new Vector3f(p.x, lightingTerrain.getLightHeight(), p.y);
        boolean claimScan = useClaimScanColour || lightingTerrain.getTerrainId() == CLAIM_SCAN_TERRAIN_ID;
        lightRules.addTerrainLight(p, position, red, green, blue,
                claimScan ? LightConstants.CLAIM_SCAN_LIGHT_INTENSITY : LightConstants.TERRAIN_LIGHT_INTENSITY);
    }

    /**
     * Slab light (design &sect;6.7, D2): enabled when the terrain's own
     * {@code Light} block has a non-zero colour. Not used by stock terrain.
     * {@code PLAYER_COLOURED} uses the slab owner's colour (design &sect;6.1).
     */
    private void handleSlabLight(Point p, Terrain terrain, short ownerId) {
        Light light = terrain.getSlabLight();
        if (light == null || light.getColor() == null) {
            return;
        }
        java.awt.Color c = light.getColor(); // Light uses AWT's colour, unlike Terrain
        if (c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 0) {
            return;
        }
        EnumSet<Light.LightFlag> flags = light.getFlags();
        int red = c.getRed() + (flags.contains(Light.LightFlag.COLOR_RED) ? LightConstants.OVER_BRIGHT_BONUS : 0);
        int green = c.getGreen() + (flags.contains(Light.LightFlag.COLOR_GREEN) ? LightConstants.OVER_BRIGHT_BONUS : 0);
        int blue = c.getBlue() + (flags.contains(Light.LightFlag.COLOR_BLUE) ? LightConstants.OVER_BRIGHT_BONUS : 0);
        if (flags.contains(Light.LightFlag.PLAYER_COLOURED)) {
            int[] player = LightConstants.getPlayerLightColour(ownerId);
            red = Math.max(LightConstants.PLAYER_COLOURED_MIN, red * player[0] / 255);
            green = Math.max(LightConstants.PLAYER_COLOURED_MIN, green * player[1] / 255);
            blue = Math.max(LightConstants.PLAYER_COLOURED_MIN, blue * player[2] / 255);
        }
        Vector3f offset = LightConstants.toJmeOffset(light.getmKPos());
        Vector3f position = new Vector3f(p.x + offset.x, offset.y, p.y + offset.z);
        lightRules.addSlabLight(p, position, red, green, blue, light.getRadius(),
                flags.contains(Light.LightFlag.PULSE), flags.contains(Light.LightFlag.FLICKER));
    }

    /**
     * D3: all 8 neighbours belong to the same room instance.
     */
    private boolean isInteriorRoomSlab(Point p, RoomInstance roomInstance) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (!roomInstance.hasCoordinate(new Point(p.x + dx, p.y + dy))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Swaps every geometry under {@code node}'s DkFlat material (created by
     * KmfModelLoader) for a DkTerrain material carrying this tile's
     * per-terrain-type vertex ambient (design &sect;8.1), and returns them
     * so the caller can register them with {@link LightRules} -- terrain
     * lights (design &sect;6.3) and global ambient (&sect;8.2) are pushed
     * into these materials continuously by {@link SceneLights}, since they
     * change with flicker/pulse/settings.
     */
    private List<Material> applyTerrainLighting(Node node, Terrain terrain, short ownerId) {
        List<Material> materials = new ArrayList<>();
        ColorRGBA ambient = getVertexAmbient(terrain, ownerId);
        boolean solid = terrain.getFlags().contains(Terrain.TerrainFlag.SOLID);
        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof BatchNode batchNode) {
                    // Room-wall corner pieces (RoomConstructor.getWallSpatial)
                    // pre-swap themselves to DkTerrain before their own
                    // internal batch() call (jME forbids re-materialling
                    // after batching, whether per-geometry or via
                    // BatchNode.setMaterial) -- just collect the result here.
                    Material existing = firstGeometryMaterial(batchNode);
                    if (existing != null && AssetUtils.isDkTerrainMaterial(existing)) {
                        materials.add(existing);
                    }
                    return; // traversal still descends; children are isBatched() and skipped below
                }
                if (!(spatial instanceof Geometry geometry) || geometry.isBatched()) {
                    return;
                }
                Material dkTerrain = AssetUtils.createDkTerrainMaterial(assetManager, geometry.getMaterial(), ambient);
                if (solid) {
                    dkTerrain.setFloat("MinLight", 0f);
                }
                geometry.setMaterial(dkTerrain);
                materials.add(dkTerrain);
            }
        });
        return materials;
    }

    private static Material firstGeometryMaterial(Node node) {
        Material[] result = new Material[1];
        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (result[0] == null && spatial instanceof Geometry geometry) {
                    result[0] = geometry.getMaterial();
                }
            }
        });
        return result[0];
    }

    /**
     * Gathers whatever Material each Geometry under {@code node} currently
     * carries - called after {@link #applyTerrainLighting} (DkTerrain
     * conversion) and {@link #setTileMaterialToGeometries} (tag/flash/decay
     * tint, which clones a fresh Material for a tagged/flashing/decaying
     * tile) have both already run, so this always matches what's actually
     * rendered instead of a pre-clone reference.
     */
    private static List<Material> collectMaterials(Node node) {
        List<Material> materials = new ArrayList<>();
        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof BatchNode batchNode) {
                    Material existing = firstGeometryMaterial(batchNode);
                    if (existing != null && AssetUtils.isDkTerrainMaterial(existing)) {
                        materials.add(existing);
                    }
                    return;
                }
                if (!(spatial instanceof Geometry geometry) || geometry.isBatched()) {
                    return;
                }
                materials.add(geometry.getMaterial());
            }
        });
        return materials;
    }

    private ColorRGBA getVertexAmbient(Terrain terrain, short ownerId) {
        if (!LightConstants.ROOM_LIGHTING_ENABLED && terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            return ColorRGBA.Black; // no per-room ambient, see LightConstants.ROOM_LIGHTING_ENABLED
        }
        if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_LIGHT)) {
            Color c = terrain.getAmbientLight();
            int r = c.getRed() + (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_RED) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
            int g = c.getGreen() + (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_GREEN) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
            int b = c.getBlue() + (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_BLUE) ? LightConstants.TERRAIN_OVER_BRIGHT_BONUS : 0);
            return new ColorRGBA(r / 255f, g / 255f, b / 255f, 1f);
        }

        // Player-coloured (claimed) paths and walls get no owner tint here: they
        // are lit by the torches and other lights only. The owner tint that used
        // to be added as ambient made a red keeper's claimed tiles read red.
        return ColorRGBA.Black;
    }

    /**
     * Legacy material tweak, kept only for {@link Water}'s own
     * (separate, KMF-independent) material -- out of scope for this pass
     * of the lighting rewrite, unaffected either way since
     * {@code UseMaterialColors} is false here and these params were never
     * visible before.
     *
     * @param material the material to adjust
     * @param terrain the terrain data
     */
    public static void setTerrainMaterialLighting(Material material, Terrain terrain) {

        // Ambient light
        if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_LIGHT)) {
            Color c = terrain.getAmbientLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.AMBIENT_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Ambient", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }

        // Not sure what the terrain light is supposed to be
        if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_LIGHT)) {
            Color c = terrain.getTerrainLight();
            int r = c.getRed();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_RED)) {
                r += 256;
            }
            int g = c.getGreen();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_GREEN)) {
                g += 256;
            }
            int b = c.getBlue();
            if (terrain.getFlags().contains(Terrain.TerrainFlag.TERRAIN_COLOR_BLUE)) {
                b += 256;
            }
            material.setColor("Specular", new ColorRGBA(r / 255f, g / 255f, b / 255f, 0));
        }
        material.setColor("Diffuse", ColorRGBA.White);
        if (material.getMaterialDef().getMaterialParam("UseMaterialColors") != null) {
            material.setBoolean("UseMaterialColors", false); // Hmm...
        }
    }

    /**
     * Remove all the given room instances (and actually removes them from the
     * scene)
     *
     * @param instances room instances to remove
     */
    protected void removeRoomInstances(RoomInstance... instances) {
        for (RoomInstance instance : instances) {
            roomsNode.detachChild(roomNodes.get(instance));
            roomNodes.remove(instance);

            roomActuals.remove(instance);
            for (Point p : instance.getCoordinates()) {
                roomCoordinates.remove(p);
            }

            lightRules.removeRoomLight(instance);
            lightRules.unregisterRegionMaterials(instance);
        }
    }

    /**
     * Find room wall sections, continuous sections facing the same way
     *
     * @param roomInstance room instance
     */
    private void findRoomWallSections(RoomInstance roomInstance) {
        if (hasRoomWalls(roomInstance.getRoom())) {
            List<WallSection> sections = new ArrayList<>();
            Map<Point, Set<WallDirection>> alreadyWalledPoints = new HashMap<>();
            for (Point p : roomInstance.getCoordinates()) {

                // Traverse in all four directions to find wallable sections facing the same direction
                traverseRoomWalls(p, roomInstance, WallDirection.NORTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.EAST, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.SOUTH, sections, alreadyWalledPoints);
                traverseRoomWalls(p, roomInstance, WallDirection.WEST, sections, alreadyWalledPoints);
            }
            roomInstance.setWallSections(sections);
        }
    }

    /**
     * Traverses room walls facing in certain direction from a room point. Finds
     * a section.
     *
     * @param p starting room point
     * @param roomInstance the room instance of which walls we need to find
     * @param direction the direction of which the want the walls to face
     * @param sections list of already find sections
     * @param alreadyWalledPoints already found wall points
     */
    private void traverseRoomWalls(Point p, RoomInstance roomInstance, WallDirection direction,
            List<WallSection> sections, Map<Point, Set<WallDirection>> alreadyWalledPoints) {

        Set<WallDirection> alreadyTraversedDirections = alreadyWalledPoints.get(p);
        if (alreadyTraversedDirections == null || !alreadyTraversedDirections.contains(direction)) {

            List<Point> section = getRoomWalls(p, roomInstance, direction);
            if (section != null) {

                // Add section
                sections.add(new WallSection(direction, section));

                // Add to known points
                for (Point sectionPoint : section) {
                    Set<WallDirection> directions = alreadyWalledPoints.get(sectionPoint);
                    if (directions == null) {
                        directions = EnumSet.noneOf(WallDirection.class);
                    }
                    directions.add(direction);
                    alreadyWalledPoints.put(sectionPoint, directions);
                }
            }
        }
    }

    private List<Point> getRoomWalls(Point p, RoomInstance roomInstance, WallDirection wallDirection) {

        // See if the starting point has a wall to the given direction
        IMapTileInformation tile;
        if (null == wallDirection) {
            tile = getMapData().getTile(p.x + 1, p.y); // West
        } else
            tile = switch (wallDirection) {
                case NORTH ->
                    getMapData().getTile(p.x, p.y + 1);
                case EAST ->
                    getMapData().getTile(p.x - 1, p.y);
                case SOUTH ->
                    getMapData().getTile(p.x, p.y - 1);
                default ->
                    getMapData().getTile(p.x + 1, p.y); // West
            };

        Terrain terrain = kwdFile.getTerrain(tile.getTerrainId());
        if (terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && terrain.getFlags().contains(Terrain.TerrainFlag.ALLOW_ROOM_WALLS)) {

            // Found wallable
            List<Point> wallPoints = new ArrayList<>();
            wallPoints.add(p);

            // Traverse possible directions, well one direction, "to the right"
            List<Point> adjacentWallPoints = null;
            if (null == wallDirection) {
                Point nextPoint = new Point(p.x, p.y + 1); // South, sorting, so right is left now
                RoomInstance instance = roomCoordinates.get(nextPoint);
                if (instance != null && instance.equals(roomInstance)) {
                    adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                }
            } else
                switch (wallDirection) {
                    case NORTH -> {
                        Point nextPoint = new Point(p.x + 1, p.y); // East
                        RoomInstance instance = roomCoordinates.get(nextPoint);
                        if (instance != null && instance.equals(roomInstance)) {
                            adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                        }
                    }
                    case EAST -> {
                        Point nextPoint = new Point(p.x, p.y + 1); // South
                        RoomInstance instance = roomCoordinates.get(nextPoint);
                        if (instance != null && instance.equals(roomInstance)) {
                            adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                        }
                    }
                    case SOUTH -> {
                        Point nextPoint = new Point(p.x + 1, p.y); // East, sorting, so right is left now
                        RoomInstance instance = roomCoordinates.get(nextPoint);
                        if (instance != null && instance.equals(roomInstance)) {
                            adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                        }
                    }
                    default -> {
                        Point nextPoint = new Point(p.x, p.y + 1); // South, sorting, so right is left now
                        RoomInstance instance = roomCoordinates.get(nextPoint);
                        if (instance != null && instance.equals(roomInstance)) {
                            adjacentWallPoints = getRoomWalls(nextPoint, roomInstance, wallDirection);
                        }
                    }
                }

            // Add the point(s)
            if (adjacentWallPoints != null) {
                wallPoints.addAll(adjacentWallPoints);
            }

            return wallPoints;
        }

        return null;
    }

    /**
     * Update the selected rooms' walls
     *
     * @param rooms the rooms to update
     */
    protected void updateRoomWalls(RoomInstance... rooms) {
        for (RoomInstance room : rooms) {
            findRoomWallSections(room);
        }
    }

    protected void updateRoomWalls(List<RoomInstance> rooms) {
        updateRoomWalls(rooms.toArray(RoomInstance[]::new));
    }

    /**
     * Redraws a room instance
     *
     * @param roomInstance the instance to redraw
     */
    protected void updateRoom(RoomInstance roomInstance) {

        // Redraw
        updateRoomWalls(roomInstance);
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress from 0.0 to 1.0
     */
    protected abstract void updateProgress(final float progress);

}
