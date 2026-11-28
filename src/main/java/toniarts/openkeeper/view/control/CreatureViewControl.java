/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.view.control;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.action.Action;
import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.controller.DropValidator;
import toniarts.openkeeper.game.controller.SlapRules;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.sound.FootstepSounds;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.text.EntityTextParser;

/**
 * View control that is intended specifically for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureViewControl extends EntityViewControl<Creature, Creature.AnimationType> {

    private static final long IN_HAND_SOUND_INTERVAL_MS = 8000;
    private static final long DIG_SOUND_RETRY_MS = 250;
    /**
     * The skeletons DK II shows on the Electric Chair for the creatures that
     * don't have their own, picked at random for each shock
     */
    private static final ArtResource[] ELECTROCUTE_SKELETONS = {
        new ArtResource("Electrocute1", ArtResource.ArtResourceType.ANIMATING_MESH),
        new ArtResource("Electrocute2", ArtResource.ArtResourceType.ANIMATING_MESH),
        new ArtResource("Electrocute3", ArtResource.ArtResourceType.ANIMATING_MESH),
        new ArtResource("Electrocute4", ArtResource.ArtResourceType.ANIMATING_MESH),
        new ArtResource("Electrocute5", ArtResource.ArtResourceType.ANIMATING_MESH),
        new ArtResource("Electrocute6", ArtResource.ArtResourceType.ANIMATING_MESH)
    };

    private long nextInHandSound;
    private int researchSound = SoundEngine.NO_HANDLE;
    private int digSound = SoundEngine.NO_HANDLE;
    private long nextDigSound;
    private static final Logger LOGGER = System.getLogger(CreatureViewControl.class.getName());

    private float previousStepTime = -1;
    private boolean stepWarningLogged;
    private Predicate<Vector3f> waterCheck = position -> false;
    private int ticksPerSecond;
    private Vector3f walkedFrom;

    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation,
            AssetManager assetManager, EntityTextParser<Creature> textParser) {
        super(entityId, entityData, data, animation, assetManager, textParser);
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(CreatureAi.class);
        components.add(CreatureImprisoned.class);
        components.add(CreatureComponent.class);
        return components;
    }

    @Override
    public DroppableStatus getDroppableStatus(IMapTileInformation tile, Terrain terrain, short playerId) {
        CreatureComponent creatureComponent = getEntity().get(CreatureComponent.class);
        boolean tool = creatureComponent != null && creatureComponent.tool;
        return DropValidator.canDrop(getEntity().get(Interaction.class), tool, tile, terrain, playerId) ? DroppableStatus.DROPPABLE : DroppableStatus.NOT_DROPPABLE;
    }

    @Override
    public boolean isSlappable(short playerId) {
        Interaction interaction = getEntity().get(Interaction.class);
        if (interaction == null || !interaction.slappable) {
            return false;
        }

        boolean imprisoned = getEntity().get(CreatureImprisoned.class) != null;
        return SlapRules.canSlapCreature(playerId, getOwnerId(), getControlId(), imprisoned);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && currentState != null) {
            playAnimation(currentState);
        }
    }

    @Override
    public void cleanup() {
        playResearchSound(null);
        digSound = SoundEngine.NO_HANDLE;
        super.cleanup();
    }

    @Override
    public String getTooltip(short playerId) {

        // Like DK II, only own creatures, heroes and neutral creatures have a tooltip, other keepers' creatures have none
        String tooltip;
        if (getOwnerId() == playerId) {
            tooltip = Utils.getMainTextResourceBundle().getString("2841");
        } else if (getOwnerId() == Player.GOOD_PLAYER_ID) {
            tooltip = Utils.getMainTextResourceBundle().getString("2412");
        } else if (getOwnerId() == Player.NEUTRAL_PLAYER_ID) {
            tooltip = Utils.getMainTextResourceBundle().getString("2415");
        } else {
            return "";
        }

        return textParser.parseText(tooltip, getEntity(), getDataObject());
    }


    @Override
    public boolean isStopAnimation() {
        return super.isStopAnimation() || (currentState != Creature.AnimationType.WALK && currentState != Creature.AnimationType.DRUNKED_WALK
                && currentState != Creature.AnimationType.IN_HAND && !isWorkAnimation());
    }

    private void playAnimation(Creature.AnimationType animation) {
        AnimationLoader.playAnimation(getSpatial(), getAnimationData(animation), assetManager);
        if (getDataObject().getFlags().contains(CreatureFlag.IS_UNIQUE)) {
            CreatureVariantTextures.apply(getSpatial(), assetManager);
        }
        isAnimationPlaying = true;
        currentState = animation;
        previousStepTime = -1;
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        // A creature in the hand talks, one line every few seconds as long as it is held. It is heard at the hand
        if (currentState == Creature.AnimationType.IN_HAND) {
            long now = System.currentTimeMillis();
            if (now >= nextInHandSound) {
                nextInHandSound = now + IN_HAND_SOUND_INTERVAL_MS;
                String category = getDataObject().getSoundCategory();
                if (category != null && !category.isEmpty()) {
                    SoundManager.playSound(category, ThingSounds.IN_HAND);
                }
            }
        } else {
            nextInHandSound = 0;
        }

        playDigSound();
        playFootsteps();
        playWalk(tpf);
    }

    /**
     * The walk and run play as fast as the creature walks
     */
    private void playWalk(float tpf) {
        if (spatial == null) {
            return;
        }
        Vector3f position = spatial.getWorldTranslation();
        if (walkedFrom == null) {
            walkedFrom = position.clone();
        }
        float speed = getWalkSpeed(position.x - walkedFrom.x, position.z - walkedFrom.z, tpf);
        walkedFrom.set(position);
        for (Spatial child : ((Node) spatial).getChildren()) {
            AnimComposer animComposer = child.getControl(AnimComposer.class);

            // A cycling animation keeps the direction it plays in
            if (animComposer != null && Math.abs(animComposer.getGlobalSpeed()) != speed) {
                animComposer.setGlobalSpeed(Math.copySign(speed, animComposer.getGlobalSpeed()));
            }
        }
    }

    /**
     * How fast the shown animation plays for a step of the creature. DK II
     * (0x48BE50, 0x4B41E0) plays the walk and the run by the speed of the
     * creature: the start distance of the animation is the speed, in 1/4096
     * tiles a second, at which its frames play at their own frame rate. A step
     * is measured as its longer side plus half the shorter one, and the
     * animation plays less than a cycle a tick. Without a start distance it
     * doesn't play. Other animations play as they are.
     *
     * @param dx the step along x, in tiles
     * @param dz the step along z, in tiles
     * @param tpf how long the step took, in seconds
     * @return the speed for the animation, 1 plays it at
     * {@link KmfModelLoader#FRAMES_PER_SECOND}
     */
    float getWalkSpeed(float dx, float dz, float tpf) {
        ArtResource animation = getAnimationData(currentState);
        if ((currentState != Creature.AnimationType.WALK && currentState != Creature.AnimationType.RUN) || animation == null) {
            return 1;
        }
        Number startDistance = animation.getData(ArtResource.KEY_START_DIST);
        Number fps = animation.getData(ArtResource.KEY_FPS);
        if (startDistance == null || startDistance.floatValue() <= 0 || fps == null || tpf <= 0) {
            return 0;
        }

        float longer = Math.max(Math.abs(dx), Math.abs(dz));
        float shorter = Math.min(Math.abs(dx), Math.abs(dz));
        float framesPerSecond = fps.floatValue() * (longer + shorter / 2) * 4096 / startDistance.floatValue() / tpf;
        Number frames = animation.getData(ArtResource.KEY_FRAMES);
        if (frames != null && ticksPerSecond > 0) {
            framesPerSecond = Math.min(framesPerSecond, frames.floatValue() * ticksPerSecond * 4095 / 4096);
        }
        return framesPerSecond / KmfModelLoader.FRAMES_PER_SECOND;
    }

    /**
     * The digging sound is restarted when it ends so that it lasts as long as the work
     */
    private void playDigSound() {
        if (currentState != Creature.AnimationType.DIG || spatial == null
                || spatial.getCullHint() == Spatial.CullHint.Always) {
            digSound = SoundEngine.NO_HANDLE;
            return;
        }
        // A dig that has no sound is not asked again on every frame
        long now = System.currentTimeMillis();
        if (now >= nextDigSound && (digSound == SoundEngine.NO_HANDLE || !SoundManager.isPlaying(digSound))) {
            digSound = ThingSounds.playCreature(getDataObject(), currentState, spatial.getWorldTranslation());
            nextDigSound = now + DIG_SOUND_RETRY_MS;
        }
    }

    /**
     * A walking or running creature steps when the animation passes a footstep mark
     */
    private void playFootsteps() {
        int[] marks = FootstepSounds.getMarks(getDataObject(), currentState);
        if (marks == null || spatial == null || spatial.getCullHint() == Spatial.CullHint.Always) {
            previousStepTime = -1;
            return;
        }

        AnimComposer animComposer = getAnimComposer();
        Action action = animComposer != null ? animComposer.getCurrentAction() : null;
        if (action == null) {
            if (!stepWarningLogged) {
                stepWarningLogged = true;
                LOGGER.log(Logger.Level.WARNING, "No animation to follow for the footsteps of {0}", getDataObject().getName());
            }
            previousStepTime = -1;
            return;
        }

        float time = (float) animComposer.getTime(AnimComposer.DEFAULT_LAYER);
        if (previousStepTime >= 0) {
            int event = FootstepSounds.getStep(marks, previousStepTime, time, (float) action.getLength());
            if (event != FootstepSounds.NO_STEP && FootstepSounds.tryAcquire(System.currentTimeMillis())) {
                LOGGER.log(Logger.Level.DEBUG, "Footstep {0} of {1} at {2}", Integer.toHexString(event), getDataObject().getName(), time);
                Vector3f position = spatial.getWorldTranslation();
                ThingSounds.play(getDataObject().getSoundCategory(),
                        FootstepSounds.getEvent(event, waterCheck.test(position)), position);
            }
        }
        previousStepTime = time;
    }

    /**
     * @param waterCheck tells if a world position is on a water tile, creatures wade there
     */
    public void setWaterCheck(Predicate<Vector3f> waterCheck) {
        this.waterCheck = waterCheck;
    }

    /**
     * @param ticksPerSecond the logic ticks a second of the level, the walk
     * and run play less than a cycle a tick
     */
    public void setTicksPerSecond(int ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    private AnimComposer getAnimComposer() {
        ArtResource animation = getDataObject().getAnimation(currentState);
        if (animation == null || !(spatial instanceof Node node)) {
            return null;
        }
        Spatial animNode = node.getChild(animation.getName());
        return animNode != null ? animNode.getControl(AnimComposer.class) : null;
    }

    /**
     * The paper of the library rustles for as long as the creature researches
     */
    private void playResearchSound(Creature.AnimationType state) {
        if (researchSound != SoundEngine.NO_HANDLE) {
            SoundManager.stopSound(researchSound);
            researchSound = SoundEngine.NO_HANDLE;
        }
        if (state == Creature.AnimationType.RESEARCHING) {
            researchSound = ThingSounds.play(ThingSounds.CATEGORY_LIBRARY, ThingSounds.ROOM_LIBRARY_PAPER,
                    spatial.getWorldTranslation());
        }
    }

    @Override
    public void setTargetState(Creature.AnimationType state) {
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState)) {
            playAnimation(state);
            if (spatial != null && spatial.getCullHint() != Spatial.CullHint.Always) {
                int handle = ThingSounds.playCreature(getDataObject(), state, spatial.getWorldTranslation());
                digSound = state == Creature.AnimationType.DIG ? handle : SoundEngine.NO_HANDLE;
                nextDigSound = System.currentTimeMillis() + DIG_SOUND_RETRY_MS;
                playResearchSound(state);
            }
        }
    }

    @Override
    public int getSlapEffectId(short playerId) {
        return getDataObject().getSlapEffectId();
    }

     @Override
    public int getDeathEffectId() {
        return getDataObject().getDeathEffectId();
    }

    @Override
    public ArtResource getInHandIcon() {
        return getDataObject().getIcon1Resource();
    }

    @Override
    public ArtResource getInHandMesh() {
        return getDataObject().getAnimation(Creature.AnimationType.IN_HAND);
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    protected ArtResource getAnimationData(Creature.AnimationType state) {
        ArtResource animation = getDataObject().getAnimation(state);
        if (animation == null && state == Creature.AnimationType.TORTURED_CHAIR_SKELETON) {
            return ELECTROCUTE_SKELETONS[ThreadLocalRandom.current().nextInt(ELECTROCUTE_SKELETONS.length)];
        }
        return animation;
    }

    /**
     * Whether the creature is a chicken, as by the Chicken spell
     *
     * @return true if the creature is a chicken
     */
    public boolean isChicken() {
        CreatureAi creatureAi = getEntity().get(CreatureAi.class);
        return creatureAi != null && creatureAi.getCreatureState() == CreatureState.CHICKEN;
    }

    private boolean isWorkAnimation() {
        CreatureAi creatureAi = getEntity().get(CreatureAi.class);
        if (creatureAi != null) {
            CreatureState state = creatureAi.getCreatureState();

            // Dancing in the Casino, the jackpot and Combat Pit celebrations and sleeping go on until the state
            // changes. DK II starts the sleep action again each time it ends (0x46FA60)
            return state == CreatureState.WORK || state == CreatureState.DANCING || state == CreatureState.CELEBRATING
                    || state == CreatureState.COMBAT_PIT_VICTORY || state == CreatureState.SLEEPING
                    || state == CreatureState.RECUPERATING;
        }
        return false;
    }

}
