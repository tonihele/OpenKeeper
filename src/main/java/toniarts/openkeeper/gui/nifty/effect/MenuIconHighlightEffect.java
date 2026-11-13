/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.gui.nifty.effect;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.effects.EffectImpl;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.Falloff;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.render.NiftyRenderEngine;
import de.lessvoid.nifty.tools.Color;

/**
 * DK II draws an opaque menu icon again with ONE/ONE blending on hover.
 * Nifty has no additive blend mode. For these opaque icons, doubling RGB in
 * the batch shader is equivalent, including saturation, without changing alpha.
 * This is not a general replacement for additive rendering of translucent art.
 */
public final class MenuIconHighlightEffect implements EffectImpl {

    private NiftyImage image;
    private Color originalColor;

    @Override
    public void activate(Nifty nifty, Element element, EffectProperties properties) {
        deactivate();
        ImageRenderer imageRenderer = element.getRenderer(ImageRenderer.class);
        if (imageRenderer == null || imageRenderer.getImage() == null) {
            return;
        }
        // Each element has its own NiftyImage, even when the atlas texture is shared.
        image = imageRenderer.getImage();
        originalColor = image.getColor();
        Color base = originalColor == null ? Color.WHITE : originalColor;
        image.setColor(new Color(base.getRed() * 2f, base.getGreen() * 2f,
                base.getBlue() * 2f, base.getAlpha()));
    }

    @Override
    public void execute(Element element, float normalizedTime, Falloff falloff, NiftyRenderEngine renderer) {
        // Nifty's image path takes RGB from NiftyImage, not the render engine's
        // text color. Its normal image draw still applies enclosing alpha/fades.
    }

    @Override
    public void deactivate() {
        if (image != null) {
            image.setColor(originalColor);
            image = null;
            originalColor = null;
        }
    }
}
