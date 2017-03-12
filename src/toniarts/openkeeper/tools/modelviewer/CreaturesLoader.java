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
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.effect.EffectManagerState;

/**
 *
 * @author ArchDemon
 */
public class CreaturesLoader implements ILoader<Creature> {

    @Override
    public Spatial load(AssetManager assetManager, Creature object) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Frontend10.fnt");
        //Create a root
        Node root = new Node(object.getName());

        List<Item> resources = new ArrayList<>();
        resources.add(new Item(object.getAnimAngryResource(), "AnimAngry", new Vector3f(-3, 0, -4)));
        resources.add(new Item(object.getAnimDanceResource(), "AnimDance", new Vector3f(-2, 0, -4)));
        resources.add(new Item(object.getAnimDejectedPoseResource(), "AnimDejected", new Vector3f(-1, 0, -4)));
        resources.add(new Item(object.getAnimDiePoseResource(), "AnimDiePose", new Vector3f(0, 0, -4)));
        resources.add(new Item(object.getAnimDieResource(), "AnimDie", new Vector3f(1, 0, -4)));
        resources.add(new Item(object.getAnimDraggedPoseResource(), "AnimDraggedPose", new Vector3f(2, 0, -4)));
        resources.add(new Item(object.getAnimDrinkResource(), "AnimDrink", new Vector3f(3, 0, -4)));
        resources.add(new Item(object.getAnimDrunk2Resource(), "AnimDrunk2", new Vector3f(-3, 0, -3)));
        resources.add(new Item(object.getAnimDrunkResource(), "AnimDrunk", new Vector3f(-2, 0, -3)));
        resources.add(new Item(object.getAnimEatResource(), "AnimEat", new Vector3f(-1, 0, -3)));
        resources.add(new Item(object.getAnimElecResource(), "AnimElec", new Vector3f(0, 0, -3)));
        resources.add(new Item(object.getAnimElectrocuteResource(), "AnimElectrocute", new Vector3f(1, 0, -3)));
        resources.add(new Item(object.getAnimEntranceResource(), "AnimEntrance", new Vector3f(2, 0, -3)));
        resources.add(new Item(object.getAnimFallbackResource(), "AnimFallback", new Vector3f(3, 0, -3)));
        resources.add(new Item(object.getAnimGetUpResource(), "AnimGetUp", new Vector3f(-3, 0, -2)));
        resources.add(new Item(object.getAnimHappyResource(), "AnimHappy", new Vector3f(-2, 0, -2)));
        resources.add(new Item(object.getAnimIdle1Resource(), "AnimIdle1", new Vector3f(-1, 0, -2)));
        resources.add(new Item(object.getAnimIdle2Resource(), "AnimIdle2", new Vector3f(0, 0, -2)));
        resources.add(new Item(object.getAnimInHandResource(), "AnimInHand", new Vector3f(1, 0, -2)));
        resources.add(new Item(object.getAnimMagicResource(), "AnimMagic", new Vector3f(2, 0, -2)));
        resources.add(new Item(object.getAnimMelee1Resource(), "AnimMelee1", new Vector3f(3, 0, -2)));
        resources.add(new Item(object.getAnimMelee2Resource(), "AnimMelee2", new Vector3f(-3, 0, -1)));
        resources.add(new Item(object.getAnimPoseFrameResource(), "AnimPoseFrame", new Vector3f(-2, 0, -1)));
        resources.add(new Item(object.getAnimPrayResource(), "AnimPray", new Vector3f(-1, 0, -1)));
        resources.add(new Item(object.getAnimRecoilHfbResource(), "AnimRecoilHfb", new Vector3f(0, 0, -1)));
        resources.add(new Item(object.getAnimRecoilHffResource(), "AnimRecoilHff", new Vector3f(1, 0, -1)));
        resources.add(new Item(object.getAnimResearchResource(), "AnimResearch", new Vector3f(2, 0, -1)));
        resources.add(new Item(object.getAnimRunResource(), "AnimRun", new Vector3f(3, 0, -1)));
        resources.add(new Item(object.getAnimSleepResource(), "AnimSleep", new Vector3f(-3, 0, 0)));
        resources.add(new Item(object.getAnimStunnedPoseResource(), "AnimStunnedPose", new Vector3f(-2, 0, 0)));
        resources.add(new Item(object.getAnimTortureResource(), "AnimTorture", new Vector3f(-1, 0, 0)));
        resources.add(new Item(object.getAnimWalk2Resource(), "AnimWalk2", new Vector3f(0, 0, 0)));
        resources.add(new Item(object.getAnimWalkResource(), "AnimWalk", new Vector3f(1, 0, 0)));
        resources.add(new Item(object.getAnimWalkbackResource(), "AnimWalkback", new Vector3f(2, 0, 0)));

