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
package toniarts.openkeeper.world.creature;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldHandler;

/**
 * Loads up creatures. TODO: Should perhaps keep a cache of loaded/constructed
 * creatures...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureLoader implements ILoader<Thing.Creature> {

    private final KwdFile kwdFile;
    private final WorldHandler worldHandler;

    public CreatureLoader(KwdFile kwdFile, WorldHandler worldHandler) {
        this.kwdFile = kwdFile;
        this.worldHandler = worldHandler;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Creature object) {
        String modelName = this.getModelFileName(object);
        Node model = (Node) assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + ".j3o");
        model.setLocalTranslation(
                object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                object.getPosZ() * MapLoader.TILE_HEIGHT,
                object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        AnimControl animControl = (AnimControl) model.getChild(0).getControl(AnimControl.class);
        if (animControl != null) {
            AnimChannel channel = animControl.createChannel();
            channel.setAnim("anim");
            channel.setLoopMode(LoopMode.Loop);

            // Don't batch animated objects, seems not to work
            model.setBatchHint(Spatial.BatchHint.Never);
        }

        // Add the creature control
        model.addControl(new CreatureControl(object, kwdFile.getCreature(object.getCreatureId()), worldHandler));

        //Geometry geom = (Geometry) model.getChild(0);
//        model.getChild(0).addControl(new RigidBodyControl(0));
        return model;
    }

    private String getModelFileName(Thing.Creature object) {
        try {
            return kwdFile.getCreature(object.getCreatureId()).getAnimWalkResource().getName();
        } catch (Exception e) {
            throw new RuntimeException("model not found");
        }
    }

}
