/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

public class KeeperSpellResearchControl extends AbstractNiftyJmeControl {

    public static final String CONTROL_NAME = "keeperSpellResearch";

    private Geometry geo;
    private boolean initialized = false;
    private float research = 0;

    @Override
    public void initJme(SimpleApplication app) {
        super.initJme(app);

        Quad quad = new Quad(getControlWidth(), getControlHeight());
        geo = new Geometry("OurQuad", quad);
        Material mat = new Material(app.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geo.setMaterial(mat);
        geo.addControl(new SimpleJmeControl(this));
        geo.setLocalTranslation(getControlX(), getControlY(), 0);
    }

    @Override
    protected void cleanup() {
        getApp().getGuiNode().detachChild(geo);
    }

    public void setResearch(float research) {
        this.research = research;

        if (!initialized && research > 0) {
            initialized = true;
            getApp().getGuiNode().attachChild(geo);
        }
    }

}
