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
import com.jme3.math.Vector2f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.ai.creature.CreatureState;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.control.AbstractUnitFlowerControl;
import toniarts.openkeeper.world.listener.CreatureListener;

/**
 * Loads up creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class CreatureLoader implements ILoader<Thing.Creature>, CreatureListener {

    private final KwdFile kwdFile;
    private final WorldState worldState;

    private static final String START_ANIMATION_NAME = "Start";
    private static final String END_ANIMATION_NAME = "End";
    private static final String ANIM_NAME = "anim";
    private static final Logger logger = Logger.getLogger(CreatureLoader.class.getName());

    public CreatureLoader(KwdFile kwdFile, WorldState worldState) {
        this.kwdFile = kwdFile;
        this.worldState = worldState;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Creature object) {
        return load(assetManager, object, object.getCreatureId(), (short) 0, (short) 0);
    }

    public Spatial load(AssetManager assetManager, short creatureId, short playerId, short level) {
        return load(assetManager, null, creatureId, playerId, level);
    }

    private Spatial load(AssetManager assetManager, Thing.Creature object, short creatureId, short playerId, short level) {
        Creature creature = kwdFile.getCreature(creatureId);
        Node creatureRoot = new Node(creature.getName());
        CreatureControl creatureControl = new CreatureControl(object, creature, worldState, playerId, level) {

            @Override
            public void onSpawn(CreatureControl creature) {
                CreatureLoader.this.onSpawn(creature);
            }

            @Override
            public void onStateChange(CreatureControl creature, CreatureState newState, CreatureState oldState) {
                CreatureLoader.this.onStateChange(creature, newState, oldState);
            }

            @Override
            public void onDie(CreatureControl creature) {
                CreatureLoader.this.onDie(creature);
            }

        };

        // Set map position
        if (object != null) {
            creatureRoot.setLocalTranslation(
                    object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                    object.getPosZ() * MapLoader.TILE_HEIGHT,
                    object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);
        }

        // Add the creature control
        creatureRoot.addControl(creatureControl);

        // Creature flower
        AbstractUnitFlowerControl aufc = new CreatureUnitFlowerControl(assetManager, creatureControl);
        creatureRoot.addControl(aufc);

        return creatureRoot;
    }

    private static void attachResource(final Node creatureRoot, final CreatureControl creatureControl, final ArtResource resource, AssetManager assetManager) {
        if (resource != null && (resource.getSettings().getType() == ArtResource.Type.ANIMATING_MESH || resource.getSettings().getType() == ArtResource.Type.MESH || resource.getSettings().getType() == ArtResource.Type.PROCEDURAL_MESH)) {
            try {

                Spatial spat = loadModel(assetManager, resource.getName(), creatureRoot);
                spat.setName(resource.getName());

                // If the animations has end and/or start, it is located in a different file
                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                    String name = resource.getName() + START_ANIMATION_NAME;
                    Spatial spatStart = loadModel(assetManager, name, creatureRoot);
                    spatStart.setName(START_ANIMATION_NAME);

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
                                LoopMode loopMode = c.getLoopMode();
                                c.setAnim(ANIM_NAME, 0);
                                if (loopMode != null) {
                                    c.setLoopMode(loopMode);
                                }
                                animControl.setEnabled(true);

                                // Hide us
                                control.setEnabled(false);
                                control.getSpatial().setCullHint(Spatial.CullHint.Always);
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        animControl.createChannel();
                    }
                }
                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    String name = resource.getName() + END_ANIMATION_NAME;
                    Spatial spatEnd = loadModel(assetManager, name, creatureRoot);
                    spatEnd.setName(END_ANIMATION_NAME);

                    // Create kinda a custom animation control
                    AnimControl animControl = spatEnd.getControl(AnimControl.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        animControl.addListener(new AnimEventListener() {

                            @Override
                            public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                                // Stop us
                                control.setEnabled(false);

                                // Signal stop
                                creatureControl.onAnimationStop();
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        animControl.createChannel();
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

                                // Stop us
                                control.setEnabled(false);

                                // We need to stop
                                if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {

                                    // Hide us
                                    control.getSpatial().setCullHint(Spatial.CullHint.Always);

                                    Spatial spat = creatureRoot.getChild(END_ANIMATION_NAME);
                                    AnimControl animControl = (AnimControl) spat.getControl(0);
                                    spat.setCullHint(Spatial.CullHint.Inherit);
                                    AnimChannel c = animControl.getChannel(0);
                                    LoopMode loopMode = c.getLoopMode();
                                    c.setAnim(ANIM_NAME, 0);
                                    if (loopMode != null) {
                                        c.setLoopMode(loopMode);
                                    }
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
                    } else {
                        Anim.FrameFactorFunction func = Anim.FrameFactorFunction.valueOf(spat.getUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION));
                        switch (func) {
                            case CLAMP: {
                                channel.setLoopMode(LoopMode.Cycle);
                                break;
                            }
                            case WRAP: {
                                channel.setLoopMode(LoopMode.Loop);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage());
            }
        }
    }

    private static Spatial loadModel(AssetManager assetManager, String resourceName, Node creatureRoot) {

        // Load the model and attach it without the root
        Spatial model = ((Node) AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + resourceName + ".j3o", false)).getChild(0);
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
    static void playAnimation(Spatial spatial, ArtResource anim, AssetManager assetManager) {

        Node root = (Node) spatial;

        // Attach the anim node and get rid of the rest
        AbstractUnitFlowerControl aufc = root.getControl(AbstractUnitFlowerControl.class);
        for (Spatial child : root.getChildren()) {

            // Don't hide the unit flower
            if (aufc != null && aufc.getSpatial().equals(child)) {
                continue;
            }
            child.removeFromParent();
        }
        attachResource(root, root.getControl(CreatureControl.class), anim, assetManager);

        // Get the anim node
        String animNodeName = anim.getName();
        if (anim.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
            animNodeName = START_ANIMATION_NAME;
        }
        Spatial spat = root.getChild(animNodeName);
        if (spat != null) {

            // Hide all
            hideAllNodes(root);

            // Show the anim
            AnimControl animControl = (AnimControl) spat.getControl(0);
            spat.setCullHint(Spatial.CullHint.Inherit);
            AnimChannel channel = animControl.getChannel(0);
            LoopMode loopMode = channel.getLoopMode();
            channel.setAnim(ANIM_NAME, 0);
            if (loopMode != null) {
                channel.setLoopMode(loopMode);
            }
            animControl.setEnabled(true);
        }
    }

    private static void hideAllNodes(Node root) {
        AbstractUnitFlowerControl aufc = root.getControl(AbstractUnitFlowerControl.class);
        for (Spatial child : root.getChildren()) {

            // Don't hide the unit flower
            if (aufc != null && aufc.getSpatial().equals(child)) {
                continue;
            }

            child.setCullHint(Spatial.CullHint.Always);

            // Also stop any animations
            AnimControl animControl = (AnimControl) child.getControl(AnimControl.class);
            if (animControl != null) {
                animControl.setEnabled(false);
            }
        }
    }

    public static void setPosition(Spatial creature, Vector2f position) {
        creature.setLocalTranslation(
                position.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0 * MapLoader.TILE_HEIGHT,
                position.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        // Need to re-adjust the steering
        CreatureControl creatureControl = creature.getControl(CreatureControl.class);
        creatureControl.setSpatial(creature);
    }

    /**
     * Restarts all animations that were playing
     *
     * @param spatial the root node of the creature
     */
    public static void resumeAnimations(Spatial spatial) {
        setAnimSpeeds((Node) spatial, 1.0f);
    }

    /**
     * Stops all animations that were playing
     *
     * @param spatial the root node of the creature
     */
    public static void pauseAnimations(Spatial spatial) {
        setAnimSpeeds((Node) spatial, 0.0f);
    }

    private static void setAnimSpeeds(Node node, float speed) {
        for (Spatial child : node.getChildren()) {
            AnimControl animControl = (AnimControl) child.getControl(AnimControl.class);
            if (animControl != null) {
                animControl.getChannel(0).setSpeed(speed);
            }
        }
    }

    static void showUnitFlower(CreatureControl creature, Integer seconds) {
        AbstractUnitFlowerControl aufc = creature.getSpatial().getControl(AbstractUnitFlowerControl.class);
        if (seconds != null) {
            aufc.show(seconds);
        } else {
            aufc.show();
        }
    }

}
