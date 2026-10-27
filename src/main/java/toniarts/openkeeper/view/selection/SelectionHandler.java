package toniarts.openkeeper.view.selection;

import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import toniarts.openkeeper.view.map.TerrainDeformer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.MapViewController;

/**
 * Class that contains the SelectionLogic of the Selection-Helper-Box in the
 * Viewport
 *
 * @author Philip Willuweit p.willuweit@gmx.de
 */
public abstract class SelectionHandler {

    public enum ColorIndicator {

        BLUE(ColorRGBA.Blue.mult(5)), RED(ColorRGBA.Red.mult(5));

        private ColorIndicator(ColorRGBA color) {
            this.color = color;
        }

        public ColorRGBA getColor() {
            return color;
        }
        private final ColorRGBA color;
    }

    private final Main app;
    private ColorIndicator selectionColor = ColorIndicator.BLUE;

    /* Visuals for Selection */
    private Geometry wireBoxGeo;
    private int deformerVersion;
    private Material matWireBox;
    /* The selected Area */
    private final SelectionArea selectionArea;
    private boolean active = false;
    private Point pointedTileIndex = new Point(); // Could be just point...
    private Vector2f pointedTilePosition = new Vector2f(Vector2f.ZERO); // Could be just point...
    private final Vector2f pointedPosition = new Vector2f(Vector2f.ZERO);
    private final Vector3f rayNear = new Vector3f();
    private final Vector3f rayDirection = new Vector3f();
    private static final float OUTLINE_STEP = 0.25f; // Same as the spacing of the terrain deformation
    private final Predicate<Point> blocks = this::isBlock;

    public SelectionHandler(Main app) {
        this.app = app;
        this.selectionArea = new SelectionArea(WorldUtils.TILE_WIDTH);

        setupVisualsForSelection();
    }

    /**
     * Updates the pointed tile. Runs every frame, also when neither the mouse
     * nor the camera moved, since digging or the fog lifting changes what is
     * under the cursor.
     *
     * @param mousePosition the cursor on the screen
     * @return true if the pointed tile changed
     */
    public boolean update(Vector2f mousePosition) {

        // The terrain changed shape under the outline
        TerrainDeformer deformer = getDeformer();
        int version = deformer != null ? deformer.getVersion() : 0;
        if (version != deformerVersion) {
            deformerVersion = version;
            updateSelectionBox();
        }

        Camera cam = app.getCamera();
        cam.getWorldCoordinates(mousePosition, 0f, rayNear);
        cam.getWorldCoordinates(mousePosition, 1f, rayDirection).subtractLocal(rayNear);

        TilePicker.Pick pick = TilePicker.pick(cam.getLocation(), rayDirection, blocks);
        if (pick == null) {
            return false;
        }

        pointedPosition.set(pick.position());
        if (pick.tile().equals(pointedTileIndex)) {
            return false;
        }

        pointedTileIndex = pick.tile();
        pointedTilePosition = WorldUtils.pointToVector2f(pointedTileIndex);
        setPos(pointedTilePosition);

        return true;
    }

    /**
     * Get the actual pointed location
     *
     * @return the actual pointed location in 2D space
     */
    public Vector2f getActualPointedPosition() {
        return pointedPosition;
    }

    /**
     * Show coordinate of tile pointed by mouse
     *
     * @return 2D rounded mouse position in tile plane
     */
    public Point getPointedTileIndex() {
        return pointedTileIndex;
    }
    
    /**
     * Show coordinate of tile pointed by mouse
     *
     * @return 2D rounded mouse position in tile plane
     */
    public Vector2f getPointedTilePosition() {
        return pointedTilePosition;
    }

    /**
     * The selection box color, to indicate the action, or its feasibility
     *
     * @return the selection indicator color
     */
    protected ColorIndicator getColorIndicator() {
        return selectionColor;
    }

