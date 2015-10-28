package toniarts.openkeeper.view.selection;

import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.debug.WireBox;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.world.MapLoader;

/**
 * Class that contains the SelectionLogic of the Selection-Helper-Box in the
 * Viewport
 *
 * @author Philip Willuweit p.willuweit@gmx.de
 */
public class SelectionHandler {

    private Main app;
    private final PlayerInteractionState state;
    private float appScaled = MapLoader.TILE_WIDTH;

    /* Visuals for Selection */
    private Geometry wireBoxGeo;
    private WireBox wireBox;
    private Material matWireBox;
    /* The selected Area */
    protected SelectionArea selectionArea;
    protected boolean hasSelectedArea = false;

    public SelectionHandler(Main app, PlayerInteractionState state) {
        this.app = app;
        this.state = state;
        this.selectionArea = new SelectionArea(appScaled);

        setupVisualsForSelection();
    }

    /**
     * Returns the mouse position as a Vector2f with rounded values (int)
     *
     * @return The position of the mouse
     */
    public Vector2f getRoundedMousePos() {
        Vector3f tmp = app.getCamera().getWorldCoordinates(state.mousePosition, 0f).clone();
        Vector3f dir = app.getCamera().getWorldCoordinates(state.mousePosition, 1f).subtractLocal(tmp).normalizeLocal();
        dir.multLocal(-app.getCamera().getLocation().y / dir.y).addLocal(app.getCamera().getLocation());

        Vector2f ret = new Vector2f((int) (dir.getX() + appScaled * 1.5), (int) (dir.getZ() + appScaled * 1.5));
        ret.multLocal(appScaled);

        return ret;
    }

    public void setNoSelectedArea() {
        hasSelectedArea = false;
    }

    protected boolean isOnView() {
        return true; // this.app.getInputManager().getCursorPosition().y > 100;
    }

    public SelectionArea getSelectionArea() {
        return selectionArea;
    }

    public void updateSelectionBox() {
        if (isOnView()) {
            float dx = selectionArea.getDeltaX();
            float dy = selectionArea.getDeltaY();

            Vector2f position = selectionArea.getCenter();
            wireBoxGeo.setLocalTranslation(position.x - 0.5f, appScaled / 2, position.y - 0.5f);

            wireBox.updatePositions(appScaled / 2 * dx + 0.01f, appScaled / 2 + 0.01f, appScaled / 2 * dy + 0.01f);
        }
    }

    private void setupVisualsForSelection() {

        matWireBox = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        // mat.getAdditionalRenderState().setWireframe(true);
        matWireBox.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        matWireBox.setColor("Color", ColorRGBA.Blue.mult(5));

        this.wireBox = new WireBox(appScaled, appScaled, appScaled);
        this.wireBox.setDynamic();
        this.wireBox.setLineWidth(3);
        this.wireBoxGeo = new Geometry("wireBox", wireBox);
        this.wireBoxGeo.setMaterial(matWireBox);
        this.wireBoxGeo.setCullHint(CullHint.Never);

        this.app.getRootNode().attachChild(this.wireBoxGeo);
    }

    public void userSubmit(SelectionArea area) {

        // Determine if this is a select/deselect by the starting tile's status
        boolean select = !state.getWorldHandler().isSelected((int) Math.max(0, selectionArea.getActualStartingCoordinates().x), (int) Math.max(0, selectionArea.getActualStartingCoordinates().y));
        state.getWorldHandler().selectTiles(selectionArea, select);
    }
}
