/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.effect.EffectManagerState;

/**
 *
 * @author ArchDemon
 */
public class RoomsLoader implements ILoader<Room> {

    @Override
    public Spatial load(AssetManager assetManager, Room object) {
        //Create a root
        Node root = new Node(object.getName());

        List<ArtResource> resources = new ArrayList<>();
        resources.add(object.getCapResource());
        resources.add(object.getCeilingResource());
        resources.add(object.getCompleteResource());
        resources.add(object.getEditorIcon());
        resources.add(object.getGuiIcon());
        resources.add(object.getInsideCornerResource());
        resources.add(object.getOutsideCornerResource());
        resources.add(object.getStraightResource());
        resources.add(object.getTorch());
        resources.add(object.getUnknownResource());
        resources.add(object.getWallResource());

        float height = 1;
        for (ArtResource resource : resources) {
            if (resource == null || resource.getType() == ArtResource.ArtResourceType.NONE) {
                continue;
            }


            Spatial s = UniversalArtResourceLoader.load(assetManager, resource);
            s.move(0, height++, 0);
            root.attachChild(s);
        }

        return root;
    }

    public Spatial load(AssetManager assetManager, EffectManagerState effectManagerState, Room object) {
        Spatial root = load(assetManager, object);

        List<Integer> effects = new ArrayList<>();
        effects.add(object.getFightEffectId());

        float height = 1;
        for (Integer effectId : effects) {
            if (effectId == 0) {
                continue;
            }

            effectManagerState.loadSingleEffect((Node) root, new Vector3f(1, height++, 1), effectId, true);
        }

        return root;
    }
}
