/*
 * Copyright (C) 2014-2025 OpenKeeper
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
import toniarts.openkeeper.game.sound.SoundManager;

/**
 * Plays a sound of the sound engine at the moment the effect starts, i.e. at once when the effect event (click,
 * hover) happens. The effect does not render anything, so it is not cut off when the screen changes right after
 * the click. Parameters: {@code category} and {@code event}.
 */
public final class EngineSoundEffect implements EffectImpl {

    @Override
    public void activate(@Nonnull final Nifty nifty, @Nonnull final Element element,
            @Nonnull final EffectProperties parameter) {
        String category = parameter.getProperty("category");
        String event = parameter.getProperty("event");
        if (category != null && event != null) {
            SoundManager.playSound(category, Integer.decode(event));
        }
    }

    @Override
    public void execute(@Nonnull final Element element, final float normalizedTime,
            @Nullable final Falloff falloff, @Nonnull final NiftyRenderEngine r) {
        // Nothing to render
    }

    @Override
    public void deactivate() {
        // Nothing to clean up
    }
}