        resources.add(new Item(object.getDrunkIdle(), "DrunkIdle", new Vector3f(3, 0, 0)));
        resources.add(new Item(object.getFirstPersonFilterResource(), "FirstPersonFilter", new Vector3f(-3, 0, 1)));
        resources.add(new Item(object.getFirstPersonMeleeResource(), "FirstPersonMelee", new Vector3f(-2, 0, 1)));
        resources.add(new Item(object.getIcon1Resource(), "Icon1", new Vector3f(-1, 0, 1)));
        resources.add(new Item(object.getIcon2Resource(), "Icon2", new Vector3f(0, 0, 1)));
        resources.add(new Item(object.getPortraitResource(), "Portrait", new Vector3f(1, 0, 1)));
        resources.add(new Item(object.getUniqueResource(), "Unique", new Vector3f(2, 0, 1)));

        resources.add(new Item(object.getUnknown13Resource(), "Unknown13", new Vector3f(3, 0, 1)));
        resources.add(new Item(object.getUnknown12Resource(), "Unknown12", new Vector3f(-3, 0, 2)));
        resources.add(new Item(object.getUnknown11Resource(), "Unknown11", new Vector3f(-2, 0, 2)));
        resources.add(new Item(object.getUnknown10Resource(), "Unknown10", new Vector3f(-1, 0, 2)));
        resources.add(new Item(object.getUnknown9Resource(), "Unknown9", new Vector3f(0, 0, 2)));
        resources.add(new Item(object.getUnknown8Resource(), "Unknown8", new Vector3f(1, 0, 2)));
        resources.add(new Item(object.getUnknown7Resource(), "Unknown7", new Vector3f(2, 0, 2)));
        resources.add(new Item(object.getUnknown6Resource(), "Unknown6", new Vector3f(3, 0, 2)));
        resources.add(new Item(object.getUnknown5Resource(), "Unknown5", new Vector3f(-3, 0, 3)));
        resources.add(new Item(object.getUnknown4Resource(), "Unknown4", new Vector3f(-2, 0, 3)));
        resources.add(new Item(object.getUnknown3Resource(), "Unknown3", new Vector3f(-1, 0, 3)));
        resources.add(new Item(object.getUnknown2Resource(), "Unknown2", new Vector3f(0, 0, 3)));

        for (Item item : resources) {
            if (item.resource == null || item.resource.getType() == ArtResource.ArtResourceType.NONE) {
                continue;
            }

            Node part = new Node(item.name);
            Spatial s = UniversalArtResourceLoader.load(assetManager, item.resource);
            part.attachChild(s);

            BitmapText text = new BitmapText(font, false);
            text.setText(item.name);
            text.setSize(0.1f);
            text.move(-text.getLineWidth() / 2, 1, 0);
            BillboardControl bc = new BillboardControl();
            bc.setAlignment(BillboardControl.Alignment.Camera);
            text.addControl(bc);
            part.attachChild(text);

            part.setLocalTranslation(item.position);

            root.attachChild(part);
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

            effectManagerState.loadSingleEffect((Node) root, new Vector3f(1, height++, 1), effectId, true);
        }

        return root;
    }

    private class Item {
        public ArtResource resource;
        public String name;
        public Vector3f position;

        public Item(ArtResource resource, String name, Vector3f position) {
            this.resource = resource;
            this.name = name;
            this.position = position;
        }
    }
}
