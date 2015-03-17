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
package toniarts.openkeeper.cinematics;

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

    public CameraSweepDataEntry(Vector3f position, Vector3f direction, Vector3f left, Vector3f up, float fov, float near) {
        this.position = position;
        this.direction = direction;
        this.left = left;
        this.up = up;
        this.fov = fov;
        this.near = near;
    }

    /**
     * Serialization-only. Do not use.
     */
    public CameraSweepDataEntry() {
    }

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
