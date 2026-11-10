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
package toniarts.openkeeper.view.loader;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Loads up traps
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrapLoader implements ILoader<TrapViewState> {
    
    private static final Logger logger = System.getLogger(TrapLoader.class.getName());

    private final IKwdFile kwdFile;

    public TrapLoader(IKwdFile kwdFile) {
        this.kwdFile = kwdFile;
    }

    @Override
    public Spatial load(AssetManager assetManager, TrapViewState trap) {
        try {
            ArtResource artResource = kwdFile.getTrapById(trap.trapId).getMeshResource();
            Node nodeObject = (Node) AssetUtils.loadModel(assetManager, artResource.getName(), artResource);
            return nodeObject;
        } catch (Exception e) {
            logger.log(Level.ERROR, "Failed to load trap " + trap + "!", e);
        }
        return null;
    }
}
