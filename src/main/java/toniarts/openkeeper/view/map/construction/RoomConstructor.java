/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;
import toniarts.openkeeper.view.map.WallSection;

/**
 * A base class for constructing different kind of rooms. FIXME: Basically
 * static style constructor would be kinda better since these are one use
 * wonders
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomConstructor {

    protected final AssetManager assetManager;
    protected final RoomInstance roomInstance;
    protected final boolean[][] map;
    protected final Point start;

    /**
     * This room's vertex ambient, set by
     * the caller (see {@code MapViewController.handleRoom}) before
     * {@link #construct()}/{@link #getWallSpatial} run, since it's needed
     * to build the Vertex materials this class pre-swaps its geometry
     * onto BEFORE batching it (jME forbids re-materialling afterwards).
     */
    private ColorRGBA ambient = ColorRGBA.Black;
    private final List<Material> lightingMaterials = new ArrayList<>();

    // Per-tile floor pieces attached via attachFloorTile(), keyed by their world
    // tile point. A room instance is found and built as a whole (see
    // MapViewController#findRoom), covering every physically-connected tile regardless
    // of fog, so this is what lets construct() hide the tiles the viewer hasn't
    // actually explored yet instead of showing the whole room at once. A list per
    // point since some constructors attach more than one piece to the same tile
    // (e.g. HeroGateConstructor's cap piece plus its regular piece).
    private final Map<Point, List<Spatial>> floorTiles = new HashMap<>();

    private final static int[] WALL_INDEXES = new int[]{7, 8};

    public RoomConstructor(AssetManager assetManager, RoomInstance roomInstance) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.map = roomInstance.getCoordinatesAsMatrix();
        this.start = roomInstance.getMatrixStartPoint();
    }

    public void setAmbient(ColorRGBA ambient) {
        this.ambient = ambient;
    }

    /**
     * The Vertex materials {@link #construct()} swapped its floor/wall
     * geometry onto, for the caller to register with {@code LightRules}. Only populated after {@link #construct()}
     * has run.
     */
    public List<Material> getLightingMaterials() {
        return lightingMaterials;
    }

    /**
     * Constructs the room
     *
     * @param fogOfWarInformation the viewer's fog-of-war query surface, used to
     * hide the floor of any room tile that isn't explored yet
     * @return the spatial representing the room
     */
    public final Spatial construct(IFogOfWarInformation fogOfWarInformation) {
        Node root = new Node(roomInstance.getRoom().getName());

        // Add the floor
        BatchNode floorNode = constructFloor();
        if (floorNode != null) {
            floorNode.setName("Floor");
            floorNode.setShadowMode(getFloorShadowMode());
            applyLighting(floorNode, lightingMaterials);
            hideUnexploredFloorTiles(fogOfWarInformation);
            floorNode.batch();
            root.attachChild(floorNode);
        }

        // Custom wall
        BatchNode wallNode = constructWall();
        if (wallNode != null) {
            wallNode.setName("Wall");
            wallNode.setShadowMode(getWallShadowMode());
            applyLighting(wallNode, lightingMaterials);
            wallNode.batch();
            root.attachChild(wallNode);
        }

        return root;
    }

    /**
     * Swaps every geometry under {@code node} to a Vertex material.
     * Must run before {@code node.batch()},
     * see the class-level note on {@link #ambient}. {@code outMaterials}
     * is a separate list per call site (not always {@link
     * #lightingMaterials}) so a single per-tile wall corner piece (see
     * {@link #getWallSpatial}) doesn't also end up double-registered as
     * part of the whole room's region.
     */
    private void applyLighting(Node node, List<Material> outMaterials) {
        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry geometry) || geometry.isBatched()) {
                    return;
                }
                Material vertex = AssetUtils.createVertexMaterial(assetManager, geometry.getMaterial(), ambient);
                geometry.setMaterial(vertex);
                outMaterials.add(vertex);
            }
        });
    }

    /**
     * Registers a per-tile floor piece so {@link #construct} can later decide
     * whether to cull it, and attaches it to the floor batch. Use this instead
     * of attaching straight to {@code root} for every piece that represents one
     * room tile.
     *
     * @param root the floor batch node being built
     * @param p the tile point this piece represents
     * @param tile the floor piece
     * @return {@code tile}, for chaining
     */
    protected final Spatial attachFloorTile(BatchNode root, Point p, Spatial tile) {
        floorTiles.computeIfAbsent(p, k -> new ArrayList<>()).add(tile);
        root.attachChild(tile);
        return tile;
    }

    /**
     * Excludes the floor pieces of not-yet-explored tiles from the batch and
     * culls them. They must be excluded from the batch (not just culled)
     * because a {@link BatchNode} merges its children's raw geometry once and
     * for all at {@code batch()} time - a child's {@code CullHint} set
     * afterwards has no effect on the merged mesh.
     */
    private void hideUnexploredFloorTiles(IFogOfWarInformation fogOfWarInformation) {
        for (Map.Entry<Point, List<Spatial>> entry : floorTiles.entrySet()) {
            if (!fogOfWarInformation.isVisible(entry.getKey())) {
                for (Spatial tile : entry.getValue()) {
                    tile.setBatchHint(Spatial.BatchHint.Never);
                    tile.setCullHint(Spatial.CullHint.Always);
                }
            }
        }
    }

    protected abstract BatchNode constructFloor();

    /**
     * Rooms typically don't construct walls themselves, instead they are asked
     * for the wall spatials by the map loader in normal map drawing situation
     *
     * @see #getWallSpatial
     */
    protected BatchNode constructWall() {
        return null;
    }

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected RenderQueue.ShadowMode getWallShadowMode() {
        return RenderQueue.ShadowMode.CastAndReceive;
    }

    /**
     * Use the big floor tile at the specified point
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return use big tile
     */
    public boolean useBigFloorTile(int x, int y) {
        return true;
    }

    /**
     * Does the given tile have the same room instance
     *
     * @param map the map
     * @param x x coordinate
     * @param y y coordinate
     * @return has the same tile
     */
    protected static boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }

    protected final Spatial loadModel(String model, ArtResource artResource) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, artResource);
        //resetSpatial(spatial);
        return spatial;
    }

    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        ArtResource artResource = roomInstance.getRoom().getCompleteResource();
        String resource = artResource.getName();

        for (WallSection section : roomInstance.getWallSections()) {
            if (section.direction() != direction) {
                continue;
            }

            int index = section.coordinates().indexOf(p);
            if (index < 0) {
                continue;
            }

            int sectionSize = section.coordinates().size();
            if (index == 0 || index == (sectionSize - 1)) {
                return createWallEnd(resource, artResource, direction, index == 0, index == (sectionSize - 1));
            }

            // Complete walls, 8, 7, 8, 7 and so forth
            return createWallMiddle(resource + getWallIndex(index), artResource, direction);
        }

        return null;
    }

    private Spatial createWallEnd(String resource, ArtResource artResource, WallSection.WallDirection direction,
            boolean first, boolean last) {
        boolean flipped = direction == WallSection.WallDirection.EAST
                || direction == WallSection.WallDirection.NORTH;
        float quarter = WorldUtils.TILE_WIDTH / 4;
        float back = -3 * WorldUtils.TILE_WIDTH / 4;
        Vector3f moveFirst = new Vector3f(flipped ? -quarter : quarter, 0, back);
        Vector3f moveSecond = new Vector3f(flipped ? quarter : -quarter, 0, back);

        // The sorting direction forces us to swap the end pieces
        int firstPiece = first ? (flipped ? 5 : 4) : 6;
        int secondPiece = last ? (flipped ? 4 : 5) : 6;

        BatchNode spatial = new BatchNode();
        spatial.attachChild(loadWallPart(resource + firstPiece, artResource, moveFirst));
        spatial.attachChild(loadWallPart(resource + secondPiece, artResource, moveSecond));

        // Pre-swap to Vertex before batching (see the class-level
        // note on `ambient`) -- MapViewController.applyTerrainLighting
        // recognizes and collects the already-swapped result for
        // per-tile light registration, it doesn't re-swap it.
        applyLighting(spatial, new ArrayList<>());
        spatial.batch();
        return spatial;
    }

    private Spatial loadWallPart(String model, ArtResource artResource, Vector3f move) {
        Spatial part = AssetUtils.loadModel(assetManager, model, artResource);
        part.move(move);
        part.rotate(0, FastMath.PI, 0);
        return part;
    }

    private Spatial createWallMiddle(String model, ArtResource artResource, WallSection.WallDirection direction) {
        Spatial spatial = AssetUtils.loadModel(assetManager, model, artResource);
        spatial.rotate(0, FastMath.PI, 0);

        switch (direction) {
            case WEST ->
                spatial.move(-WorldUtils.TILE_WIDTH, 0, 0);
            case SOUTH ->
                spatial.move(0, 0, WorldUtils.TILE_WIDTH);
            case EAST ->
                spatial.move(WorldUtils.TILE_WIDTH, 0, 0);
            default ->
                spatial.move(0, 0, -WorldUtils.TILE_WIDTH); // NORTH
        }
        return spatial;
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     * @param p the tile point
     */
    protected void moveSpatial(Spatial tile, Point start, Point p) {
        // Reset, really, the size is 1 after this...
        //resetSpatial(tile);
        tile.move(p.x - start.x, 0, p.y - start.y);
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, Spatial
     * @param start start point
     */
    protected void moveSpatial(Spatial tile, Point start) {
        // Reset, really, the size is 1 after this...
        //resetSpatial(tile);
        tile.move(start.x, 0, start.y);
    }

    public int getWallIndex(int index) {
        int pointer = index % WALL_INDEXES.length;
        return WALL_INDEXES[pointer];
    }

}
