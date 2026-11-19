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
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.controller.SlapRules;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.sound.FootstepSounds;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Creature.CreatureFlag;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.animation.CreatureAnimationRequest;
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
    private CreatureAnimationRequest lastAttempt;
    private CreatureAnimationRequest activeRequest;
    private Creature.AnimationType requestedState;
    private boolean visualRequestActive;
    private java.util.function.DoubleSupplier gameTime = () -> Double.NaN;
    private ArtResource activeAnimation;
    private int[] stepMarks;
    private toniarts.openkeeper.view.animation.FootstepMarkers normalizedStepMarks;
    private double previousMarkerAge = Double.NaN;

    public void setGameTime(java.util.function.DoubleSupplier gameTime) {
        this.gameTime = Objects.requireNonNull(gameTime);
    }

    /** Accepts restart identity even when the animation enum did not change. */
    public boolean setViewState(Creature creature, CreatureViewState state) {
        var request = new CreatureAnimationRequest(state);
        if (request.equals(lastAttempt) || request.equals(activeRequest)) return false;
        lastAttempt = request;
        acceptLogicalState(state.state);
        Creature previous = getDataObject();
        if (previous != creature && spatial != null) {
            var cache = spatial.getControl(toniarts.openkeeper.view.animation.PreparedAnimationCache.class);
            if (cache != null) cache.clear();
        }
        setDataObject(creature);
        Creature.AnimationType previousTarget = targetState;
        super.setTargetState(state.state);
        if (!playAnimation(state.state)) {
            setDataObject(previous);
            super.setTargetState(previousTarget);
            holdFailedVisual();
            return false;
        }
        playStateSounds(state.state);
        activeRequest = request;
        AnimationLoader.bindPlaybackClock(spatial, () -> gameTime.getAsDouble(), request.startTime());
        previousStepTime = -1;
        return true;
    }

    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation,
            AssetManager assetManager, EntityTextParser<Creature> textParser) {
        super(entityId, entityData, data, animation, assetManager, textParser);
        requestedState = animation;
    }

    /** Logical activity is independent of whether its art could replace the last valid visual. */
    public Creature.AnimationType getRequestedState() { return requestedState; }

    @Override public void onAnimationAttached() {
        // Retained clips may have been used as a corpse during an earlier logical state.
        toniarts.openkeeper.view.CharredOverlay.clear(spatial);
        super.onAnimationAttached();
    }

    private void acceptLogicalState(Creature.AnimationType state) {
        requestedState = state;
        stopActivitySounds();
        previousStepTime = -1;
        previousMarkerAge = Double.NaN;
        nextInHandSound = 0;
    }

    private void holdFailedVisual() {
        visualRequestActive = false;
        isAnimationPlaying = false;
        if (spatial != null) AnimationLoader.holdAnimation(spatial);
    }

    private void stopActivitySounds() {
        playResearchSound(null);
        if (digSound != SoundEngine.NO_HANDLE) SoundManager.stopSound(digSound);
        digSound = SoundEngine.NO_HANDLE;
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(CreatureAi.class);
        components.add(CreatureImprisoned.class);
        return components;
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
        if (spatial != null && spatial.getControl(toniarts.openkeeper.view.animation.PreparedAnimationCache.class) == null) {
            int clips = Math.clamp(toniarts.openkeeper.Main.getUserSettings().getInteger(
                    toniarts.openkeeper.game.data.Settings.Setting.ANIMATION_CACHE_CLIPS),0,32);
            long bytes = Math.clamp(toniarts.openkeeper.Main.getUserSettings().getInteger(
                    toniarts.openkeeper.game.data.Settings.Setting.ANIMATION_CACHE_KIB),0,4096) * 1024L;
            if (clips > 0) spatial.addControl(new toniarts.openkeeper.view.animation.PreparedAnimationCache(clips,bytes));
        }

        // Play the starting animation
        if (spatial != null && currentState != null) {
            playAnimation(currentState);
        }
    }

    @Override
    public void cleanup() {
        if (spatial != null) {
            var cache = spatial.getControl(toniarts.openkeeper.view.animation.PreparedAnimationCache.class);
            if (cache != null) cache.clear();
        }
        stopActivitySounds();
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
        return super.isStopAnimation() || (currentState != Creature.AnimationType.WALK
                && currentState != Creature.AnimationType.RUN && currentState != Creature.AnimationType.STEALTH_WALK
                && currentState != Creature.AnimationType.IN_HAND && !isWorkAnimation());
    }

    private boolean playAnimation(Creature.AnimationType animation) {
        ArtResource resource = getAnimationData(animation);
        if (AnimationLoader.playAnimation(getSpatial(), resource, (name, art) -> {
                Spatial model = toniarts.openkeeper.utils.AssetUtils.loadModel(assetManager,name,art);
                if (getDataObject().getFlags() != null && getDataObject().getFlags().contains(CreatureFlag.IS_UNIQUE))
                    CreatureVariantTextures.apply(model,assetManager);
                toniarts.openkeeper.view.animation.TextureAnimationControl.install(model,()->gameTime.getAsDouble(),
                        toniarts.openkeeper.animation.AnimationTiming.framesPerSecond(art));
                return model;
            }, false)
                != AnimationLoader.PlaybackResult.STARTED) return false;
        activeAnimation = resource;
        visualRequestActive = true;
        isAnimationPlaying = true;
        currentState = animation;
        stepMarks = FootstepSounds.getMarks(getDataObject(), animation);
        normalizedStepMarks = stepMarks == null ? null : new toniarts.openkeeper.view.animation.FootstepMarkers(
                stepMarks, toniarts.openkeeper.animation.AnimationTiming.framesPerSecond(resource));
        previousMarkerAge = Double.NaN;
        previousStepTime = -1;
        return true;
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);
        if (AnimationLoader.isPaused(spatial) || !visualRequestActive || requestedState != currentState) {
            previousStepTime = -1;
            previousMarkerAge = Double.NaN;
            return;
        }

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
        int[] marks = stepMarks;
        if (marks == null || spatial == null || spatial.getCullHint() == Spatial.CullHint.Always) {
            previousStepTime = -1;
            previousMarkerAge = Double.NaN;
            if (spatial != null && spatial.getCullHint() == Spatial.CullHint.Always) playResearchSound(null);
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
        double age = AnimationLoader.getPlaybackAge(spatial) - AnimationLoader.getMainStartDelay(spatial);
        if (previousStepTime >= 0) {
            int event = FootstepSounds.getStep(marks, previousStepTime, time, (float) action.getLength(),
                    toniarts.openkeeper.animation.AnimationTiming.framesPerSecond(activeAnimation));
            if (Double.isFinite(age) && Double.isFinite(previousMarkerAge)) {
                var mode = getPlaybackPolicy() == PlaybackPolicy.LOOP ? AnimationLoader.getLoopModeOnChannel(animComposer.getSpatial())
                        : toniarts.openkeeper.view.animation.LoopMode.DontLoop;
                long crossings = normalizedStepMarks.crossings(previousMarkerAge,age,action.getLength(),mode);
                // One audible catch-up per render update; the shared crowd budget applies afterward.
                event = crossings == 0 ? FootstepSounds.NO_STEP : event == FootstepSounds.NO_STEP ? FootstepSounds.STEP_A : event;
            }
            if (event != FootstepSounds.NO_STEP && FootstepSounds.tryAcquire(System.currentTimeMillis())) {
                LOGGER.log(Logger.Level.DEBUG, "Footstep {0} of {1} at {2}", Integer.toHexString(event), getDataObject().getName(), time);
                Vector3f position = spatial.getWorldTranslation();
                ThingSounds.play(getDataObject().getSoundCategory(),
                        FootstepSounds.getEvent(event, waterCheck.test(position)), position);
            }
        }
        previousStepTime = time;
        previousMarkerAge = age;
    }

    /**
     * @param waterCheck tells if a world position is on a water tile, creatures wade there
     */
    public void setWaterCheck(Predicate<Vector3f> waterCheck) {
        this.waterCheck = waterCheck;
    }

    private AnimComposer getAnimComposer() {
        ArtResource animation = activeAnimation;
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
        if (requestedState != state) acceptLogicalState(state);
        Creature.AnimationType previousTarget = targetState;
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState) || !visualRequestActive) {
            if (!playAnimation(state)) { super.setTargetState(previousTarget); holdFailedVisual(); return; }
            activeRequest = null;
            playStateSounds(state);
        }
    }

    private void playStateSounds(Creature.AnimationType state) {
            // State exit always releases a loop, including transitions hidden by fog.
            stopActivitySounds();
            if (spatial != null && spatial.getCullHint() != Spatial.CullHint.Always) {
                int handle = ThingSounds.playCreature(getDataObject(), state, spatial.getWorldTranslation());
                digSound = state == Creature.AnimationType.DIG ? handle : SoundEngine.NO_HANDLE;
                nextDigSound = System.currentTimeMillis() + DIG_SOUND_RETRY_MS;
                playResearchSound(state);
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

            // Dancing in the Casino and the jackpot celebration go on until the state changes
            return state == CreatureState.WORK || state == CreatureState.DANCING || state == CreatureState.CELEBRATING;
        }
        return false;
    }

}
