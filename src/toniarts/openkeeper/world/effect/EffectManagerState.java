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
package toniarts.openkeeper.world.effect;

import com.jme3.app.state.AbstractAppState;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * An app state to manage ALL the effects in the world. Mainly their lifetime.
 *
 * @author ArchDemon
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EffectManagerState extends AbstractAppState {

    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final List<VisualEffect> activeEffects = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(EffectManagerState.class.getName());

    public EffectManagerState(KwdFile kwdFile, AssetManager assetManager) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
    }

    @Override
    public void update(float tpf) {

        // Maintain the effects (on every frame?)
        Iterator<VisualEffect> iterator = activeEffects.iterator();
        while (iterator.hasNext()) {
            VisualEffect visualEffect = iterator.next();
            if (!visualEffect.update(tpf)) {
                iterator.remove();
            }
        }
    }

    /**
     * Loads up an particle effect
     *
     * @param node the node to attach the effect to
     * @param location particle effect node location, maybe {@code null}
     * @param effectId the effect ID to load
     * @param infinite the effect should restart always, infinite effect (room
     * effects...?)
     */
    public void loadSingleEffect(Node node, Vector3f location, int effectId, boolean infinite) {

        // Load the effect
        VisualEffect visualEffect = new VisualEffect(kwdFile, assetManager, this, node, location, kwdFile.getEffect(effectId), infinite);
        clearActiveEffects();
        activeEffects.add(visualEffect);
    }

    private void clearActiveEffects() {
        activeEffects.clear();
    }

    /**
     * Loads up an particle effect
     *
     * @param node the node to attach the effect to
     * @param location particle effect node location, maybe {@code null}
     * @param effectId the effect ID to load
     * @param infinite the effect should restart always, infinite effect (room
     * effects...?)
     */
    public void load(Node node, Vector3f location, int effectId, boolean infinite) {

        // Load the effect
        VisualEffect visualEffect = new VisualEffect(kwdFile, assetManager, this, node, location, kwdFile.getEffect(effectId), infinite);
        activeEffects.add(visualEffect);
    }

}
