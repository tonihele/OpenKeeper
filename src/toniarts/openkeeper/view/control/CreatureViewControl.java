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

import com.google.common.base.Objects;
import com.jme3.asset.AssetManager;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.world.animation.AnimationLoader;

/**
 * View control that is intended specifically for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class CreatureViewControl extends EntityViewControl<Creature, Creature.AnimationType> {

    public CreatureViewControl(EntityId entityId, EntityData entityData, Creature data, Creature.AnimationType animation, AssetManager assetManager) {
        super(entityId, entityData, data, animation, assetManager);
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        if (!isAnimationPlaying && targetAnimation != null) {
            playAnimation(targetAnimation);
        }
    }

    @Override
    public void onAnimationStop() {
        super.onAnimationStop();

        playAnimation(targetAnimation);
    }

    private void playAnimation(Creature.AnimationType animation) {
        AnimationLoader.playAnimation(getSpatial(), getDataObject().getAnimation(animation), assetManager);
        isAnimationPlaying = true;
        currentAnimation = animation;
    }

    @Override
    public void setTargetAnimation(Creature.AnimationType state) {
        super.setTargetAnimation(state);

        // Play immediately
        if (!Objects.equal(currentAnimation, targetAnimation)) {
            playAnimation(state);
        }
    }

}
