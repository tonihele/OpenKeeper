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
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
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
        Creature creature = kwdFile.getCreature(object.getCreatureId());
        Node creatureRoot = new Node(creature.getName());
        CreatureControl creatureControl = new CreatureControl(object, creature, worldHandler);

        // Load all the resources
        attachResource(creatureRoot, creatureControl, creature.getAnimAngryResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDanceResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDejectedPoseResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDiePoseResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDieResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDraggedPoseResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDrinkResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDrunk2Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimDrunkResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimEatResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimElecResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimElectrocuteResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimEntranceResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimFallbackResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimGetUpResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimHappyResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimIdle1Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimIdle2Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimMagicResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimMelee1Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimMelee2Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimPoseFrameResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimPrayResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimRecoilHfbResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimRecoilHffResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimResearchResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimRunResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimSleepResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimStunnedPoseResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimSwingResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimTortureResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimWalk2Resource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimWalkResource(), assetManager);
        attachResource(creatureRoot, creatureControl, creature.getAnimWalkbackResource(), assetManager);

        // Set map position
        creatureRoot.setLocalTranslation(
                object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                object.getPosZ() * MapLoader.TILE_HEIGHT,
                object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        // Add the creature control
        creatureRoot.addControl(creatureControl);

        return creatureRoot;
    }

    private void attachResource(Node creatureRoot, CreatureControl creatureControl, ArtResource resource, AssetManager assetManager) {
        if (resource != null && (resource.getSettings().getType() == ArtResource.Type.ANIMATING_MESH || resource.getSettings().getType() == ArtResource.Type.MESH || resource.getSettings().getType() == ArtResource.Type.PROCEDURAL_MESH)) {
            try {

                Spatial spat = loadModel(assetManager, resource.getName(), creatureRoot);

                // If the animations has end and/or start, it is located in a different file
                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                    Spatial spatStart = loadModel(assetManager, resource.getName() + "Start", creatureRoot);

                    // Create kinda a custom animation control
                    AnimControl animControl = spatStart.getControl(AnimControl.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        animControl.addListener(new AnimEventListener() {

                            @Override
                            public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                                // Start the real animation
                                Spatial spat = creatureRoot.getChild(resource.getName());
                                AnimControl animControl = (AnimControl) spat.getControl(0);
                                spat.setCullHint(Spatial.CullHint.Inherit);
                                AnimChannel c = animControl.getChannel(0);
                                c.setAnim("anim", 0);
                                animControl.setEnabled(true);

                                // Hide us
                                control.setEnabled(false);
                                control.getSpatial().setCullHint(Spatial.CullHint.Always);
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        AnimChannel channel = animControl.createChannel();
                    }
                }
                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    Spatial spatEnd = loadModel(assetManager, resource.getName() + "End", creatureRoot);

                    // Create kinda a custom animation control
                    AnimControl animControl = spatEnd.getControl(AnimControl.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        animControl.addListener(new AnimEventListener() {

                            @Override
                            public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                                // Hide us
                                control.getSpatial().setCullHint(Spatial.CullHint.Always);
                                control.setEnabled(false);

                                // Signal stop
                                creatureControl.onAnimationStop();
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        AnimChannel channel = animControl.createChannel();
                    }
                }

                // Create kinda a custom animation control
                AnimControl animControl = spat.getControl(AnimControl.class);
                if (animControl != null) {
                    animControl.setEnabled(false);
                    animControl.addListener(new AnimEventListener() {

                        @Override
                        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                            // Signal that the main animation cycle is done
                            creatureControl.onAnimationCycleDone();

                            // See if we need to stop
                            if (creatureControl.isStopAnimation() || channel.getLoopMode() == LoopMode.DontLoop) {

                                // Hide us
                                control.getSpatial().setCullHint(Spatial.CullHint.Always);
                                control.setEnabled(false);

                                // We need to stop
                                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                                    Spatial spat = creatureRoot.getChild(resource.getName() + "End");
                                    AnimControl animControl = (AnimControl) spat.getControl(0);
                                    spat.setCullHint(Spatial.CullHint.Inherit);
                                    AnimChannel c = animControl.getChannel(0);
                                    c.setAnim("anim", 0);
                                    animControl.setEnabled(true);
                                } else {

                                    // Signal stop
                                    creatureControl.onAnimationStop();
                                }
                            }
                        }

                        @Override
                        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                        }
                    });
                    AnimChannel channel = animControl.createChannel();
                    if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.DOESNT_LOOP)) {
                        channel.setLoopMode(LoopMode.DontLoop);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Model not found!", e);
            }
        }
    }

    private Spatial loadModel(AssetManager assetManager, String resourceName, Node creatureRoot) {

        // Load the model and attach it without the root
        Node modelRoot = (Node) assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + resourceName + ".j3o");
        Spatial model = modelRoot.getChild(0);
        model.setCullHint(Spatial.CullHint.Always);
        creatureRoot.attachChild(model);
        return model;
    }

    /**
     * Start playing an animation on the creature
     *
     * @param spatial the creature root
     * @param anim wanted animation
     */
    static void playAnimation(Spatial spatial, ArtResource anim) {

        Node root = (Node) spatial;

        // Get the anim node
        String animNodeName = anim.getName();
        if (anim.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
            animNodeName = anim.getName() + "Start";
        }
        Spatial spat = root.getChild(animNodeName);
        if (spat != null) {

            // Hide all
            hideAllNodes(root);

            // Show the anim
            AnimControl animControl = (AnimControl) spat.getControl(0);
            spat.setCullHint(Spatial.CullHint.Inherit);
            AnimChannel channel = animControl.getChannel(0);
            channel.setAnim("anim", 0);
            animControl.setEnabled(true);
        }
    }

    private static void hideAllNodes(Node root) {
        for (Spatial child : root.getChildren()) {
            child.setCullHint(Spatial.CullHint.Always);

            // Also stop any animations
            AnimControl animControl = (AnimControl) child.getControl(AnimControl.class);
            if (animControl != null) {
                animControl.setEnabled(false);
            }
        }
    }

}
