/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.effect;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import java.util.Random;

/**
 * Plays an effect again and again for as long as the spatial it is attached
 * to is in the scene, e.g. the fire of an Inferno while it burns. The pause
 * before each play is random, so the plays overlap unevenly like a real fire.
 * The effects already playing fade out on their own once it is gone.
 */
public final class RepeatingEffectControl extends AbstractControl {

    private final float minInterval;
    private final float maxInterval;
    private final Runnable playEffect;
    private final Random random;
    private float untilNextPlay;

    /**
     * @param minInterval shortest pause between two plays, in seconds
     * @param maxInterval longest pause between two plays, in seconds
     * @param playEffect plays the effect once
     */
    public RepeatingEffectControl(float minInterval, float maxInterval, Runnable playEffect) {
        this(minInterval, maxInterval, playEffect, new Random());
    }

    RepeatingEffectControl(float minInterval, float maxInterval, Runnable playEffect, Random random) {
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.playEffect = playEffect;
        this.random = random;
        untilNextPlay = nextInterval();
    }

    @Override
    protected void controlUpdate(float tpf) {
        untilNextPlay -= tpf;
        while (untilNextPlay <= 0) {
            playEffect.run();
            untilNextPlay += nextInterval();
        }
    }

    private float nextInterval() {
        return minInterval + random.nextFloat() * (maxInterval - minInterval);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }

}
