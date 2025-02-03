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
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.world.effect.EffectManagerState;

/**
 *
 * @author ArchDemon
 */
public final class CreaturesLoader implements ILoader<Creature> {

    @Override
    public Spatial load(AssetManager assetManager, Creature object) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Frontend10.fnt");
        //Create a root
        Node root = new Node(object.getName());

        float x = -5;
        float z = -5;
        for (AnimationType type : AnimationType.values()) {

            ArtResource animation = object.getAnimation(type);
            if (animation != null && animation.getType() != ArtResource.ArtResourceType.NONE) {
                Node part = new Node(type.toString());
                Spatial s = UniversalArtResourceLoader.load(assetManager, animation);
                part.attachChild(s);

                BitmapText text = new BitmapText(font, false);
                text.setText(type.toString() + "\n" + animation.getName());
                text.setSize(0.1f);
                text.move(-text.getLineWidth() / 2, 1, 0);
    //            BillboardControl bc = new BillboardControl();
    //            bc.setAlignment(BillboardControl.Alignment.Camera);
    //            text.addControl(bc);
                part.attachChild(text);
                part.setLocalTranslation(x, 0, z);
                root.attachChild(part);
            }

            x += 1.5f;
            if (x > 5) {
                z += 1.5f;
                x = -5;
            }
        }

        return root;
    }

    public Spatial load(AssetManager assetManager, EffectManagerState effectManagerState, Creature object) {
        Spatial root = load(assetManager, object);

        List<Integer> effects = new ArrayList<>();
        effects.add(object.getEntranceEffectId());
        effects.add(object.getSlapEffectId());
        effects.add(object.getOlhiEffectId());
        effects.add(object.getOlhiEffectId());
        effects.add(object.getDeathEffectId());

        float height = 1;
        for (Integer effectId : effects) {
            if (effectId == 0) {
                continue;
            }

            effectManagerState.loadSingleEffect((Node) root, new Vector3f(0, height++, 0), effectId, true);
        }

        return root;
    }
}