    public SelectionArea getSelectionArea() {
        return selectionArea;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            setPos(selectionArea.getRealEnd());
        }
    }

    /**
     * @param position the start or end to set
     */
    public void setPos(Vector2f position) {
        boolean changed = false;

        if (!active && (position != selectionArea.getRealStart())) {
            selectionArea.setStart(position);
            changed = true;
        }

        if (position != selectionArea.getRealEnd()) {
            selectionArea.setEnd(position);
            changed = true;
        }

        if (changed) {
            updateSelectionBox();
        }
    }

    public void updateSelectionBox() {
        if (isVisible()) {
            Vector2f start = selectionArea.getStart();
            Vector2f end = selectionArea.getEnd();
            float half = WorldUtils.TILE_WIDTH / 2;
            wireBoxGeo.setMesh(createOutline(start.x - half, start.y - half, end.x + half, end.y + half));

            // Selection color indicator
            ColorIndicator newSelectionColor = getColorIndicator();
            if (!newSelectionColor.equals(selectionColor)) {
                selectionColor = newSelectionColor;
                matWireBox.setColor("Color", selectionColor.getColor());
            }

            this.wireBoxGeo.setCullHint(CullHint.Never);
        } else {
            this.wireBoxGeo.setCullHint(CullHint.Always);
        }
    }

    private void setupVisualsForSelection() {
        matWireBox = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matWireBox.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        matWireBox.setColor("Color", selectionColor.getColor());
        matWireBox.getAdditionalRenderState().setLineWidth(6); // TODO: GL_INVALID_VALUE on intel driver

        this.wireBoxGeo = new Geometry("wireBox", createOutline(0, 0, WorldUtils.TILE_WIDTH, WorldUtils.TILE_WIDTH));
        this.wireBoxGeo.setMaterial(matWireBox);
        this.wireBoxGeo.setCullHint(CullHint.Never);
        this.wireBoxGeo.setShadowMode(RenderQueue.ShadowMode.Off);

        this.app.getRootNode().attachChild(this.wireBoxGeo);
    }

    /**
     * The edges of the selected area as a column from the floor to the top of
     * the wall. The lines are cut short and moved the same way as the terrain
     * is, so they stay on the bent floor and walls.
     */
    private Mesh createOutline(float minX, float minZ, float maxX, float maxZ) {
        TerrainDeformer deformer = getDeformer();
        List<Vector3f> points = new ArrayList<>();
        float delta = 0.01f;
        float[] heights = {WorldUtils.FLOOR_HEIGHT, WorldUtils.TOP_HEIGHT};
        float[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};

        // The floor and the top, around
        for (float y : heights) {
            for (int i = 0; i < corners.length; i++) {
                float[] from = corners[i];
                float[] to = corners[(i + 1) % corners.length];
                addLine(points, deformer, from[0], y, from[1], to[0], y, to[1]);
            }
        }

        // The corners between
        for (float[] corner : corners) {
            addLine(points, deformer, corner[0], heights[0], corner[1], corner[0], heights[1], corner[1]);
        }

        FloatBuffer positions = BufferUtils.createFloatBuffer(points.size() * 3);
        for (Vector3f point : points) {
            positions.put(point.x).put(point.y + delta).put(point.z);
        }
        positions.flip();

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    private static void addLine(List<Vector3f> points, TerrainDeformer deformer,
            float x1, float y1, float z1, float x2, float y2, float z2) {
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(x2 - x1) + Math.abs(z2 - z1), Math.abs(y2 - y1)) / OUTLINE_STEP));
        Vector3f previous = null;
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            Vector3f point = new Vector3f(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t);
            if (deformer != null) {
                deformer.deform(point, point, null);
            }
            if (previous != null) {
                points.add(previous);
                points.add(point);
            }
            previous = point;
        }
    }

    /**
     * Detaches the selection box
     */
    public void cleanup() {
        this.app.getRootNode().detachChild(this.wireBoxGeo);
    }

    /**
     * Should the selection be visible
     *
     * @return is visible
     */
    abstract public boolean isVisible();

    /**
     * Whether a tile is drawn as a block up to wall height, the cursor stops
     * at its top or its side instead of going through to the floor
     *
     * @param tile the tile, may be outside the map
     * @return is the tile a block
     */
    abstract protected boolean isBlock(Point tile);

    /**
     * @return what bends the terrain, the outline follows it. {@code null}
     * for a flat terrain
     */
    protected TerrainDeformer getDeformer() {
        return null;
    }
}
