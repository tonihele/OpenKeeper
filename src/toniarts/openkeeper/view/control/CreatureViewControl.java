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
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.Objects;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.text.TextParser;
import toniarts.openkeeper.world.animation.AnimationLoader;

/**
 * View control that is intended specifically for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureViewControl extends EntityViewControl<Creature, Creature.AnimationType> {

    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation,
            AssetManager assetManager, TextParser textParser) {
        super(entityId, entityData, data, animation, assetManager, textParser);
    }

    @Override
    protected Collection<Class<? extends EntityComponent>> getWatchedComponents() {
        Collection<Class<? extends EntityComponent>> components = super.getWatchedComponents();
        components.add(CreatureAi.class);
        return components;
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
        return super.isStopAnimation() || (currentState != Creature.AnimationType.WALK && !isWorkAnimation());
    }

    private void playAnimation(Creature.AnimationType animation) {
        AnimationLoader.playAnimation(getSpatial(), getDataObject().getAnimation(animation), assetManager);
        isAnimationPlaying = true;
        currentState = animation;
    }

    @Override
    public void setTargetState(Creature.AnimationType state) {
        super.setTargetState(state);

        // Play immediately
        if (!Objects.equals(currentState, targetState)) {
            playAnimation(state);
        }
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
