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
import java.util.function.Predicate;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
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

    private long nextInHandSound;
    private int researchSound = SoundEngine.NO_HANDLE;
    private int digSound = SoundEngine.NO_HANDLE;
    private long nextDigSound;
    private static final Logger LOGGER = System.getLogger(CreatureViewControl.class.getName());

    private float previousStepTime = -1;
    private boolean stepWarningLogged;
    private Predicate<Vector3f> waterCheck = position -> false;

    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation,
            AssetManager assetManager, EntityTextParser<Creature> textParser) {
        super(entityId, entityData, data, animation, assetManager, textParser);
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
        String tooltip;
        if (getOwnerId() == playerId) {
            tooltip = Utils.getMainTextResourceBundle().getString("2841");
        } else {
            tooltip = Utils.getMainTextResourceBundle().getString(Integer.toString(getDataObject().getTooltipStringId()));
        }

        return textParser.parseText(tooltip, getEntity(), getDataObject());
    }


    @Override
    public boolean isStopAnimation() {
        return super.isStopAnimation() || (currentState != Creature.AnimationType.WALK && currentState != Creature.AnimationType.IN_HAND && !isWorkAnimation());
    }

    private void playAnimation(Creature.AnimationType animation) {
        AnimationLoader.playAnimation(getSpatial(), getDataObject().getAnimation(animation), assetManager);
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
        return getDataObject().getAnimation(state);
    }

    private boolean isWorkAnimation() {
        CreatureAi creatureAi = getEntity().get(CreatureAi.class);
        if (creatureAi != null) {
            return creatureAi.getCreatureState() == CreatureState.WORK;
        }
        return false;
    }

}
