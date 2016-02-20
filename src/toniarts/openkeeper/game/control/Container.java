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
package toniarts.openkeeper.game.control;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.util.SafeArrayList;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author ArchDemon
 */


public class Container implements IContainer {

    protected SafeArrayList<IControl> controls = new SafeArrayList<>(IControl.class);

    private void updateControl(float tpf) {
        if (controls.isEmpty()) {
            return;
        }

        for (IControl c : controls.getArray()) {
            c.update(tpf);
        }
    }

    /**
     * Add a control to the list of controls.
     *
     * @param control The control to add.
     *
     * @see Container#removeControl(java.lang.Class)
     */
    @Override
    public void addControl(IControl control) {
        controls.add(control);
        control.setParent(this);
    }

    /**
     * Removes the first control that is an instance of the given class.
     *
     * @see Container#addControl(IControl)
     */
    public void removeControl(Class<? extends IControl> controlType) {
        for (int i = 0; i < controls.size(); i++) {
            if (controlType.isAssignableFrom(controls.get(i).getClass())) {
                IControl control = controls.remove(i);
                control.setParent(null);
            }
        }
    }

    /**
     * Removes the given control from this Container's controls.
     *
     * @param control The control to remove
     * @return True if the control was successfuly removed. False if the control is not assigned to this Container.
     *
     * @see Container#addControl(IControl)
     */
    @Override
    public boolean removeControl(IControl control) {
        boolean result = controls.remove(control);
        if (result) {
            control.setParent(null);
        }

        return result;
    }

    /**
     * Returns the first control that is an instance of the given class, or null if no such control exists.
     *
     * @param controlType The superclass of the control to look for.
     * @return The first instance in the list of the controlType class, or null.
     *
     * @see Container#addControl(IControl)
     */
    public <T extends IControl> T getControl(Class<T> controlType) {
        for (IControl c : controls.getArray()) {
            if (controlType.isAssignableFrom(c.getClass())) {
                return (T) c;
            }
        }
        return null;
    }

    /**
     * Returns the control at the given index in the list.
     *
     * @param index The index of the control in the list to find.
     * @return The control at the given index.
     *
     * @throws IndexOutOfBoundsException If the index is outside the range [0, getNumControls()-1]
     *
     * @see Container#addControl(IControl)
     */
    @Override
    public IControl getControl(int index) {
        return controls.get(index);
    }

    /**
     * @return The number of controls attached to this Spatial.
     * @see Container#addControl(IControl)
     * @see Container#removeControl(java.lang.Class)
     */
    public int getNumControls() {
        return controls.size();
    }

    /**
     * <code>updateLogicalState</code> calls the
     * <code>updateContol()</code> method for all controls attached to this Container.
     *
     * @param tpf Time per frame.
     *
     * @see Container#addControl(IControl)
     */
    @Override
    public void update(float tpf) {
        updateControl(tpf);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.writeSavableArrayList(new ArrayList(controls), "controlsList", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        controls.addAll(0, ic.readSavableArrayList("controlsList", null));
    }
}
