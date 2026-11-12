/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.view.animation.AnimationControl;
import toniarts.openkeeper.view.animation.AnimationLoader;

/**
 * A standalone control that randomly cycles through a set of creature idle
 * animations every 4 seconds. It is separate from the default idling behavior
 * and is intended for decorative creature displays (e.g. the hero gate front
 * end).
 */
public final class CreatureRandomAnimationControl extends AbstractControl implements AnimationControl {
    private final Creature creature;
    private final AssetManager assetManager;
    private final List<Creature.AnimationType> availableAnimations;

    /**
     * Creates a new idle animation cycling control.
     *
     * @param creature the creature data, used to look up animation resources
     * @param assetManager the asset manager for loading models
     * @param animationTypes the animation types to cycle through; types that
     *                       the creature does not have are silently filtered out
     */
    public CreatureRandomAnimationControl(Creature creature, AssetManager assetManager,
                                          List<Creature.AnimationType> animationTypes) {
        this.creature = creature;
        this.assetManager = assetManager;

        // Pre-filter to only animations the creature actually has
        this.availableAnimations = new ArrayList<>();
        for (Creature.AnimationType type : animationTypes) {
            ArtResource resource = creature.getAnimation(type);
            if (resource != null && resource.getType() != ArtResource.ArtResourceType.NONE) {
                availableAnimations.add(type);
            }
        }
    }

    @Override
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        super.setSpatial(spatial);

        if (spatial != null && !availableAnimations.isEmpty()) {
            // Start with a random animation immediately
            playRandomAnimation();
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // No render-time logic needed
    }

    private void playRandomAnimation() {
        int index = (int) (Math.random() * availableAnimations.size());
        Creature.AnimationType type = availableAnimations.get(index);
        ArtResource resource = creature.getAnimation(type);
        AnimationLoader.playAnimation(spatial, resource, assetManager);
    }

    // AnimationControl interface

    @Override
    public void onAnimationStop() {}

    @Override
    public void onAnimationCycleDone() {
        playRandomAnimation();// No-op; cycling is driven by the timer, not by animation completion
    }

    @Override
    public boolean isStopAnimation() {
        // Never stop — we want the animations to keep playing
        return false;
    }
}
