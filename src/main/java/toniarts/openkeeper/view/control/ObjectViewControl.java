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

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import toniarts.openkeeper.game.component.LevelIndicator;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.EntityPickupValidator;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.animation.AnimationLoader;
import toniarts.openkeeper.view.text.EntityTextParser;

/**
 * View control that is intended specifically for objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ObjectViewControl extends EntityViewControl<GameObject, ObjectViewState> {

    /**
     * A spin in the original (object state 18, 0x4A13F0) turns the object by
     * 1536 / (ticks + 3) - 64 of the 2048ths of a turn per game tick, so it
     * slows down and stops after 21 ticks
     */
    private static final float SPIN_SPEED = 1536;
    private static final float SPIN_SLOW_DOWN = 64;

    private boolean initialized = false;
    private int loopHandle = SoundEngine.NO_HANDLE;
    private final IMapDataInformation<? extends IMapTileInformation> mapData;
    private final boolean uniqueCreatureBed;
    private final int ticksPerSecond;
    private float spinTicks = -1;
    private float spinDirection;
    private float spinAngle;

    public ObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, ObjectViewState state,
            AssetManager assetManager, EntityTextParser<GameObject> textParser) {
        this(entityId, entityData, data, state, assetManager, textParser, null, false, 0);
    }

    public ObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, ObjectViewState state,
            AssetManager assetManager, EntityTextParser<GameObject> textParser,
            IMapDataInformation<? extends IMapTileInformation> mapData) {
        this(entityId, entityData, data, state, assetManager, textParser, mapData, false, 0);
    }

    /**
     * Creates the view control of an object
     *
     * @param entityId the object
     * @param entityData the entity data
     * @param data the object's data
     * @param state the object's view state
     * @param assetManager the asset manager
     * @param textParser the tooltip parser
     * @param mapData the map, for where the object can be picked up
     * @param uniqueCreatureBed whether the object is the bed of a unique
     * creature
     * @param ticksPerSecond the game ticks per second of the level, for the
     * spin, or 0 when the object doesn't spin
     */
    public ObjectViewControl(EntityId entityId, EntityData entityData, GameObject data, ObjectViewState state,
            AssetManager assetManager, EntityTextParser<GameObject> textParser,
            IMapDataInformation<? extends IMapTileInformation> mapData, boolean uniqueCreatureBed, int ticksPerSecond) {
        super(entityId, entityData, data, state, assetManager, textParser);
        this.mapData = mapData;
        this.uniqueCreatureBed = uniqueCreatureBed;
        this.ticksPerSecond = ticksPerSecond;
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(ObjectComponent.class);
        components.add(Position.class);
        components.add(LevelIndicator.class);
        return components;
    }

    @Override
    public boolean isPickable(short playerId) {
        return super.isPickable(playerId)
                && (mapData == null || EntityPickupValidator.isValidLocation(
                        getEntity().get(ObjectComponent.class),
                        getEntity().get(Owner.class),
                        getEntity().get(Position.class),
                        playerId,
                        mapData));
    }

    @Override
    public int getSlapEffectId(short playerId) {
        return getDataObject().getSlapEffectId();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(getEntityId());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ObjectViewControl other = (ObjectViewControl) obj;
        return Objects.equals(getEntityId(), other.getEntityId());
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Play the starting animation
        if (spatial != null && !initialized) {

            // Don't play for prison door nor the bar....?
            if (getDataObject().getObjectId() != 109 && getDataObject().getObjectId() != 116) {
                playAnimation(currentState);
                showLevel();
            }
            startSounds();
            initialized = true;
        }
    }

    private void startSounds() {
        if (spatial.getCullHint() == Spatial.CullHint.Always) {
            return;
        }
        // The spatial is not yet positioned when the control is attached, use the entity position
        Position position = getEntity().get(Position.class);
        if (position == null) {
            return;
        }
        String category = getDataObject().getSoundCategory();
        loopHandle = ThingSounds.play(category, ThingSounds.OBJECT_LOOP, position.position);
        ThingSounds.play(category, ThingSounds.OBJECT_CREATE, position.position);
    }

    @Override
    public void cleanup() {
        if (loopHandle != SoundEngine.NO_HANDLE) {
            SoundManager.stopSound(loopHandle);
            loopHandle = SoundEngine.NO_HANDLE;
        }
        super.cleanup();
    }

    @Override
    public String getTooltip(short playerId) {
        String key = getTooltipKey(getDataObject());
        if (key == null) {
            return null;
        }
        return textParser.parseText(Utils.getMainTextResourceBundle().getString(key), getEntity(), getDataObject());
    }

    /**
     * Get the text key of the object's own tooltip
     *
     * @param object the object
     * @return the key, or {@code null} if the object has no tooltip of its own,
     * like the creature beds
     */
    static String getTooltipKey(GameObject object) {
        if (object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {

            // TODO: better separation between loose and room gold
            return object.getObjectId() == 1 ? "2544" : "2543";
        }

        // Text 0 is "Jelly", the objects that have it have no tooltip
        if (object.getTooltipStringId() == 0) {
            return null;
        }
        return Integer.toString(object.getTooltipStringId());
    }

    @Override
    public ArtResource getInHandIcon() {
        return getDataObject().getGuiIconResource();
    }

    @Override
    public ArtResource getInHandMesh() {
        return getDataObject().getInHandMeshResource();
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        if (getDataObject().getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return CursorFactory.CursorType.HOLD_GOLD;
        }
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public CursorFactory.CursorType getDropCursor() {
        if (getDataObject().getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return CursorFactory.CursorType.DROP_GOLD;
        }
        return super.getDropCursor();
    }

    @Override
    public void setTargetState(ObjectViewState state) {
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState)) {
            playAnimation(state);
            currentState = targetState;

            // Every spin starts over, one way or the other
            if (state.state == GameObject.State.SPINNING && ticksPerSecond > 0) {
                spinTicks = 0;
                spinDirection = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
            }
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        if (spinTicks >= 0) {
            float speed = SPIN_SPEED / ((int) spinTicks + 3) - SPIN_SLOW_DOWN;
            if (speed > 0) {
                float ticks = tpf * ticksPerSecond;
                spinAngle = (spinAngle + spinDirection * speed * ticks * FastMath.TWO_PI / 2048) % FastMath.TWO_PI;
                spinTicks += ticks;
            } else {
                spinTicks = -1;
            }
        }

        // The spin turns the object from where it stands, an object that has just lost its place (sold, rebuilt)
        // is left alone
        Position position = spinAngle != 0 ? getEntity().get(Position.class) : null;
        if (position != null) {
            spatial.setLocalRotation(spatial.getLocalRotation().fromAngles(0, position.rotation + spinAngle, 0));
        }
    }

    /**
     * An object without a model of its own, like the Casino's pay out adjuster,
     * shows an indicator that moves once to where it points and stays there.
     * So does the My Pet Dungeon level indicator after a pull of its lever.
     *
     * @return true to hold the last frame
     */
    @Override
    public boolean isHoldLastFrame() {
        return getDataObject().getMeshResource() == null || isLevelIndicator();
    }

    private boolean isLevelIndicator() {
        return getDataObject().getStartState() == GameObject.State.MY_PET_DUNGEON_LEVEL_INDICATOR;
    }

    /**
     * The numbers of the My Pet Dungeon level indicator spin while its lever
     * is pulled, then stop on the level (DKII.EXE 0x4A3910 and 0x4A3A00)
     */
    @Override
    public void onAnimationCycleDone() {
        showLevel();
    }

    private void showLevel() {
        if (!isLevelIndicator()) {
            return;
        }
        LevelIndicator levelIndicator = getEntity().get(LevelIndicator.class);
        if (levelIndicator != null) {
            LevelIndicatorTextures.apply(getSpatial(), levelIndicator.level, assetManager);
        }
    }

    private void playAnimation(ObjectViewState viewState) {
        AnimationLoader.playAnimation(getSpatial(), getAnimationData(viewState), assetManager);
        if (uniqueCreatureBed) {
            CreatureVariantTextures.apply(getSpatial(), assetManager);
        }
        isAnimationPlaying = true;
    }

    @Override
    protected ArtResource getAnimationData(ObjectViewState state) {
        switch (state.animState) {
            case MESH_RESOURCE:
                return getDataObject().getMeshResource();
            case ADDITIONAL_RESOURCE_1: {
                return getDataObject().getAdditionalResources().get(0);
            }
            case ADDITIONAL_RESOURCE_2: {
                return getDataObject().getAdditionalResources().get(1);
            }
            case ADDITIONAL_RESOURCE_3: {
                return getDataObject().getAdditionalResources().get(2);
            }
            case ADDITIONAL_RESOURCE_4: {
                return getDataObject().getAdditionalResources().get(3);
            }
        }
        return getDataObject().getMeshResource();
    }

}
