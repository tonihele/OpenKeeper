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
package toniarts.openkeeper.view.effect;

import com.jme3.effect.shapes.EmitterShape;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import toniarts.openkeeper.tools.convert.map.Effect;

import java.io.IOException;

/**
 * Samples a {@code CUBE_GEN} effect's annulus/height-band spawn volume
 * (innerOriginRange/outerOriginRange, lowerHeightLimit/upperHeightLimit)
 * see {@link com.jme3.effect.ParticleEmitter}, so sprite-type elements (e.g. the
 * front-end gems' sparkles) scatter the same way the mesh-type elements do
 * instead of all emitting from one point.
 */
public class EmitterCubeGenShape implements EmitterShape {

    private Effect effect;

    /**
     * For serialization only. Do not use.
     */
    public EmitterCubeGenShape() {
    }

    public EmitterCubeGenShape(Effect effect) {
        this.effect = effect;
    }

    @Override
    public void getRandomPoint(Vector3f store) {
        store.set(EffectControl.randomOriginOffset(effect));
    }

    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        getRandomPoint(store);
        normal.set(Vector3f.UNIT_Y);
    }

    @Override
    public EmitterShape deepClone() {
        return new EmitterCubeGenShape(effect);
    }

    @Override
    public Object jmeClone() {
        return new EmitterCubeGenShape(effect);
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        // Effect is shared, immutable reference data - nothing to clone.
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        // Effects are never serialized to disk.
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        // Effects are never serialized to disk.
    }
}
