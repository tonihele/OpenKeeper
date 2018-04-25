/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 *
 * @author ArchDemon
 */
public class FunnyCameraContol  implements Control, JmeCloneable {
    protected Spatial target = null;

    protected float distance = 10;
    protected float height = 0.0f;

    protected boolean enabled = true;
    protected Camera cam = null;

    protected Vector3f initialUpVec;
    protected final Vector3f pos = new Vector3f();
    protected Vector3f lookAtOffset = new Vector3f(0, 0, 0);
    protected final Vector3f temp = new Vector3f(0, 0, 0);

    /**
     * Constructs the chase camera
     * @param cam the application camera
     * @param target the spatial to follow
     */
    public FunnyCameraContol(Camera cam, final Spatial target) {
        this(cam);
        target.addControl(this);
    }

    /**
     * Constructs the chase camera
     * if you use this constructor you have to attach the cam later to a spatial
     * doing spatial.addControl(chaseCamera);
     * @param cam the application camera
     */
    public FunnyCameraContol(Camera cam) {
        this.cam = cam;
        initialUpVec = cam.getUp().clone();
    }

    protected void computePosition() {

        float hDistance = (distance) * FastMath.sin((FastMath.PI / 2) - 0);
        pos.set(hDistance * FastMath.cos(0), (distance) * FastMath.sin(0), hDistance * FastMath.sin(0));
        pos.addLocal(target.getWorldTranslation());
    }

    /**
     * Updates the camera, should only be called internally
     * @param tpf
     */
    protected void updateCamera(float tpf) {
        if (!enabled) {
            return;
        }

        //targetLocation.set(target.getWorldTranslation());
        temp.set(target.getWorldTranslation()).addLocal(lookAtOffset);
        pos.set(temp).addLocal(0, height, 0);
        //easy no smooth motion
        //computePosition();
        //cam.setLocation(pos);
        cam.setRotation(target.getLocalRotation());
        cam.setLocation(pos.subtractLocal(cam.getDirection().mult(distance)));

        //the cam looks at the target
        cam.lookAt(temp, initialUpVec);
    }

    /**
     * Return the enabled/disabled state of the camera
     * @return true if the camera is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the camera
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * clone this camera for a spatial
     * @param spatial
     * @return
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        FunnyCameraContol cc = new FunnyCameraContol(cam, spatial);
        return cc;
    }

    @Override
    public Object jmeClone() {
        FunnyCameraContol cc = new FunnyCameraContol(cam);
        cc.target = target;
        return cc;
    }

    @Override
    public void cloneFields(Cloner cloner, Object original ) {
        this.target = cloner.clone(target);
        computePosition();
        cam.setLocation(pos);
    }

    /**
     * Sets the spacial for the camera control, should only be used internally
     * @param spatial
     */
    @Override
    public void setSpatial(Spatial spatial) {
        target = spatial;
        if (spatial == null) {
            return;
        }
        computePosition();
        cam.setLocation(pos);
    }

    /**
     * update the camera control, should only be used internally
     * @param tpf
     */
    @Override
    public void update(float tpf) {
        updateCamera(tpf);
    }

    /**
     * renders the camera control, should only be used internally
     * @param rm
     * @param vp
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
        //nothing to render
    }

    /**
     * Write the camera
     * @param ex the exporter
     * @throws IOException
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("remove ChaseCamera before saving");
    }

    /**
     * Read the camera
     * @param im
     * @throws IOException
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
    }

    /**
     * return the current distance from the camera to the target
     * @return
     */
    public float getDistanceToTarget() {
        return distance;
    }

    /**
     * returns the offset from the target's position where the camera looks at
     * @return
     */
    public Vector3f getLookAtOffset() {
        return lookAtOffset;
    }

    /**
     * Sets the offset from the target's position where the camera looks at
     * @param lookAtOffset
     */
    public void setLookAtOffset(Vector3f lookAtOffset) {
        this.lookAtOffset = lookAtOffset;
    }

    /**
     * Returns the up vector of the camera used for the lookAt on the target
     * @return
     */
    public Vector3f getUpVector() {
        return initialUpVec;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}