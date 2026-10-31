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
import com.jme3.scene.debug.WireBox;
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
    private WireBox wireBox;
    private Material matWireBox;
    /* The selected Area */
    private final SelectionArea selectionArea;
    private boolean active = false;
    private Point pointedTileIndex = new Point(); // Could be just point...
    private Vector2f pointedTilePosition = new Vector2f(Vector2f.ZERO); // Could be just point...
    private final Vector2f pointedPosition = new Vector2f(Vector2f.ZERO);
    private final Vector3f rayNear = new Vector3f();
    private final Vector3f rayDirection = new Vector3f();
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
     * Where the mouse ray crosses the horizontal plane at {@code height}.
     * {@link #getActualPointedPosition()} is that point at wall-top height, so
     * for anything lower down it is off towards the camera.
     *
     * @return the crossing in 2D (x, z) space
     */
    public Vector2f getPointedPositionAtHeight(float height) {
        // The ray from the last update(): rayDirection is far minus near, so it needs no normalizing
        Vector3f pos = app.getCamera().getLocation();
        if (Math.abs(rayDirection.getY()) < 1e-5f) {
            return pointedPosition;
        }
        float t = (height - pos.getY()) / rayDirection.getY();
        return new Vector2f(pos.getX() + rayDirection.getX() * t, pos.getZ() + rayDirection.getZ() * t);
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

    /**
     * The color the selection box shows now
     *
     * @return the shown selection indicator color
     */
    public ColorIndicator getSelectionColor() {
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
            float dx = selectionArea.getDeltaX();
            float dy = selectionArea.getDeltaY();
            float delta = 0.01f;

            Vector2f position = selectionArea.getCenter();
            wireBoxGeo.setLocalTranslation(position.x, WorldUtils.FLOOR_HEIGHT, position.y);

            wireBox.updatePositions(WorldUtils.TILE_WIDTH / 2 * dx + delta,
                    WorldUtils.FLOOR_HEIGHT + delta,
                    WorldUtils.TILE_WIDTH / 2 * dy + delta);

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

        this.wireBox = new WireBox(WorldUtils.TILE_WIDTH, WorldUtils.TILE_WIDTH, WorldUtils.TILE_WIDTH);
        this.wireBox.setDynamic();

        this.wireBoxGeo = new Geometry("wireBox", wireBox);
        this.wireBoxGeo.setMaterial(matWireBox);
        this.wireBoxGeo.setCullHint(CullHint.Never);
        this.wireBoxGeo.setShadowMode(RenderQueue.ShadowMode.Off);

        this.app.getRootNode().attachChild(this.wireBoxGeo);
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
}
