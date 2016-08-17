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
import java.io.IOException;
/**
 *
 * @author ArchDemon
 */
public abstract class Control implements IControl {

    protected boolean enabled = true;
    protected IContainer parent;

    /**
     * empty serialization constructor
     */
    public Control() {
    }

    @Override
    public void setParent(IContainer parent) {
        if (this.parent != null && parent != null && parent != this.parent) {
            throw new IllegalStateException("This control has already been added to a parent");
        }
        this.parent = parent;
    }

    public IContainer getParent() {
        return parent;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * To be implemented in subclass.
     * @param tpf time per frame
     */
    protected abstract void updateControl(float tpf);

    @Override
    public void update(float tpf) {
        if (enabled) {
            updateControl(tpf);
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(parent, "parent", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        parent = (IContainer) ic.readSavable("parent", null);
    }
}
