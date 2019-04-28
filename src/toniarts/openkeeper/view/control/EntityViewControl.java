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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.WatchedEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.map.MapTile;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import static toniarts.openkeeper.view.map.MapViewController.COLOR_FLASH;
import toniarts.openkeeper.view.text.TextParser;
import toniarts.openkeeper.world.animation.AnimationControl;

/**
 * General entity controller, a bridge between entities and view
 *
 * @param <T> The type of the entity
 * @param <S> The view state, or animation type
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class EntityViewControl<T, S> extends AbstractControl implements IEntityViewControl<T, S>, AnimationControl {

    private final EntityId entityId;
    private final WatchedEntity entity;
    private final T data;
    protected S currentState;
    protected S targetState;
    protected final AssetManager assetManager;
    protected final TextParser textParser;
    protected boolean isAnimationPlaying = false;

    private static final Collection<Class<? extends EntityComponent>> WATCHED_COMPONENTS = Arrays.asList(Interaction.class, Owner.class);

    private boolean active = false;

    public EntityViewControl(EntityId entityId, EntityData entityData, T data, S state, AssetManager assetManager, TextParser textParser) {
        this.entityId = entityId;
        this.currentState = state;
        this.targetState = state;
        this.assetManager = assetManager;
        this.data = data;
        this.textParser = textParser;

        // Subscribe to the entity changes
        entity = entityData.watchEntity(entityId, compileWatchedComponents());
    }

    /**
     * Get the entity for this control. It has all the up to date components
     * attached. To control what components are included, add the wanted
     * components with the {@link #getWatchedComponents() } method.
     *
     * @return
     */
    protected final Entity getEntity() {
        return entity;
    }

    private Class<EntityComponent>[] compileWatchedComponents() {
        Set<Class<? extends EntityComponent>> components = new HashSet<>();
        components.addAll(WATCHED_COMPONENTS);
        if (textParser != null) {
            components.addAll(textParser.getWatchedComponents());
        }
        components.addAll(getWatchedComponents());

        return components.toArray(new Class[components.size()]);
    }

    /**
     * Override this to give out the components needed to watch for. By default
     * a mutable list is returned so you can just ourright add your stuff here.
     * No need to worry about duplicates.
     *
     * @return list of components needed from the entity
     */
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        return new ArrayList<>();
    }

    @Override
    protected void controlUpdate(float tpf) {
        entity.applyChanges();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

    @Override
    public String getTooltip(short playerId) {
        return "";
    }

    @Override
    public boolean isPickable(short playerId) {
        Interaction interaction = entity.get(Interaction.class);
        if (interaction == null) {
            return false;
        }
        return interaction.pickUppable;
    }

    @Override
    public boolean isInteractable(short playerId) {
        Interaction interaction = entity.get(Interaction.class);
        if (interaction == null) {
            return false;
        }
        return interaction.interactable && getOwnerId() == playerId;
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_THING;
    }

    @Override
    public ArtResource getInHandMesh() {
        return null;
    }

    @Override
    public ArtResource getInHandIcon() {
        return null;
    }

    @Override
    public DroppableStatus getDroppableStatus(MapTile tile, Terrain terrain, short playerId) {
        return (tile.getOwnerId() == playerId
                && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) ? DroppableStatus.DROPPABLE : DroppableStatus.NOT_DROPPABLE);
    }

    @Override
    public void drop(MapTile tile, Vector2f coordinates, IEntityViewControl control) {

    }

    @Override
    public void onHover(short playerId) {
        UnitFlowerControl.showUnitFlower(this, null);
    }

    @Override
    public void onHoverStart(short playerId) {
        setHighlight(isInteractable(playerId) || isPickable(playerId));
    }

    @Override
    public void onHoverEnd(short playerId) {
        setHighlight(false);
    }

    private void setHighlight(final boolean enabled) {
        if (active != enabled) {
            active = enabled;
            AssetUtils.setModelHighlight(spatial, COLOR_FLASH, enabled);
        }
    }

    @Override
    public short getOwnerId() {
        return entity.get(Owner.class).ownerId;
    }

    @Override
    public void pickUp(short playerId) {

    }

    @Override
    public void interact(short playerId) {

    }

    @Override
    public boolean isSlappable(short playerId) {
        Interaction interaction = entity.get(Interaction.class);
        if (interaction == null) {
            return false;
        }
        return interaction.slappable;
    }

    @Override
    public void slap(short playerId) {

    }

    @Override
    public EntityId getEntityId() {
        return entityId;
    }

    @Override
    public void onAnimationStop() {
        isAnimationPlaying = false;
    }

    @Override
    public void onAnimationCycleDone() {

    }

    @Override
    public boolean isStopAnimation() {
        return targetState == null || targetState != currentState;
    }

    @Override
    public void setTargetState(S state) {
        this.targetState = (S) state;
    }

    @Override
    public T getDataObject() {
        return data;
    }

    protected abstract ArtResource getAnimationData(S state);

    @Override
    public void cleanup() {
        entity.release();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.entityId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EntityViewControl<?, ?> other = (EntityViewControl<?, ?>) obj;
        if (!Objects.equals(this.entityId, other.entityId)) {
            return false;
        }
        return true;
    }

}
