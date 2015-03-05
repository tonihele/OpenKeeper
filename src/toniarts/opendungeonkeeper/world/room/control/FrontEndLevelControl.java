/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.world.room.control;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * Controls level graphics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FrontEndLevelControl extends AbstractControl {

    private final int level;
    private final String variation;
    private volatile boolean active = false;
    private volatile boolean moved = true;

    public FrontEndLevelControl(int level, String variation) {
        this.level = level;
        this.variation = variation;
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (moved == false) {
            if (active) {
                getSpatial().move(0, 0.05f, 0);
            } else {
                getSpatial().move(0, -0.05f, 0);
            }
            moved = true;
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        /* Optional: rendering manipulation (for advanced users) */
    }

    /**
     * Sets this controller active (not enabled/disabled)
     *
     * @param active active status
     */
    public void setActive(boolean active) {
        if (active != this.active) {
            this.moved = false;
        }
        this.active = active;
    }

    public int getLevel() {
        return level;
    }

    public String getVariation() {
        return variation;
    }
}
