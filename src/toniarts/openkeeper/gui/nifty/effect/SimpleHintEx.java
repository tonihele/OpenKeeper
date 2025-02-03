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
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.effects.EffectImpl;
import de.lessvoid.nifty.effects.EffectProperties;
import de.lessvoid.nifty.effects.Falloff;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.render.NiftyRenderEngine;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.tools.SizeValue;
import de.lessvoid.nifty.tools.TargetElementResolver;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is a simple extension to the Nifty {@link SimpleHint}. SimpleHint
 * requires a {@link TextRenderer} to be present. Rework this so that we can
 * deal also with {@link Label}
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class SimpleHintEx implements EffectImpl {

    /**
     * target element.
     */
    @Nullable
    private Element targetElement;

    /**
     * hint text.
     */
    @Nullable
    private String hintText;

    /**
     * initialize.
     *
     * @param nifty Nifty
     * @param element Element
     * @param parameter Parameter
     */
    @Override
    public void activate(
            @Nonnull final Nifty nifty,
            @Nonnull final Element element,
            @Nonnull final EffectProperties parameter) {
        Screen screen = nifty.getCurrentScreen();
        if (screen == null) {
            return;
        }
        TargetElementResolver resolver = new TargetElementResolver(nifty.getCurrentScreen(), element);
        targetElement = resolver.resolve(parameter.getProperty("targetElement"));

        String text = parameter.getProperty("hintText");
        if (text != null) {
            hintText = text;
        }
    }

    /**
     * execute the effect.
     *
     * @param element the Element
     * @param normalizedTime TimeInterpolator to use
     * @param falloff falloff value
     * @param r RenderDevice to use
     */
    @Override
    public void execute(
            @Nonnull final Element element,
            final float normalizedTime,
            @Nullable final Falloff falloff,
            @Nonnull final NiftyRenderEngine r) {
        if (targetElement == null) {
            return;
        }

        Label label = targetElement.getNiftyControl(Label.class);
        String hint = hintText == null ? "Missing Hint Text!" : hintText;
        if (label != null) {
            label.setText(hint);
        } else {
            TextRenderer textRenderer = targetElement.getRenderer(TextRenderer.class);
            if (textRenderer != null) {
                textRenderer.setText(hint);
                targetElement.setConstraintWidth(SizeValue.px(textRenderer.getTextWidth()));
                element.getParent().layoutElements();
            }
        }
    }

    /**
     * deactivate the effect.
     */
    @Override
    public void deactivate() {
    }

}
