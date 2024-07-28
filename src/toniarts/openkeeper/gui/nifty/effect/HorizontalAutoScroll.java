/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.gui.nifty.effect;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectImpl;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.Falloff;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.render.NiftyRenderEngine;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is like a marquee effect, constantly scrolling the view back and forth
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HorizontalAutoScroll implements EffectImpl {

    private static final float EFFECT_SPEED = 2500f;
    private float end = 100;
    private float start = 0;
    private float timeWhenChangedDirection = 0;
    private float delay;
    private float delayStart;
    private float currentXOffset;
    private boolean reverse = false;

    @Override
    public void activate(
            @Nonnull final Nifty nifty,
            @Nonnull final Element element,
            @Nonnull final EffectProperties parameter) {
        end = Integer.parseInt(parameter.getProperty("end", "0"));
        start = Integer.parseInt(parameter.getProperty("start", "0"));
        delay = Integer.parseInt(parameter.getProperty("delay", "0")) / 100f;
        delayStart = 0;
        reverse = start < end;
        timeWhenChangedDirection = 0;
        currentXOffset = 0;
    }

    @Override
    public void execute(
            @Nonnull final Element element,
            final float normalizedTime,
            @Nullable final Falloff falloff,
            @Nonnull final NiftyRenderEngine r) {
        if (normalizedTime - delayStart < delay) {
            timeWhenChangedDirection = normalizedTime;

            // Keep us always "on the go", if we don't apply the moveTo, posission will be reset
            r.moveTo(currentXOffset, 0);

            return;
        }

        if (reverse) {
            currentXOffset = Math.max(end - (normalizedTime - timeWhenChangedDirection) * EFFECT_SPEED, start);
        } else {
            currentXOffset = Math.min(start + (normalizedTime - timeWhenChangedDirection) * EFFECT_SPEED, end);
        }
        r.moveTo(currentXOffset, 0);

        if (currentXOffset >= end) {
            reverse = true;
            timeWhenChangedDirection = normalizedTime;
            delayStart = normalizedTime;
        } else if (currentXOffset <= start) {
            reverse = false;
            timeWhenChangedDirection = normalizedTime;
            delayStart = normalizedTime;
        }
    }

    @Override
    public void deactivate() {
    }

}
