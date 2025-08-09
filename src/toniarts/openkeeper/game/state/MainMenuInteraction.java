
package toniarts.openkeeper.game.state;

import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import toniarts.openkeeper.view.map.construction.FrontEndLevelControl;

/**
 *
 * This is for the level pick up
 */
public final class MainMenuInteraction implements RawInputListener {
    private final MainMenuState mainMenuState;
    private FrontEndLevelControl currentControl;

    public MainMenuInteraction(MainMenuState mainMenuState) {
        this.mainMenuState = mainMenuState;
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        setCampaignMapActive(evt.getX(), evt.getY());
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (currentControl != null && evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
            evt.setConsumed();

            // Select level
            mainMenuState.selectCampaignLevel(currentControl);
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

        // NOT TESTED AT ALL, just for shit & giggles, may work which would be super cool
        if (!evt.isScaleSpanInProgress()) {
            if (currentControl != null) {
                evt.setConsumed();

                // Select level
                mainMenuState.selectCampaignLevel(currentControl);
            } else if (currentControl == null) {
                evt.setConsumed();

                // Treat this like "on hover"
                setCampaignMapActive((int) evt.getX(), (int) evt.getY());
            }
        }
    }
    
    /**
         * Sets the map at certain point as active (i.e. selected), IF there is
         * one
         *
         * @param x x screen coordinate
         * @param y y screen coordinate
         */
        private void setCampaignMapActive(int x, int y) {

            // See if we hit a map
            CollisionResults results = new CollisionResults();

            // Convert screen click to 3D position
            Vector3f click3d = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(x, y), 0f);
            Vector3f dir = mainMenuState.app.getCamera().getWorldCoordinates(
                    new Vector2f(x, y), 1f).subtractLocal(click3d);

            // Aim the ray from the clicked spot forwards
            Ray ray = new Ray(click3d, dir);

            // Collect intersections between ray and all nodes in results list
            mainMenuState.menuNode.collideWith(ray, results);

            // See the results so we see what is going on
            for (int i = 0; i < results.size(); i++) {

                FrontEndLevelControl controller = results.getCollision(i).getGeometry().getParent().getParent().getControl(FrontEndLevelControl.class);
                if (controller != null) {

                    // Deactivate current controller
                    if (currentControl != null && !currentControl.equals(controller)) {
                        currentControl.setActive(false);
                    }

                    // Set and activate current controller
                    currentControl = controller;
                    currentControl.setActive(true);
                    return;
                }
            }

            // Deactivate current controller, nothing is selected
            if (currentControl != null) {
                currentControl.setActive(false);
                currentControl = null;
            }
        }
}
