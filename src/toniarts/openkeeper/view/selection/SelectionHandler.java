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
import toniarts.openkeeper.Main;
import toniarts.openkeeper.world.MapLoader;

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
    private final float appScaled = MapLoader.TILE_WIDTH;
    private ColorIndicator selectionColor = ColorIndicator.BLUE;

    /* Visuals for Selection */
    private Geometry wireBoxGeo;
    private WireBox wireBox;
    private Material matWireBox;
    /* The selected Area */
    private final SelectionArea selectionArea;
    private boolean active = false;
    private Vector2f mousePosition = Vector2f.ZERO;
    private Vector3f cameraPosition = Vector3f.NAN;
    private Vector2f pointedTilePosition = Vector2f.ZERO;

    public SelectionHandler(Main app) {
        this.app = app;
        this.selectionArea = new SelectionArea(appScaled);

        setupVisualsForSelection();
    }

    public boolean update(Vector2f mousePosition) {
        Camera cam = app.getCamera();
        Vector3f pos = cam.getLocation();

        if (cameraPosition.equals(pos) && this.mousePosition.equals(mousePosition)) {
            return false;
        }

        cameraPosition = pos.clone();
        this.mousePosition = mousePosition.clone();

        Vector3f tmp = cam.getWorldCoordinates(this.mousePosition, 0f).clone();
        Vector3f dir = cam.getWorldCoordinates(this.mousePosition, 1f).subtractLocal(tmp).normalizeLocal();
        dir.multLocal((MapLoader.TILE_HEIGHT - pos.getY()) / dir.getY()).addLocal(pos);

        pointedTilePosition = new Vector2f(Math.round(dir.getX() + appScaled / 2),
                Math.round(dir.getZ() + appScaled / 2));
        pointedTilePosition.multLocal(appScaled);

        setPos(pointedTilePosition);

        return true;
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
            float dx = selectionArea.getDeltaX();
            float dy = selectionArea.getDeltaY();

            Vector2f position = selectionArea.getCenter();
            wireBoxGeo.setLocalTranslation(position.x - appScaled / 2, appScaled / 2, position.y - appScaled / 2);

            wireBox.updatePositions(appScaled / 2 * dx + 0.01f, appScaled / 2 + 0.01f, appScaled / 2 * dy + 0.01f);

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
        matWireBox.getAdditionalRenderState().setLineWidth(6);

        this.wireBox = new WireBox(appScaled, appScaled, appScaled);
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
}
