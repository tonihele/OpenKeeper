/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.cinematics;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * A camera sweep waypoint entry
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CameraSweepDataEntry implements Savable {

    private Vector3f position;
    private Vector3f direction;
    private Vector3f left;
    private Vector3f up;
    private float fov;
    private float near;

    public Vector3f getPosition() {
        return position;
    }

    /**
     * Forward orientation vector
     *
     * @return Forward orientation
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Left orientation vector
     *
     * @return Left orientation
     */
    public Vector3f getLeft() {
        return left;
    }

    /**
     * Up orientation vector
     *
     * @return Up orientation
     */
    public Vector3f getUp() {
        return up;
    }

    /**
     * Field of view (in radians)
     *
     * @return Field of view
     */
    public float getFov() {
        return fov;
    }

    /**
     * Near clipping distance in fixed units
     *
     * @return Near clipping distance
     */
    public float getNear() {
        return near;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(position, "position", null);
        out.write(direction, "direction", null);
        out.write(left, "left", null);
        out.write(up, "up", null);
        out.write(fov, "fov", Float.NaN);
        out.write(near, "near", Float.NaN);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        position = (Vector3f) in.readSavable("position", null);
        direction = (Vector3f) in.readSavable("direction", null);
        left = (Vector3f) in.readSavable("left", null);
        up = (Vector3f) in.readSavable("up", null);
        fov = in.readFloat("fov", Float.NaN);
        near = in.readFloat("near", Float.NaN);
    }
}
